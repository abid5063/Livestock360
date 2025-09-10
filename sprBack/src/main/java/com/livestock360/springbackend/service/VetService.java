package com.livestock360.springbackend.service;

import com.google.gson.Gson;
import com.livestock360.springbackend.model.Vet;
import com.livestock360.springbackend.utils.PasswordUtil;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class VetService {

    private final MongoDatabase database;
    private final Gson gson;

    @Autowired
    public VetService(MongoDatabase database) {
        this.database = database;
        this.gson = new Gson();
    }

    private MongoCollection<Document> getVetsCollection() {
        return database.getCollection("vets");
    }

    public Vet findByEmail(String email) {
        try {
            MongoCollection<Document> collection = getVetsCollection();
            Document doc = collection.find(Filters.eq("email", email)).first();
            
            if (doc != null) {
                return documentToVet(doc);
            }
            return null;
        } catch (Exception e) {
            System.out.println("Error finding vet by email: " + e.getMessage());
            return null;
        }
    }

    public Vet findById(String id) {
        try {
            MongoCollection<Document> collection = getVetsCollection();
            Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
            
            if (doc != null) {
                return documentToVet(doc);
            }
            return null;
        } catch (Exception e) {
            System.out.println("Error finding vet by ID: " + e.getMessage());
            return null;
        }
    }

    public Vet save(Vet vet) {
        try {
            MongoCollection<Document> collection = getVetsCollection();
            Document doc = vetToDocument(vet);
            
            if (vet.getId() == null) {
                // Insert new vet
                collection.insertOne(doc);
                vet.setId(doc.getObjectId("_id"));
            } else {
                // Update existing vet
                collection.replaceOne(
                    Filters.eq("_id", vet.getId()),
                    doc
                );
            }
            
            return vet;
        } catch (Exception e) {
            System.out.println("Error saving vet: " + e.getMessage());
            return null;
        }
    }

    public boolean deleteById(String id) {
        try {
            MongoCollection<Document> collection = getVetsCollection();
            return collection.deleteOne(Filters.eq("_id", new ObjectId(id))).getDeletedCount() > 0;
        } catch (Exception e) {
            System.out.println("Error deleting vet: " + e.getMessage());
            return false;
        }
    }

    public List<Vet> getAllVets() {
        try {
            MongoCollection<Document> collection = getVetsCollection();
            List<Vet> vets = new ArrayList<>();
            
            collection.find().forEach(doc -> {
                Vet vet = documentToVet(doc);
                if (vet != null) {
                    vets.add(vet);
                }
            });
            
            return vets;
        } catch (Exception e) {
            System.out.println("Error getting all vets: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Vet createVet(String name, String email, String password, String phone,
                        String location, String address, String profilePicture,
                        String specialization, Double latitude, Double longitude) {
        try {
            // Check if vet already exists
            if (findByEmail(email) != null) {
                return null; // Vet already exists
            }

            // Generate salt and hash password
            String salt = PasswordUtil.generateSalt();
            String hashedPassword = PasswordUtil.hashPassword(password, salt);
            
            // Create vet with current timestamp
            String dateJoined = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            Vet vet = new Vet(name, email, hashedPassword, salt, phone, location, address,
                             profilePicture, dateJoined, specialization, latitude, longitude);
            
            return save(vet);
        } catch (Exception e) {
            System.out.println("Error creating vet: " + e.getMessage());
            return null;
        }
    }

    public boolean verifyPassword(Vet vet, String password) {
        return PasswordUtil.verifyPassword(password, vet.getPassword(), vet.getSalt());
    }

    private Vet documentToVet(Document doc) {
        try {
            Vet vet = new Vet();
            vet.setId(doc.getObjectId("_id"));
            vet.setName(doc.getString("name"));
            vet.setEmail(doc.getString("email"));
            vet.setPassword(doc.getString("password"));
            vet.setSalt(doc.getString("salt"));
            vet.setPhone(doc.getString("phone"));
            vet.setLocation(doc.getString("location"));
            vet.setAddress(doc.getString("address"));
            vet.setProfilePicture(doc.getString("profilePicture"));
            vet.setDateJoined(doc.getString("dateJoined"));
            vet.setSpecialization(doc.getString("specialization"));
            vet.setLatitude(doc.getDouble("latitude"));
            vet.setLongitude(doc.getDouble("longitude"));
            return vet;
        } catch (Exception e) {
            System.out.println("Error converting document to vet: " + e.getMessage());
            return null;
        }
    }

    private Document vetToDocument(Vet vet) {
        Document doc = new Document();
        if (vet.getId() != null) {
            doc.append("_id", vet.getId());
        }
        doc.append("name", vet.getName())
           .append("email", vet.getEmail())
           .append("password", vet.getPassword())
           .append("salt", vet.getSalt())
           .append("phone", vet.getPhone())
           .append("location", vet.getLocation())
           .append("address", vet.getAddress())
           .append("profilePicture", vet.getProfilePicture())
           .append("dateJoined", vet.getDateJoined())
           .append("specialization", vet.getSpecialization())
           .append("latitude", vet.getLatitude())
           .append("longitude", vet.getLongitude());
        return doc;
    }
}
