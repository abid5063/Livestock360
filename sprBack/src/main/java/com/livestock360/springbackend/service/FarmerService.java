package com.livestock360.springbackend.service;

import com.google.gson.Gson;
import com.livestock360.springbackend.model.Farmer;
import com.livestock360.springbackend.utils.PasswordUtil;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class FarmerService {

    private final MongoDatabase database;
    private final Gson gson;

    @Autowired
    public FarmerService(MongoDatabase database) {
        this.database = database;
        this.gson = new Gson();
    }

    private MongoCollection<Document> getFarmersCollection() {
        return database.getCollection("farmers");
    }

    public Farmer findByEmail(String email) {
        try {
            MongoCollection<Document> collection = getFarmersCollection();
            Document doc = collection.find(Filters.eq("email", email)).first();
            
            if (doc != null) {
                return documentToFarmer(doc);
            }
            return null;
        } catch (Exception e) {
            System.out.println("Error finding farmer by email: " + e.getMessage());
            return null;
        }
    }

    public Farmer findById(String id) {
        try {
            MongoCollection<Document> collection = getFarmersCollection();
            Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
            
            if (doc != null) {
                return documentToFarmer(doc);
            }
            return null;
        } catch (Exception e) {
            System.out.println("Error finding farmer by ID: " + e.getMessage());
            return null;
        }
    }

    public Farmer save(Farmer farmer) {
        try {
            MongoCollection<Document> collection = getFarmersCollection();
            Document doc = farmerToDocument(farmer);
            
            if (farmer.getId() == null) {
                // Insert new farmer
                collection.insertOne(doc);
                farmer.setId(doc.getObjectId("_id"));
            } else {
                // Update existing farmer
                collection.replaceOne(
                    Filters.eq("_id", farmer.getId()),
                    doc
                );
            }
            
            return farmer;
        } catch (Exception e) {
            System.out.println("Error saving farmer: " + e.getMessage());
            return null;
        }
    }

    public boolean deleteById(String id) {
        try {
            MongoCollection<Document> collection = getFarmersCollection();
            return collection.deleteOne(Filters.eq("_id", new ObjectId(id))).getDeletedCount() > 0;
        } catch (Exception e) {
            System.out.println("Error deleting farmer: " + e.getMessage());
            return false;
        }
    }

    public List<Farmer> searchFarmers(String query) {
        try {
            MongoCollection<Document> collection = getFarmersCollection();
            List<Farmer> farmers = new ArrayList<>();
            
            // Create case-insensitive regex pattern
            Pattern pattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE);
            
            // Search in name, email, location, and address fields
            Document searchFilter = new Document("$or", List.of(
                new Document("name", pattern),
                new Document("email", pattern),
                new Document("location", pattern),
                new Document("address", pattern)
            ));
            
            collection.find(searchFilter).forEach(doc -> {
                Farmer farmer = documentToFarmer(doc);
                if (farmer != null) {
                    farmers.add(farmer);
                }
            });
            
            return farmers;
        } catch (Exception e) {
            System.out.println("Error searching farmers: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Farmer> getAllFarmers() {
        try {
            MongoCollection<Document> collection = getFarmersCollection();
            List<Farmer> farmers = new ArrayList<>();
            
            collection.find().forEach(doc -> {
                Farmer farmer = documentToFarmer(doc);
                if (farmer != null) {
                    farmers.add(farmer);
                }
            });
            
            return farmers;
        } catch (Exception e) {
            System.out.println("Error getting all farmers: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Farmer createFarmer(String name, String email, String password, String phone,
                              String location, String address, String profilePicture) {
        try {
            System.out.println("FarmerService.createFarmer called with email: " + email);
            
            // Check if farmer already exists
            Farmer existingFarmer = findByEmail(email);
            if (existingFarmer != null) {
                System.out.println("Farmer already exists with email: " + email);
                return null; // Farmer already exists
            }

            System.out.println("Generating password salt and hash...");
            // Generate salt and hash password
            String salt = PasswordUtil.generateSalt();
            String hashedPassword = PasswordUtil.hashPassword(password, salt);
            System.out.println("Password hashed successfully. Salt length: " + salt.length());
            
            // Create farmer with current timestamp
            String dateJoined = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            System.out.println("Date joined: " + dateJoined);
            
            Farmer farmer = new Farmer(name, email, hashedPassword, salt, phone, 
                                     location, address, profilePicture, dateJoined);
            
            System.out.println("Farmer object created, calling save...");
            Farmer savedFarmer = save(farmer);
            System.out.println("Save operation completed. Result: " + (savedFarmer != null ? "Success" : "Failed"));
            
            return savedFarmer;
        } catch (Exception e) {
            System.out.println("Error creating farmer: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public boolean verifyPassword(Farmer farmer, String password) {
        return PasswordUtil.verifyPassword(password, farmer.getPassword(), farmer.getSalt());
    }

    private Farmer documentToFarmer(Document doc) {
        try {
            Farmer farmer = new Farmer();
            farmer.setId(doc.getObjectId("_id"));
            farmer.setName(doc.getString("name"));
            farmer.setEmail(doc.getString("email"));
            farmer.setPassword(doc.getString("password"));
            farmer.setSalt(doc.getString("salt"));
            farmer.setPhone(doc.getString("phone"));
            farmer.setLocation(doc.getString("location"));
            farmer.setAddress(doc.getString("address"));
            farmer.setProfilePicture(doc.getString("profilePicture"));
            farmer.setDateJoined(doc.getString("dateJoined"));
            return farmer;
        } catch (Exception e) {
            System.out.println("Error converting document to farmer: " + e.getMessage());
            return null;
        }
    }

    private Document farmerToDocument(Farmer farmer) {
        Document doc = new Document();
        if (farmer.getId() != null) {
            doc.append("_id", farmer.getId());
        }
        doc.append("name", farmer.getName())
           .append("email", farmer.getEmail())
           .append("password", farmer.getPassword())
           .append("salt", farmer.getSalt())
           .append("phone", farmer.getPhone())
           .append("location", farmer.getLocation())
           .append("address", farmer.getAddress())
           .append("profilePicture", farmer.getProfilePicture())
           .append("dateJoined", farmer.getDateJoined());
        return doc;
    }
}
