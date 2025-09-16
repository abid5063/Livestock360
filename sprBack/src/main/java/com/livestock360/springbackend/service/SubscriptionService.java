package com.livestock360.springbackend.service;

import com.livestock360.springbackend.model.Subscription;
import com.livestock360.springbackend.model.Farmer;
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
public class SubscriptionService {

    @Autowired
    private MongoDatabase mongoDatabase;

    @Autowired
    private FarmerService farmerService;

    private MongoCollection<Document> getSubscriptionsCollection() {
        return mongoDatabase.getCollection("subscriptions");
    }

    // Save a new subscription
    public Subscription save(Subscription subscription) {
        try {
            MongoCollection<Document> collection = getSubscriptionsCollection();
            Document doc = subscriptionToDocument(subscription);
            collection.insertOne(doc);
            subscription.setId(doc.getObjectId("_id"));
            return subscription;
        } catch (Exception e) {
            System.out.println("Error saving subscription: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Find subscription by ID
    public Subscription findById(String id) {
        try {
            MongoCollection<Document> collection = getSubscriptionsCollection();
            Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
            return doc != null ? documentToSubscription(doc) : null;
        } catch (Exception e) {
            System.out.println("Error finding subscription by ID: " + e.getMessage());
            return null;
        }
    }

    // Find all subscriptions by user ID
    public List<Subscription> findByUserId(String userId) {
        try {
            MongoCollection<Document> collection = getSubscriptionsCollection();
            List<Document> docs = collection.find(Filters.eq("userId", userId))
                    .sort(Sorts.descending("createdAt"))
                    .into(new ArrayList<>());
            
            return docs.stream()
                    .map(this::documentToSubscription)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Error finding subscriptions by user ID: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Find all subscriptions with optional status filter
    public List<Subscription> findAll(String status) {
        try {
            MongoCollection<Document> collection = getSubscriptionsCollection();
            
            List<Document> docs;
            if (status != null && !status.isEmpty()) {
                docs = collection.find(Filters.eq("status", status))
                        .sort(Sorts.descending("createdAt"))
                        .into(new ArrayList<>());
            } else {
                docs = collection.find()
                        .sort(Sorts.descending("createdAt"))
                        .into(new ArrayList<>());
            }
            
            return docs.stream()
                    .map(this::documentToSubscription)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Error finding all subscriptions: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Update subscription status and admin notes
    public boolean updateStatus(String subscriptionId, String status, String adminNotes) {
        try {
            MongoCollection<Document> collection = getSubscriptionsCollection();
            
            Document updateDoc = new Document()
                    .append("status", status)
                    .append("updatedAt", new Date());
            
            if (adminNotes != null && !adminNotes.trim().isEmpty()) {
                updateDoc.append("adminNotes", adminNotes);
            }
            
            long modifiedCount = collection.updateOne(
                    Filters.eq("_id", new ObjectId(subscriptionId)),
                    new Document("$set", updateDoc)
            ).getModifiedCount();
            
            // If approved, credit tokens to farmer
            if ("approved".equals(status) && modifiedCount > 0) {
                creditTokensToFarmer(subscriptionId);
            }
            
            return modifiedCount > 0;
        } catch (Exception e) {
            System.out.println("Error updating subscription status: " + e.getMessage());
            return false;
        }
    }

    // Credit tokens to farmer when subscription is approved
    private void creditTokensToFarmer(String subscriptionId) {
        try {
            Subscription subscription = findById(subscriptionId);
            if (subscription != null && "farmer".equals(subscription.getUserType())) {
                Farmer farmer = farmerService.findById(subscription.getUserId());
                if (farmer != null) {
                    farmer.addTokens(subscription.getTokens());
                    farmerService.save(farmer);
                    System.out.println("Credited " + subscription.getTokens() + " tokens to farmer: " + farmer.getName());
                }
            }
        } catch (Exception e) {
            System.out.println("Error crediting tokens to farmer: " + e.getMessage());
        }
    }

    // Get subscription statistics for admin dashboard
    public Map<String, Object> getSubscriptionStats() {
        try {
            MongoCollection<Document> collection = getSubscriptionsCollection();
            
            long totalSubscriptions = collection.countDocuments();
            long pendingSubscriptions = collection.countDocuments(Filters.eq("status", "pending"));
            long approvedSubscriptions = collection.countDocuments(Filters.eq("status", "approved"));
            long rejectedSubscriptions = collection.countDocuments(Filters.eq("status", "rejected"));
            
            // Calculate total revenue from approved subscriptions
            List<Document> approvedDocs = collection.find(Filters.eq("status", "approved"))
                    .into(new ArrayList<>());
            
            double totalRevenue = approvedDocs.stream()
                    .mapToDouble(doc -> doc.getDouble("amount"))
                    .sum();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalSubscriptions", totalSubscriptions);
            stats.put("pendingSubscriptions", pendingSubscriptions);
            stats.put("approvedSubscriptions", approvedSubscriptions);
            stats.put("rejectedSubscriptions", rejectedSubscriptions);
            stats.put("totalRevenue", totalRevenue);
            
            return stats;
        } catch (Exception e) {
            System.out.println("Error getting subscription stats: " + e.getMessage());
            return new HashMap<>();
        }
    }

    // Helper method to convert Subscription to Document
    private Document subscriptionToDocument(Subscription subscription) {
        Document doc = new Document()
                .append("userId", subscription.getUserId())
                .append("userType", subscription.getUserType())
                .append("userEmail", subscription.getUserEmail())
                .append("userName", subscription.getUserName())
                .append("amount", subscription.getAmount())
                .append("tokens", subscription.getTokens())
                .append("transactionId", subscription.getTransactionId())
                .append("subscriptionPackage", subscription.getSubscriptionPackage())
                .append("status", subscription.getStatus())
                .append("createdAt", subscription.getCreatedAt())
                .append("updatedAt", subscription.getUpdatedAt());
        
        if (subscription.getAdminNotes() != null) {
            doc.append("adminNotes", subscription.getAdminNotes());
        }
        
        return doc;
    }

    // Helper method to convert Document to Subscription
    private Subscription documentToSubscription(Document doc) {
        Subscription subscription = new Subscription();
        subscription.setId(doc.getObjectId("_id"));
        subscription.setUserId(doc.getString("userId"));
        subscription.setUserType(doc.getString("userType"));
        subscription.setUserEmail(doc.getString("userEmail"));
        subscription.setUserName(doc.getString("userName"));
        subscription.setAmount(doc.getDouble("amount"));
        subscription.setTokens(doc.getInteger("tokens"));
        subscription.setTransactionId(doc.getString("transactionId"));
        subscription.setSubscriptionPackage(doc.getString("subscriptionPackage"));
        subscription.setStatus(doc.getString("status"));
        subscription.setCreatedAt(doc.getDate("createdAt"));
        subscription.setUpdatedAt(doc.getDate("updatedAt"));
        subscription.setAdminNotes(doc.getString("adminNotes"));
        
        return subscription;
    }

    // Check if transaction ID already exists (to prevent duplicate submissions)
    public boolean isTransactionIdExists(String transactionId) {
        try {
            MongoCollection<Document> collection = getSubscriptionsCollection();
            long count = collection.countDocuments(Filters.eq("transactionId", transactionId));
            return count > 0;
        } catch (Exception e) {
            System.out.println("Error checking transaction ID: " + e.getMessage());
            return false;
        }
    }
}