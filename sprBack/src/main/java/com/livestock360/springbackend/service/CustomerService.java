package com.livestock360.springbackend.service;

import com.livestock360.springbackend.model.Customer;
import com.livestock360.springbackend.utils.PasswordUtil;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final MongoDatabase database;

    @Autowired
    public CustomerService(MongoDatabase database) {
        this.database = database;
    }

    private MongoCollection<Document> getCustomersCollection() {
        return database.getCollection("customers");
    }

    // Find customer by email (for authentication)
    public Customer findByEmail(String email) {
        try {
            MongoCollection<Document> collection = getCustomersCollection();
            Document doc = collection.find(Filters.eq("email", email)).first();
            
            if (doc != null) {
                return documentToCustomer(doc);
            }
            return null;
        } catch (Exception e) {
            System.out.println("Error finding customer by email: " + e.getMessage());
            return null;
        }
    }

    // Find customer by ID
    public Customer findById(String id) {
        try {
            MongoCollection<Document> collection = getCustomersCollection();
            Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
            
            if (doc != null) {
                return documentToCustomer(doc);
            }
            return null;
        } catch (Exception e) {
            System.out.println("Error finding customer by ID: " + e.getMessage());
            return null;
        }
    }

    // Save or update customer
    public Customer save(Customer customer) {
        try {
            MongoCollection<Document> collection = getCustomersCollection();
            Document doc = customerToDocument(customer);
            
            if (customer.getId() == null) {
                // Insert new customer
                collection.insertOne(doc);
                customer.setId(doc.getObjectId("_id"));
            } else {
                // Update existing customer
                customer.setUpdatedAt(new Date());
                doc = customerToDocument(customer);
                collection.replaceOne(
                    Filters.eq("_id", customer.getId()),
                    doc
                );
            }
            
            return customer;
        } catch (Exception e) {
            System.out.println("Error saving customer: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Delete customer by ID
    public boolean deleteById(String id) {
        try {
            MongoCollection<Document> collection = getCustomersCollection();
            long deletedCount = collection.deleteOne(Filters.eq("_id", new ObjectId(id))).getDeletedCount();
            return deletedCount > 0;
        } catch (Exception e) {
            System.out.println("Error deleting customer: " + e.getMessage());
            return false;
        }
    }

    // Update customer profile
    public Customer updateCustomer(Customer customer) {
        if (customer.getId() == null) {
            return null;
        }
        customer.setUpdatedAt(new Date());
        return save(customer);
    }

    // Find all customers (for admin purposes)
    public List<Customer> findAll() {
        return findAll(0, 0); // No pagination by default
    }

    public List<Customer> findAll(int page, int limit) {
        try {
            MongoCollection<Document> collection = getCustomersCollection();
            List<Document> docs;
            
            if (limit > 0) {
                docs = collection.find()
                    .sort(Sorts.descending("createdAt"))
                    .skip((page - 1) * limit)
                    .limit(limit)
                    .into(new ArrayList<>());
            } else {
                docs = collection.find()
                    .sort(Sorts.descending("createdAt"))
                    .into(new ArrayList<>());
            }
            
            return docs.stream()
                .map(this::documentToCustomer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Error finding all customers: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Find customers by location
    public List<Customer> findByLocation(String location) {
        try {
            MongoCollection<Document> collection = getCustomersCollection();
            List<Document> docs = collection.find(
                Filters.regex("location", ".*" + location + ".*", "i")
            ).sort(Sorts.ascending("name")).into(new ArrayList<>());
            
            return docs.stream()
                .map(this::documentToCustomer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Error finding customers by location: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Search customers by name or email
    public List<Customer> searchCustomers(String searchTerm) {
        try {
            MongoCollection<Document> collection = getCustomersCollection();
            List<Document> docs = collection.find(
                Filters.or(
                    Filters.regex("name", ".*" + searchTerm + ".*", "i"),
                    Filters.regex("email", ".*" + searchTerm + ".*", "i"),
                    Filters.regex("businessName", ".*" + searchTerm + ".*", "i")
                )
            ).sort(Sorts.ascending("name")).into(new ArrayList<>());
            
            return docs.stream()
                .map(this::documentToCustomer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Error searching customers: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Find customers by customer type
    public List<Customer> findByCustomerType(String customerType) {
        try {
            MongoCollection<Document> collection = getCustomersCollection();
            List<Document> docs = collection.find(
                Filters.eq("customerType", customerType)
            ).sort(Sorts.ascending("name")).into(new ArrayList<>());
            
            return docs.stream()
                .map(this::documentToCustomer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Error finding customers by type: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Find customers interested in specific animal types
    public List<Customer> findByInterestedAnimalType(String animalType) {
        try {
            MongoCollection<Document> collection = getCustomersCollection();
            List<Document> docs = collection.find(
                Filters.in("interestedAnimalTypes", animalType)
            ).sort(Sorts.ascending("name")).into(new ArrayList<>());
            
            return docs.stream()
                .map(this::documentToCustomer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Error finding customers by animal interest: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Update last login timestamp
    public void updateLastLogin(String customerId) {
        try {
            MongoCollection<Document> collection = getCustomersCollection();
            collection.updateOne(
                Filters.eq("_id", new ObjectId(customerId)),
                new Document("$set", new Document("lastLoginAt", new Date()).append("updatedAt", new Date()))
            );
        } catch (Exception e) {
            System.out.println("Error updating last login: " + e.getMessage());
        }
    }

    // Update customer purchase statistics
    public void updatePurchaseStats(String customerId, double amount) {
        try {
            MongoCollection<Document> collection = getCustomersCollection();
            collection.updateOne(
                Filters.eq("_id", new ObjectId(customerId)),
                new Document("$inc", new Document("totalPurchases", 1).append("totalSpent", amount))
                    .append("$set", new Document("updatedAt", new Date()))
            );
        } catch (Exception e) {
            System.out.println("Error updating purchase stats: " + e.getMessage());
        }
    }

    // Get customer statistics
    public Map<String, Object> getCustomerStats() {
        try {
            MongoCollection<Document> collection = getCustomersCollection();
            
            long totalCustomers = collection.countDocuments();
            long verifiedCustomers = collection.countDocuments(Filters.eq("isVerified", true));
            long activeCustomers = collection.countDocuments(Filters.eq("isActive", true));
            long businessCustomers = collection.countDocuments(Filters.eq("customerType", "business"));
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCustomers", totalCustomers);
            stats.put("verifiedCustomers", verifiedCustomers);
            stats.put("activeCustomers", activeCustomers);
            stats.put("businessCustomers", businessCustomers);
            stats.put("individualCustomers", totalCustomers - businessCustomers);
            stats.put("verificationRate", totalCustomers > 0 ? (double) verifiedCustomers / totalCustomers * 100 : 0.0);
            
            return stats;
        } catch (Exception e) {
            System.out.println("Error getting customer stats: " + e.getMessage());
            return new HashMap<>();
        }
    }

    // Convert Document to Customer
    private Customer documentToCustomer(Document doc) {
        try {
            Customer customer = new Customer();
            customer.setId(doc.getObjectId("_id"));
            customer.setName(doc.getString("name"));
            customer.setEmail(doc.getString("email"));
            customer.setPassword(doc.getString("password"));
            customer.setSalt(doc.getString("salt"));
            customer.setPhone(doc.getString("phone"));
            customer.setLocation(doc.getString("location"));
            customer.setAddress(doc.getString("address"));
            customer.setProfilePicture(doc.getString("profilePicture"));
            customer.setDateJoined(doc.getString("dateJoined"));
            customer.setCreatedAt(doc.getDate("createdAt"));
            customer.setUpdatedAt(doc.getDate("updatedAt"));
            
            // Handle optional marketplace-specific fields
            @SuppressWarnings("unchecked")
            List<String> interestedTypes = (List<String>) doc.get("interestedAnimalTypes");
            customer.setInterestedAnimalTypes(interestedTypes);
            customer.setPreferredLocation(doc.getString("preferredLocation"));
            customer.setMaxBudget(doc.getDouble("maxBudget"));
            customer.setPaymentMethod(doc.getString("paymentMethod"));
            customer.setIsVerified(doc.getBoolean("isVerified", false));
            customer.setIsActive(doc.getBoolean("isActive", true));
            customer.setCustomerType(doc.getString("customerType"));
            customer.setBusinessName(doc.getString("businessName"));
            customer.setBusinessLicense(doc.getString("businessLicense"));
            customer.setTotalPurchases(doc.getInteger("totalPurchases", 0));
            Double totalSpent = doc.getDouble("totalSpent");
            customer.setTotalSpent(totalSpent != null ? totalSpent : 0.0);
            customer.setLastLoginAt(doc.getDate("lastLoginAt"));
            customer.setPreferredCommunication(doc.getString("preferredCommunication"));
            
            return customer;
        } catch (Exception e) {
            System.out.println("Error converting document to customer: " + e.getMessage());
            return null;
        }
    }

    // Convert Customer to Document
    private Document customerToDocument(Customer customer) {
        Document doc = new Document();
        if (customer.getId() != null) {
            doc.append("_id", customer.getId());
        }
        doc.append("name", customer.getName())
           .append("email", customer.getEmail())
           .append("password", customer.getPassword())
           .append("salt", customer.getSalt())
           .append("phone", customer.getPhone())
           .append("location", customer.getLocation())
           .append("address", customer.getAddress())
           .append("profilePicture", customer.getProfilePicture())
           .append("dateJoined", customer.getDateJoined())
           .append("createdAt", customer.getCreatedAt())
           .append("updatedAt", customer.getUpdatedAt())
           .append("interestedAnimalTypes", customer.getInterestedAnimalTypes())
           .append("preferredLocation", customer.getPreferredLocation())
           .append("maxBudget", customer.getMaxBudget())
           .append("paymentMethod", customer.getPaymentMethod())
           .append("isVerified", customer.getIsVerified())
           .append("isActive", customer.getIsActive())
           .append("customerType", customer.getCustomerType())
           .append("businessName", customer.getBusinessName())
           .append("businessLicense", customer.getBusinessLicense())
           .append("totalPurchases", customer.getTotalPurchases())
           .append("totalSpent", customer.getTotalSpent())
           .append("lastLoginAt", customer.getLastLoginAt())
           .append("preferredCommunication", customer.getPreferredCommunication());
        return doc;
    }

    // Password management methods
    public Customer setPassword(Customer customer, String password) {
        try {
            String salt = PasswordUtil.generateSalt();
            String hashedPassword = PasswordUtil.hashPassword(password, salt);
            
            customer.setSalt(salt);
            customer.setPassword(hashedPassword);
            
            return customer;
        } catch (Exception e) {
            System.out.println("Error setting password for customer: " + e.getMessage());
            throw new RuntimeException("Failed to set password", e);
        }
    }

    public boolean verifyPassword(Customer customer, String password) {
        try {
            String hashedPassword = PasswordUtil.hashPassword(password, customer.getSalt());
            return hashedPassword.equals(customer.getPassword());
        } catch (Exception e) {
            System.out.println("Error verifying password for customer: " + e.getMessage());
            return false;
        }
    }
}