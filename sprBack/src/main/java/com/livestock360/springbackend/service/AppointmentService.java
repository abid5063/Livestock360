package com.livestock360.springbackend.service;

import com.livestock360.springbackend.model.Appointment;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import com.mongodb.client.model.Sorts;

@Service
public class AppointmentService {

    private final MongoDatabase mongoDatabase;

    @Autowired
    public AppointmentService(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    public Appointment save(Appointment appointment) {
        try {
            MongoCollection<Document> collection = mongoDatabase.getCollection("appointments");
            Document doc = appointmentToDocument(appointment);
            
            if (appointment.getId() == null) {
                appointment.setId(new ObjectId());
                doc.put("_id", appointment.getId());
                collection.insertOne(doc);
            } else {
                collection.replaceOne(eq("_id", appointment.getId()), doc);
            }
            
            return appointment;
        } catch (Exception e) {
            System.out.println("Error saving appointment: " + e.getMessage());
            return null;
        }
    }

    public Appointment findById(String appointmentId) {
        try {
            MongoCollection<Document> collection = mongoDatabase.getCollection("appointments");
            Document doc = collection.find(eq("_id", new ObjectId(appointmentId))).first();
            return doc != null ? documentToAppointment(doc) : null;
        } catch (Exception e) {
            System.out.println("Error finding appointment by ID: " + e.getMessage());
            return null;
        }
    }

    public List<Appointment> findByFarmerId(String farmerId, String status, int page, int limit) {
        try {
            MongoCollection<Document> collection = mongoDatabase.getCollection("appointments");
            
            // Build query
            Document query = new Document("farmerId", new ObjectId(farmerId));
            if (status != null && !status.isEmpty()) {
                query.append("status", status);
            }
            
            List<Document> docs = collection.find(query)
                .sort(Sorts.descending("createdAt"))
                .skip((page - 1) * limit)
                .limit(limit)
                .into(new ArrayList<>());
            
            return docs.stream()
                .map(this::documentToAppointment)
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Error finding appointments by farmer ID: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Appointment> findByVetId(String vetId, String status, String date, int page, int limit) {
        try {
            MongoCollection<Document> collection = mongoDatabase.getCollection("appointments");
            
            // Build query
            Document query = new Document("vetId", new ObjectId(vetId));
            if (status != null && !status.isEmpty()) {
                query.append("status", status);
            }
            
            // Add date filter if provided
            if (date != null && !date.isEmpty()) {
                Date selectedDate = parseDate(date);
                Date startOfDay = new Date(selectedDate.getTime());
                Date endOfDay = new Date(selectedDate.getTime() + 24 * 60 * 60 * 1000 - 1);
                query.append("scheduledDate", new Document("$gte", startOfDay).append("$lte", endOfDay));
            }
            
            List<Document> docs = collection.find(query)
                .sort(Sorts.ascending("scheduledDate", "scheduledTime"))
                .skip((page - 1) * limit)
                .limit(limit)
                .into(new ArrayList<>());
            
            return docs.stream()
                .map(this::documentToAppointment)
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Error finding appointments by vet ID: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public long countByFarmerId(String farmerId, String status) {
        try {
            MongoCollection<Document> collection = mongoDatabase.getCollection("appointments");
            Document query = new Document("farmerId", new ObjectId(farmerId));
            if (status != null && !status.isEmpty()) {
                query.append("status", status);
            }
            return collection.countDocuments(query);
        } catch (Exception e) {
            System.out.println("Error counting appointments by farmer ID: " + e.getMessage());
            return 0;
        }
    }

    public long countByVetId(String vetId, String status, String date) {
        try {
            MongoCollection<Document> collection = mongoDatabase.getCollection("appointments");
            Document query = new Document("vetId", new ObjectId(vetId));
            if (status != null && !status.isEmpty()) {
                query.append("status", status);
            }
            
            if (date != null && !date.isEmpty()) {
                Date selectedDate = parseDate(date);
                Date startOfDay = new Date(selectedDate.getTime());
                Date endOfDay = new Date(selectedDate.getTime() + 24 * 60 * 60 * 1000 - 1);
                query.append("scheduledDate", new Document("$gte", startOfDay).append("$lte", endOfDay));
            }
            
            return collection.countDocuments(query);
        } catch (Exception e) {
            System.out.println("Error counting appointments by vet ID: " + e.getMessage());
            return 0;
        }
    }

    public boolean updateAppointment(String appointmentId, String vetId, Map<String, Object> updates) {
        try {
            MongoCollection<Document> collection = mongoDatabase.getCollection("appointments");
            
            // Build update document
            Document updateDoc = new Document("updatedAt", new Date());
            if (updates.get("status") != null) updateDoc.append("status", updates.get("status"));
            if (updates.get("diagnosis") != null) updateDoc.append("diagnosis", updates.get("diagnosis"));
            if (updates.get("treatment") != null) updateDoc.append("treatment", updates.get("treatment"));
            if (updates.get("prescription") != null) updateDoc.append("prescription", updates.get("prescription"));
            if (updates.get("vetNotes") != null) updateDoc.append("vetNotes", updates.get("vetNotes"));
            if (updates.get("followUpRequired") != null) updateDoc.append("followUpRequired", updates.get("followUpRequired"));
            if (updates.get("followUpDate") != null) updateDoc.append("followUpDate", parseDate((String) updates.get("followUpDate")));
            
            return collection.updateOne(
                and(eq("_id", new ObjectId(appointmentId)), eq("vetId", new ObjectId(vetId))),
                new Document("$set", updateDoc)
            ).getModifiedCount() > 0;
        } catch (Exception e) {
            System.out.println("Error updating appointment: " + e.getMessage());
            return false;
        }
    }

    public boolean cancelAppointment(String appointmentId, String userId, String userType, String reason) {
        try {
            MongoCollection<Document> collection = mongoDatabase.getCollection("appointments");
            
            // Verify user has permission to cancel
            Document appointment = collection.find(eq("_id", new ObjectId(appointmentId))).first();
            if (appointment == null) {
                return false;
            }
            
            boolean hasPermission = false;
            if ("farmer".equals(userType) && appointment.getObjectId("farmerId").toString().equals(userId)) {
                hasPermission = true;
            } else if ("vet".equals(userType) && appointment.getObjectId("vetId").toString().equals(userId)) {
                hasPermission = true;
            }
            
            if (!hasPermission) {
                return false;
            }
            
            Document updateDoc = new Document()
                .append("status", "cancelled")
                .append("cancelledBy", userType)
                .append("cancellationReason", reason)
                .append("cancelledAt", new Date())
                .append("updatedAt", new Date());
            
            return collection.updateOne(
                eq("_id", new ObjectId(appointmentId)),
                new Document("$set", updateDoc)
            ).getModifiedCount() > 0;
        } catch (Exception e) {
            System.out.println("Error cancelling appointment: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteAppointment(String appointmentId, String userId, String userType) {
        try {
            MongoCollection<Document> collection = mongoDatabase.getCollection("appointments");
            
            // Verify user has permission to delete
            Document appointment = collection.find(eq("_id", new ObjectId(appointmentId))).first();
            if (appointment == null) {
                return false;
            }
            
            boolean hasPermission = false;
            if ("farmer".equals(userType) && appointment.getObjectId("farmerId").toString().equals(userId)) {
                hasPermission = true;
            } else if ("vet".equals(userType) && appointment.getObjectId("vetId").toString().equals(userId)) {
                hasPermission = true;
            }
            
            if (!hasPermission) {
                return false;
            }
            
            return collection.deleteOne(eq("_id", new ObjectId(appointmentId))).getDeletedCount() > 0;
        } catch (Exception e) {
            System.out.println("Error deleting appointment: " + e.getMessage());
            return false;
        }
    }

    public List<String> getAvailableTimeSlots(String vetId, String dateStr) {
        try {
            MongoCollection<Document> collection = mongoDatabase.getCollection("appointments");
            Date selectedDate = parseDate(dateStr);
            Date startOfDay = new Date(selectedDate.getTime());
            Date endOfDay = new Date(selectedDate.getTime() + 24 * 60 * 60 * 1000 - 1);
            
            List<Document> existingAppointments = collection.find(
                and(
                    eq("vetId", new ObjectId(vetId)),
                    and(gte("scheduledDate", startOfDay), lte("scheduledDate", endOfDay)),
                    in("status", Arrays.asList("pending", "accepted", "in-progress"))
                )
            ).into(new ArrayList<>());
            
            List<String> bookedSlots = new ArrayList<>();
            for (Document appointment : existingAppointments) {
                bookedSlots.add(appointment.getString("scheduledTime"));
            }
            
            // Generate available time slots (9 AM to 5 PM, 30-minute slots)
            List<String> availableSlots = new ArrayList<>();
            for (int hour = 9; hour < 17; hour++) {
                for (int minute = 0; minute < 60; minute += 30) {
                    String timeSlot = String.format("%02d:%02d", hour, minute);
                    if (!bookedSlots.contains(timeSlot)) {
                        availableSlots.add(timeSlot);
                    }
                }
            }
            
            return availableSlots;
        } catch (Exception e) {
            System.out.println("Error getting available time slots: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Helper method to convert Appointment to MongoDB Document
    private Document appointmentToDocument(Appointment appointment) {
        Document doc = new Document();
        if (appointment.getId() != null) doc.append("_id", appointment.getId());
        if (appointment.getFarmerId() != null) doc.append("farmerId", appointment.getFarmerId());
        if (appointment.getVetId() != null) doc.append("vetId", appointment.getVetId());
        if (appointment.getAnimalId() != null) doc.append("animalId", appointment.getAnimalId());
        if (appointment.getAnimalName() != null) doc.append("animalName", appointment.getAnimalName());
        if (appointment.getAppointmentType() != null) doc.append("appointmentType", appointment.getAppointmentType());
        if (appointment.getPriority() != null) doc.append("priority", appointment.getPriority());
        if (appointment.getScheduledDate() != null) doc.append("scheduledDate", appointment.getScheduledDate());
        if (appointment.getScheduledTime() != null) doc.append("scheduledTime", appointment.getScheduledTime());
        if (appointment.getDuration() != null) doc.append("duration", appointment.getDuration());
        if (appointment.getSymptoms() != null) doc.append("symptoms", appointment.getSymptoms());
        if (appointment.getDescription() != null) doc.append("description", appointment.getDescription());
        if (appointment.getStatus() != null) doc.append("status", appointment.getStatus());
        if (appointment.getDiagnosis() != null) doc.append("diagnosis", appointment.getDiagnosis());
        if (appointment.getTreatment() != null) doc.append("treatment", appointment.getTreatment());
        if (appointment.getPrescription() != null) doc.append("prescription", appointment.getPrescription());
        if (appointment.getVetNotes() != null) doc.append("vetNotes", appointment.getVetNotes());
        if (appointment.getFollowUpRequired() != null) doc.append("followUpRequired", appointment.getFollowUpRequired());
        if (appointment.getFollowUpDate() != null) doc.append("followUpDate", appointment.getFollowUpDate());
        if (appointment.getCancelledBy() != null) doc.append("cancelledBy", appointment.getCancelledBy());
        if (appointment.getCancellationReason() != null) doc.append("cancellationReason", appointment.getCancellationReason());
        if (appointment.getCancelledAt() != null) doc.append("cancelledAt", appointment.getCancelledAt());
        if (appointment.getCreatedAt() != null) doc.append("createdAt", appointment.getCreatedAt());
        if (appointment.getUpdatedAt() != null) doc.append("updatedAt", appointment.getUpdatedAt());
        return doc;
    }

    // Helper method to convert MongoDB Document to Appointment
    private Appointment documentToAppointment(Document doc) {
        Appointment appointment = new Appointment();
        appointment.setId(doc.getObjectId("_id"));
        appointment.setFarmerId(doc.getObjectId("farmerId"));
        appointment.setVetId(doc.getObjectId("vetId"));
        appointment.setAnimalId(doc.getObjectId("animalId"));
        appointment.setAnimalName(doc.getString("animalName"));
        appointment.setAppointmentType(doc.getString("appointmentType"));
        appointment.setPriority(doc.getString("priority"));
        appointment.setScheduledDate(doc.getDate("scheduledDate"));
        appointment.setScheduledTime(doc.getString("scheduledTime"));
        appointment.setDuration(doc.getInteger("duration"));
        appointment.setSymptoms(doc.getString("symptoms"));
        appointment.setDescription(doc.getString("description"));
        appointment.setStatus(doc.getString("status"));
        appointment.setDiagnosis(doc.getString("diagnosis"));
        appointment.setTreatment(doc.getString("treatment"));
        appointment.setPrescription(doc.getString("prescription"));
        appointment.setVetNotes(doc.getString("vetNotes"));
        appointment.setFollowUpRequired(doc.getBoolean("followUpRequired"));
        appointment.setFollowUpDate(doc.getDate("followUpDate"));
        appointment.setCancelledBy(doc.getString("cancelledBy"));
        appointment.setCancellationReason(doc.getString("cancellationReason"));
        appointment.setCancelledAt(doc.getDate("cancelledAt"));
        appointment.setCreatedAt(doc.getDate("createdAt"));
        appointment.setUpdatedAt(doc.getDate("updatedAt"));
        return appointment;
    }

    // Helper method to parse date strings safely
    private Date parseDate(String dateString) {
        try {
            // Try ISO format first (YYYY-MM-DD)
            if (dateString.matches("\\d{4}-\\d{2}-\\d{2}")) {
                java.time.LocalDate localDate = java.time.LocalDate.parse(dateString);
                return Date.from(localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
            }
            // Try timestamp format
            return new Date(Long.parseLong(dateString));
        } catch (Exception e) {
            // Fallback to current date
            return new Date();
        }
    }
}