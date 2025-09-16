package com.livestock360.springbackend.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.livestock360.springbackend.model.Subscription;
import com.livestock360.springbackend.model.Farmer;
import com.livestock360.springbackend.service.SubscriptionService;
import com.livestock360.springbackend.service.FarmerService;
import com.livestock360.springbackend.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "*")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private FarmerService farmerService;

    // Submit a new subscription request
    @PostMapping("/submit")
    public ResponseEntity<String> submitSubscription(@RequestBody Map<String, Object> request,
                                                   @RequestHeader("Authorization") String authHeader) {
        try {
            // Validate JWT token
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            String userType = JwtUtil.getUserTypeFromToken(token);
            
            if (userId == null || !"farmer".equals(userType)) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Invalid or unauthorized token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
            }

            // Get farmer details
            Farmer farmer = farmerService.findById(userId);
            if (farmer == null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Farmer not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response.toString());
            }

            // Validate required fields
            String transactionId = (String) request.get("transactionId");
            String subscriptionPackage = (String) request.get("subscriptionPackage");

            if (transactionId == null || transactionId.trim().isEmpty()) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Transaction ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
            }

            if (subscriptionPackage == null || subscriptionPackage.trim().isEmpty()) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Subscription package is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
            }

            // Check if transaction ID already exists
            if (subscriptionService.isTransactionIdExists(transactionId)) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "This transaction ID has already been used");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response.toString());
            }

            // Validate subscription package and get amount/tokens
            Double amount;
            Integer tokens;

            switch (subscriptionPackage) {
                case "basic":
                    amount = 50.0;
                    tokens = 10;
                    break;
                case "standard":
                    amount = 100.0;
                    tokens = 25;
                    break;
                case "premium":
                    amount = 500.0;
                    tokens = 150;
                    break;
                default:
                    JsonObject response = new JsonObject();
                    response.addProperty("success", false);
                    response.addProperty("message", "Invalid subscription package");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
            }

            // Create subscription
            Subscription subscription = new Subscription(
                    userId,
                    "farmer",
                    farmer.getEmail(),
                    farmer.getName(),
                    amount,
                    tokens,
                    transactionId,
                    subscriptionPackage
            );

            Subscription savedSubscription = subscriptionService.save(subscription);

            if (savedSubscription != null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("message", "Subscription request submitted successfully");
                
                JsonObject subscriptionData = new JsonObject();
                subscriptionData.addProperty("id", savedSubscription.getId().toString());
                subscriptionData.addProperty("amount", savedSubscription.getAmount());
                subscriptionData.addProperty("tokens", savedSubscription.getTokens());
                subscriptionData.addProperty("package", savedSubscription.getSubscriptionPackage());
                subscriptionData.addProperty("status", savedSubscription.getStatus());
                subscriptionData.addProperty("transactionId", savedSubscription.getTransactionId());
                subscriptionData.addProperty("createdAt", savedSubscription.getCreatedAt().toString());
                
                response.add("subscription", subscriptionData);
                
                return ResponseEntity.status(HttpStatus.CREATED).body(response.toString());
            } else {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Failed to submit subscription request");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
            }

        } catch (Exception e) {
            System.out.println("Error submitting subscription: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    // Get subscription history for a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<String> getUserSubscriptions(@PathVariable String userId,
                                                      @RequestHeader("Authorization") String authHeader) {
        try {
            // Validate JWT token
            String token = authHeader.replace("Bearer ", "");
            String tokenUserId = JwtUtil.getUserIdFromToken(token);
            
            if (tokenUserId == null || !tokenUserId.equals(userId)) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Unauthorized access");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
            }

            List<Subscription> subscriptions = subscriptionService.findByUserId(userId);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Subscriptions retrieved successfully");
            
            JsonArray subscriptionsArray = new JsonArray();
            for (Subscription subscription : subscriptions) {
                JsonObject subObj = new JsonObject();
                subObj.addProperty("id", subscription.getId().toString());
                subObj.addProperty("amount", subscription.getAmount());
                subObj.addProperty("tokens", subscription.getTokens());
                subObj.addProperty("transactionId", subscription.getTransactionId());
                subObj.addProperty("subscriptionPackage", subscription.getSubscriptionPackage());
                subObj.addProperty("status", subscription.getStatus());
                subObj.addProperty("createdAt", subscription.getCreatedAt().toString());
                
                if (subscription.getAdminNotes() != null) {
                    subObj.addProperty("adminNotes", subscription.getAdminNotes());
                }
                
                subscriptionsArray.add(subObj);
            }
            
            response.add("subscriptions", subscriptionsArray);
            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error getting user subscriptions: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    // Get subscription packages information
    @GetMapping("/packages")
    public ResponseEntity<String> getSubscriptionPackages() {
        try {
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Subscription packages retrieved successfully");
            
            JsonArray packagesArray = new JsonArray();
            
            // Basic package
            JsonObject basicPackage = new JsonObject();
            basicPackage.addProperty("id", "basic");
            basicPackage.addProperty("name", "Basic Package");
            basicPackage.addProperty("amount", 50);
            basicPackage.addProperty("tokens", 10);
            basicPackage.addProperty("description", "50 TK for 10 Tokens");
            packagesArray.add(basicPackage);
            
            // Standard package
            JsonObject standardPackage = new JsonObject();
            standardPackage.addProperty("id", "standard");
            standardPackage.addProperty("name", "Standard Package");
            standardPackage.addProperty("amount", 100);
            standardPackage.addProperty("tokens", 25);
            standardPackage.addProperty("description", "100 TK for 25 Tokens");
            packagesArray.add(standardPackage);
            
            // Premium package
            JsonObject premiumPackage = new JsonObject();
            premiumPackage.addProperty("id", "premium");
            premiumPackage.addProperty("name", "Premium Package");
            premiumPackage.addProperty("amount", 500);
            premiumPackage.addProperty("tokens", 150);
            premiumPackage.addProperty("description", "500 TK for 150 Tokens");
            packagesArray.add(premiumPackage);
            
            response.add("packages", packagesArray);
            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error getting subscription packages: " + e.getMessage());
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    // Get current user's token balance
    @GetMapping("/tokens/balance")
    public ResponseEntity<String> getTokenBalance(@RequestHeader("Authorization") String authHeader) {
        try {
            // Validate JWT token
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            String userType = JwtUtil.getUserTypeFromToken(token);
            
            if (userId == null || !"farmer".equals(userType)) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Invalid or unauthorized token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
            }

            // Get farmer
            Farmer farmer = farmerService.findById(userId);
            if (farmer == null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Farmer not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response.toString());
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Token balance retrieved successfully");
            response.addProperty("tokenBalance", farmer.getTokenCount() != null ? farmer.getTokenCount() : 0);
            
            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error getting token balance: " + e.getMessage());
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }
}