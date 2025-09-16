package com.livestock360.springbackend.service;

import com.livestock360.springbackend.model.Admin;
import com.livestock360.springbackend.utils.PasswordUtil;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AdminService {

    @Autowired
    private MongoDatabase mongoDatabase;

    private MongoCollection<Document> getAdminsCollection() {
        return mongoDatabase.getCollection("admins");
    }

    // Find admin by email
    public Admin findByEmail(String email) {
        try {
            MongoCollection<Document> collection = getAdminsCollection();
            Document doc = collection.find(Filters.eq("email", email)).first();
            return doc != null ? documentToAdmin(doc) : null;
        } catch (Exception e) {
            System.out.println("Error finding admin by email: " + e.getMessage());
            return null;
        }
    }

    // Find admin by ID
    public Admin findById(String id) {
        try {
            MongoCollection<Document> collection = getAdminsCollection();
            Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
            return doc != null ? documentToAdmin(doc) : null;
        } catch (Exception e) {
            System.out.println("Error finding admin by ID: " + e.getMessage());
            return null;
        }
    }

    // Save admin
    public Admin save(Admin admin) {
        try {
            MongoCollection<Document> collection = getAdminsCollection();
            Document doc = adminToDocument(admin);
            
            if (admin.getId() != null) {
                // Update existing admin
                collection.replaceOne(Filters.eq("_id", admin.getId()), doc);
            } else {
                // Insert new admin
                collection.insertOne(doc);
                admin.setId(doc.getObjectId("_id"));
            }
            
            return admin;
        } catch (Exception e) {
            System.out.println("Error saving admin: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Verify admin password
    public boolean verifyPassword(Admin admin, String password) {
        try {
            String hashedPassword = PasswordUtil.hashPassword(password, admin.getSalt());
            return hashedPassword.equals(admin.getPassword());
        } catch (Exception e) {
            System.out.println("Error verifying admin password: " + e.getMessage());
            return false;
        }
    }

    // Set admin password with salt
    public Admin setPassword(Admin admin, String password) {
        try {
            String salt = PasswordUtil.generateSalt();
            String hashedPassword = PasswordUtil.hashPassword(password, salt);
            
            admin.setSalt(salt);
            admin.setPassword(hashedPassword);
            
            return admin;
        } catch (Exception e) {
            System.out.println("Error setting admin password: " + e.getMessage());
            throw new RuntimeException("Failed to set admin password", e);
        }
    }

    // Create default admin if not exists
    public void createDefaultAdminIfNotExists() {
        try {
            Admin existingAdmin = findByEmail("admin@gmail.com");
            
            if (existingAdmin == null) {
                System.out.println("Creating default admin account...");
                
                Admin defaultAdmin = new Admin();
                defaultAdmin.setEmail("admin@gmail.com");
                defaultAdmin.setName("System Administrator");
                defaultAdmin.setRole("admin");
                defaultAdmin.setIsActive(true);
                
                // Set password to "654321"
                defaultAdmin = setPassword(defaultAdmin, "654321");
                
                Admin savedAdmin = save(defaultAdmin);
                
                if (savedAdmin != null) {
                    System.out.println("Default admin created successfully with email: admin@gmail.com");
                } else {
                    System.out.println("Failed to create default admin");
                }
            } else {
                System.out.println("Default admin already exists");
            }
        } catch (Exception e) {
            System.out.println("Error creating default admin: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Update admin last login
    public void updateLastLogin(String adminId) {
        try {
            MongoCollection<Document> collection = getAdminsCollection();
            collection.updateOne(
                    Filters.eq("_id", new ObjectId(adminId)),
                    new Document("$set", new Document()
                            .append("lastLoginAt", new Date())
                            .append("updatedAt", new Date()))
            );
        } catch (Exception e) {
            System.out.println("Error updating admin last login: " + e.getMessage());
        }
    }

    // Check if admin exists
    public boolean adminExists(String email) {
        try {
            MongoCollection<Document> collection = getAdminsCollection();
            long count = collection.countDocuments(Filters.eq("email", email));
            return count > 0;
        } catch (Exception e) {
            System.out.println("Error checking if admin exists: " + e.getMessage());
            return false;
        }
    }

    // Get admin count
    public long getAdminCount() {
        try {
            MongoCollection<Document> collection = getAdminsCollection();
            return collection.countDocuments();
        } catch (Exception e) {
            System.out.println("Error getting admin count: " + e.getMessage());
            return 0;
        }
    }

    // Helper method to convert Admin to Document
    private Document adminToDocument(Admin admin) {
        Document doc = new Document()
                .append("email", admin.getEmail())
                .append("password", admin.getPassword())
                .append("salt", admin.getSalt())
                .append("name", admin.getName())
                .append("isActive", admin.getIsActive())
                .append("role", admin.getRole())
                .append("createdAt", admin.getCreatedAt())
                .append("updatedAt", admin.getUpdatedAt());

        if (admin.getLastLoginAt() != null) {
            doc.append("lastLoginAt", admin.getLastLoginAt());
        }

        return doc;
    }

    // Helper method to convert Document to Admin
    private Admin documentToAdmin(Document doc) {
        Admin admin = new Admin();
        admin.setId(doc.getObjectId("_id"));
        admin.setEmail(doc.getString("email"));
        admin.setPassword(doc.getString("password"));
        admin.setSalt(doc.getString("salt"));
        admin.setName(doc.getString("name"));
        admin.setIsActive(doc.getBoolean("isActive"));
        admin.setRole(doc.getString("role"));
        admin.setCreatedAt(doc.getDate("createdAt"));
        admin.setUpdatedAt(doc.getDate("updatedAt"));
        admin.setLastLoginAt(doc.getDate("lastLoginAt"));

        return admin;
    }
}