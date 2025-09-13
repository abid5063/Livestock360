package com.livestock360.springbackend.controller;

import com.livestock360.springbackend.model.Appointment;
import com.livestock360.springbackend.service.AppointmentService;
import com.livestock360.springbackend.service.FarmerService;
import com.livestock360.springbackend.service.VetService;
import com.livestock360.springbackend.service.AnimalService;
import com.livestock360.springbackend.utils.JwtUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final FarmerService farmerService;
    private final VetService vetService;
    private final AnimalService animalService;
    private final Gson gson = new Gson();
    private final Type mapType = new TypeToken<Map<String, Object>>(){}.getType();

    @Autowired
    public AppointmentController(AppointmentService appointmentService, FarmerService farmerService, 
                               VetService vetService, AnimalService animalService) {
        this.appointmentService = appointmentService;
        this.farmerService = farmerService;
        this.vetService = vetService;
        this.animalService = animalService;
    }

    // POST /api/appointments - Create new appointment
    @PostMapping
    public ResponseEntity<String> createAppointment(@RequestBody String appointmentData, @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            String userType = JwtUtil.getUserTypeFromToken(token);
            
            if (userId == null || userType == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(gson.toJson(error));
            }

            if (!"farmer".equals(userType) && !"vet".equals(userType)) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Only farmers and vets can create appointments");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson(error));
            }

            Map<String, Object> request = gson.fromJson(appointmentData, mapType);
            Appointment appointment = new Appointment();
            
            if ("farmer".equals(userType)) {
                // Farmer creating appointment
                String vetId = (String) request.get("vetId");
                String animalId = (String) request.get("animalId");
                String scheduledDate = (String) request.get("scheduledDate");
                String scheduledTime = (String) request.get("scheduledTime");
                String symptoms = (String) request.get("symptoms");
                
                // Validation
                if (vetId == null || animalId == null || scheduledDate == null || scheduledTime == null || symptoms == null) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Vet ID, Animal ID, scheduled date, time, and symptoms are required");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(gson.toJson(error));
                }
                
                // Verify vet exists
                if (vetService.findById(vetId) == null) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Vet not found or not available");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(gson.toJson(error));
                }
                
                appointment.setFarmerId(new ObjectId(userId));
                appointment.setVetId(new ObjectId(vetId));
                appointment.setAnimalId(new ObjectId(animalId));
                appointment.setAppointmentType((String) request.getOrDefault("appointmentType", "consultation"));
                appointment.setPriority((String) request.getOrDefault("priority", "normal"));
                appointment.setScheduledDate(parseDate(scheduledDate));
                appointment.setScheduledTime(scheduledTime);
                appointment.setDuration(request.get("duration") != null ? ((Double) request.get("duration")).intValue() : 30);
                appointment.setSymptoms(symptoms.trim());
                appointment.setDescription(request.get("description") != null ? ((String) request.get("description")).trim() : "");
                appointment.setStatus("pending");
                
            } else {
                // Vet creating appointment
                String farmerId = (String) request.get("farmer");
                String animalName = (String) request.get("animalName");
                String date = (String) request.get("date");
                String time = (String) request.get("time");
                String reason = (String) request.get("reason");
                
                // Validation
                if (farmerId == null || animalName == null || date == null || time == null || reason == null) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Farmer ID, animal name, date, time, and reason are required");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(gson.toJson(error));
                }
                
                // Verify farmer exists
                if (farmerService.findById(farmerId) == null) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Farmer not found");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(gson.toJson(error));
                }
                
                appointment.setFarmerId(new ObjectId(farmerId));
                appointment.setVetId(new ObjectId(userId));
                appointment.setAnimalName(animalName.trim());
                appointment.setAppointmentType("consultation");
                appointment.setPriority("normal");
                appointment.setScheduledDate(parseDate(date));
                appointment.setScheduledTime(time);
                appointment.setDuration(30);
                appointment.setSymptoms(reason.trim());
                appointment.setDescription(request.get("notes") != null ? ((String) request.get("notes")).trim() : "");
                appointment.setStatus((String) request.getOrDefault("status", "pending"));
            }
            
            appointment.setCreatedAt(new Date());
            appointment.setUpdatedAt(new Date());
            
            Appointment savedAppointment = appointmentService.save(appointment);
            
            if (savedAppointment != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Appointment created successfully");
                response.put("appointment", createAppointmentResponse(savedAppointment));
                
                return ResponseEntity.status(HttpStatus.CREATED).body(gson.toJson(response));
            } else {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Failed to create appointment");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
            }
            
        } catch (Exception e) {
            System.out.println("Error in createAppointment: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
        }
    }

    // GET /api/appointments/farmer - Get farmer's appointments
    @GetMapping("/farmer")
    public ResponseEntity<String> getFarmerAppointments(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            String userType = JwtUtil.getUserTypeFromToken(token);
            
            if (userId == null || !"farmer".equals(userType)) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Access denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson(error));
            }

            List<Appointment> appointments = appointmentService.findByFarmerId(userId, status, page, limit);
            long total = appointmentService.countByFarmerId(userId, status);
            
            List<Map<String, Object>> formattedAppointments = new ArrayList<>();
            for (Appointment appointment : appointments) {
                formattedAppointments.add(createAppointmentResponse(appointment));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("appointments", formattedAppointments);
            
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("current", page);
            pagination.put("pages", Math.ceil((double) total / limit));
            pagination.put("total", total);
            response.put("pagination", pagination);
            
            return ResponseEntity.ok(gson.toJson(response));
            
        } catch (Exception e) {
            System.out.println("Error in getFarmerAppointments: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
        }
    }

    // GET /api/appointments/vet - Get vet's appointments
    @GetMapping("/vet")
    public ResponseEntity<String> getVetAppointments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            String userType = JwtUtil.getUserTypeFromToken(token);
            
            if (userId == null || !"vet".equals(userType)) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Access denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson(error));
            }

            List<Appointment> appointments = appointmentService.findByVetId(userId, status, date, page, limit);
            long total = appointmentService.countByVetId(userId, status, date);
            
            List<Map<String, Object>> formattedAppointments = new ArrayList<>();
            for (Appointment appointment : appointments) {
                Map<String, Object> appointmentData = createAppointmentResponse(appointment);
                
                // Add additional fields for vet dashboard
                appointmentData.put("farmerName", getFarmerName(appointment.getFarmerId()));
                appointmentData.put("animalType", getAnimalType(appointment.getAnimalId()));
                appointmentData.put("animalName", getAnimalName(appointment.getAnimalId(), appointment.getAnimalName()));
                appointmentData.put("date", appointment.getScheduledDate());
                
                formattedAppointments.add(appointmentData);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("appointments", formattedAppointments);
            
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("current", page);
            pagination.put("pages", Math.ceil((double) total / limit));
            pagination.put("total", total);
            response.put("pagination", pagination);
            
            return ResponseEntity.ok(gson.toJson(response));
            
        } catch (Exception e) {
            System.out.println("Error in getVetAppointments: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
        }
    }

    // GET /api/appointments/vet/{vetId} - Get vet's appointments by vet ID (legacy route)
    @GetMapping("/vet/{vetId}")
    public ResponseEntity<String> getVetAppointmentsByVetId(
            @PathVariable String vetId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            String userType = JwtUtil.getUserTypeFromToken(token);
            
            if (userId == null || !"vet".equals(userType)) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Access denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson(error));
            }

            // Use the authenticated user's ID instead of the path parameter for security
            return getVetAppointments(status, date, page, limit, authHeader);
            
        } catch (Exception e) {
            System.out.println("Error in getVetAppointmentsByVetId: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
        }
    }

    // GET /api/appointments/{id} - Get single appointment
    @GetMapping("/{appointmentId}")
    public ResponseEntity<String> getSingleAppointment(
            @PathVariable String appointmentId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            String userType = JwtUtil.getUserTypeFromToken(token);
            
            if (userId == null || userType == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(gson.toJson(error));
            }

            Appointment appointment = appointmentService.findById(appointmentId);
            
            if (appointment == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Appointment not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(error));
            }
            
            // Check access permissions
            boolean hasAccess = false;
            if ("farmer".equals(userType) && appointment.getFarmerId().toString().equals(userId)) {
                hasAccess = true;
            } else if ("vet".equals(userType) && appointment.getVetId().toString().equals(userId)) {
                hasAccess = true;
            }
            
            if (!hasAccess) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Access denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson(error));
            }
            
            Map<String, Object> response = createAppointmentResponse(appointment);
            
            return ResponseEntity.ok(gson.toJson(response));
            
        } catch (Exception e) {
            System.out.println("Error in getSingleAppointment: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
        }
    }

    // PUT /api/appointments/{id} - Update appointment status
    @PutMapping("/{appointmentId}")
    public ResponseEntity<String> updateAppointment(
            @PathVariable String appointmentId,
            @RequestBody String updateData,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            String userType = JwtUtil.getUserTypeFromToken(token);
            
            if (userId == null || !"vet".equals(userType)) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Only vets can update appointment status");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(gson.toJson(error));
            }

            Map<String, Object> updates = gson.fromJson(updateData, mapType);
            
            boolean success = appointmentService.updateAppointment(appointmentId, userId, updates);
            
            if (success) {
                // Update vet statistics if status changed
                String newStatus = (String) updates.get("status");
                if ("completed".equals(newStatus)) {
                    // Update vet completed appointments count
                    // This could be handled in the service layer if needed
                } else if ("cancelled".equals(newStatus)) {
                    // Update vet cancelled appointments count
                    // This could be handled in the service layer if needed
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Appointment updated successfully");
                return ResponseEntity.ok(gson.toJson(response));
            } else {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Appointment not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(error));
            }
            
        } catch (Exception e) {
            System.out.println("Error in updateAppointment: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
        }
    }

    // DELETE /api/appointments/{id} - Cancel appointment
    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<String> cancelAppointment(
            @PathVariable String appointmentId,
            @RequestBody(required = false) String requestData,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            String userType = JwtUtil.getUserTypeFromToken(token);
            
            if (userId == null || userType == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(gson.toJson(error));
            }

            String reason = null;
            if (requestData != null && !requestData.isEmpty()) {
                Map<String, Object> request = gson.fromJson(requestData, mapType);
                reason = (String) request.get("reason");
            }
            
            boolean success = appointmentService.cancelAppointment(appointmentId, userId, userType, reason);
            
            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Appointment cancelled successfully");
                return ResponseEntity.ok(gson.toJson(response));
            } else {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Appointment not found or access denied");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(error));
            }
            
        } catch (Exception e) {
            System.out.println("Error in cancelAppointment: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
        }
    }

    // DELETE /api/appointments/remove/{id} - Delete appointment permanently
    @DeleteMapping("/remove/{appointmentId}")
    public ResponseEntity<String> deleteAppointment(
            @PathVariable String appointmentId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            String userType = JwtUtil.getUserTypeFromToken(token);
            
            if (userId == null || userType == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(gson.toJson(error));
            }

            boolean success = appointmentService.deleteAppointment(appointmentId, userId, userType);
            
            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Appointment deleted successfully");
                return ResponseEntity.ok(gson.toJson(response));
            } else {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Appointment not found or access denied");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(error));
            }
            
        } catch (Exception e) {
            System.out.println("Error in deleteAppointment: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
        }
    }

    // GET /api/appointments/availability/{vetId}/{date} - Get available time slots
    @GetMapping("/availability/{vetId}/{date}")
    public ResponseEntity<String> getAvailability(
            @PathVariable String vetId,
            @PathVariable String date,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            
            if (userId == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(gson.toJson(error));
            }

            // Verify vet exists
            if (vetService.findById(vetId) == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Vet not found or not available");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(error));
            }
            
            List<String> availableSlots = appointmentService.getAvailableTimeSlots(vetId, date);
            
            Map<String, Object> response = new HashMap<>();
            response.put("availableSlots", availableSlots);
            
            return ResponseEntity.ok(gson.toJson(response));
            
        } catch (Exception e) {
            System.out.println("Error in getAvailability: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
        }
    }

    // Helper method to create appointment response
    private Map<String, Object> createAppointmentResponse(Appointment appointment) {
        Map<String, Object> response = new HashMap<>();
        response.put("_id", appointment.getId().toString());
        response.put("farmerId", appointment.getFarmerId().toString());
        response.put("vetId", appointment.getVetId().toString());
        if (appointment.getAnimalId() != null) {
            response.put("animalId", appointment.getAnimalId().toString());
        }
        response.put("animalName", appointment.getAnimalName());
        response.put("appointmentType", appointment.getAppointmentType());
        response.put("priority", appointment.getPriority());
        response.put("scheduledDate", appointment.getScheduledDate());
        response.put("scheduledTime", appointment.getScheduledTime());
        response.put("duration", appointment.getDuration());
        response.put("symptoms", appointment.getSymptoms());
        response.put("description", appointment.getDescription());
        response.put("status", appointment.getStatus());
        response.put("diagnosis", appointment.getDiagnosis());
        response.put("treatment", appointment.getTreatment());
        response.put("prescription", appointment.getPrescription());
        response.put("vetNotes", appointment.getVetNotes());
        response.put("followUpRequired", appointment.getFollowUpRequired());
        response.put("followUpDate", appointment.getFollowUpDate());
        response.put("cancelledBy", appointment.getCancelledBy());
        response.put("cancellationReason", appointment.getCancellationReason());
        response.put("cancelledAt", appointment.getCancelledAt());
        response.put("createdAt", appointment.getCreatedAt());
        response.put("updatedAt", appointment.getUpdatedAt());
        return response;
    }

    // Helper method to get farmer name
    private String getFarmerName(ObjectId farmerId) {
        try {
            if (farmerId == null) return "Unknown Farmer";
            var farmer = farmerService.findById(farmerId.toString());
            return farmer != null ? farmer.getName() : "Unknown Farmer";
        } catch (Exception e) {
            return "Unknown Farmer";
        }
    }

    // Helper method to get animal type
    private String getAnimalType(ObjectId animalId) {
        try {
            if (animalId == null) return "Unknown";
            var animal = animalService.findById(animalId.toString());
            return animal != null ? animal.getType() : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    // Helper method to get animal name
    private String getAnimalName(ObjectId animalId, String fallbackName) {
        try {
            if (animalId == null) return fallbackName != null ? fallbackName : "Unknown";
            var animal = animalService.findById(animalId.toString());
            return animal != null ? animal.getName() : (fallbackName != null ? fallbackName : "Unknown");
        } catch (Exception e) {
            return fallbackName != null ? fallbackName : "Unknown";
        }
    }

    // Helper method to parse date strings safely
    private Date parseDate(String dateString) {
        try {
            // Try ISO format first (YYYY-MM-DD)
            if (dateString.matches("\\d{4}-\\d{2}-\\d{2}")) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                return formatter.parse(dateString);
            }
            // Try timestamp format
            return new Date(Long.parseLong(dateString));
        } catch (ParseException | NumberFormatException e) {
            // Fallback to current date
            return new Date();
        }
    }
}