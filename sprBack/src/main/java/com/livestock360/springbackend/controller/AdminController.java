package com.livestock360.springbackend.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.livestock360.springbackend.model.Admin;
import com.livestock360.springbackend.model.Subscription;
import com.livestock360.springbackend.service.AdminService;
import com.livestock360.springbackend.service.SubscriptionService;
import com.livestock360.springbackend.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private SubscriptionService subscriptionService;

    // Admin login
    @PostMapping("/login")
    public ResponseEntity<String> adminLogin(@RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            String password = (String) request.get("password");

            if (email == null || email.trim().isEmpty()) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Email is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
            }

            if (password == null || password.trim().isEmpty()) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Password is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
            }

            // Find admin by email
            Admin admin = adminService.findByEmail(email);
            
            if (admin == null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Invalid email or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
            }

            // Check if admin is active
            if (!admin.getIsActive()) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Admin account is deactivated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
            }

            // Verify password
            if (!adminService.verifyPassword(admin, password)) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Invalid email or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
            }

            // Update last login
            adminService.updateLastLogin(admin.getId().toString());

            // Generate JWT token for admin
            String token = JwtUtil.generateToken(
                    admin.getId().toString(),
                    admin.getEmail(),
                    admin.getName(),
                    "admin"
            );

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Admin login successful");
            response.addProperty("token", token);
            
            JsonObject adminData = new JsonObject();
            adminData.addProperty("id", admin.getId().toString());
            adminData.addProperty("email", admin.getEmail());
            adminData.addProperty("name", admin.getName());
            adminData.addProperty("role", admin.getRole());
            adminData.addProperty("userType", "admin");
            
            response.add("admin", adminData);
            
            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error during admin login: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    // Get all subscriptions for admin dashboard
    @GetMapping("/subscriptions")
    public ResponseEntity<String> getAllSubscriptions(@RequestParam(required = false) String status,
                                                     @RequestHeader("Authorization") String authHeader) {
        try {
            // Validate admin token
            String token = authHeader.replace("Bearer ", "");
            String userType = JwtUtil.getUserTypeFromToken(token);
            String adminId = JwtUtil.getUserIdFromToken(token);
            
            if (adminId == null || !"admin".equals(userType)) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Unauthorized access - Admin only");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
            }

            List<Subscription> subscriptions = subscriptionService.findAll(status);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Subscriptions retrieved successfully");
            
            JsonArray subscriptionsArray = new JsonArray();
            for (Subscription subscription : subscriptions) {
                JsonObject subObj = new JsonObject();
                subObj.addProperty("id", subscription.getId().toString());
                subObj.addProperty("userId", subscription.getUserId());
                subObj.addProperty("userEmail", subscription.getUserEmail());
                subObj.addProperty("userName", subscription.getUserName());
                subObj.addProperty("amount", subscription.getAmount());
                subObj.addProperty("tokens", subscription.getTokens());
                subObj.addProperty("transactionId", subscription.getTransactionId());
                subObj.addProperty("subscriptionPackage", subscription.getSubscriptionPackage());
                subObj.addProperty("status", subscription.getStatus());
                subObj.addProperty("createdAt", subscription.getCreatedAt().toString());
                subObj.addProperty("updatedAt", subscription.getUpdatedAt().toString());
                
                if (subscription.getAdminNotes() != null) {
                    subObj.addProperty("adminNotes", subscription.getAdminNotes());
                }
                
                subscriptionsArray.add(subObj);
            }
            
            response.add("subscriptions", subscriptionsArray);
            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error getting admin subscriptions: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    // Approve or reject subscription
    @PutMapping("/subscriptions/{subscriptionId}/status")
    public ResponseEntity<String> updateSubscriptionStatus(@PathVariable String subscriptionId,
                                                          @RequestBody Map<String, Object> request,
                                                          @RequestHeader("Authorization") String authHeader) {
        try {
            // Validate admin token
            String token = authHeader.replace("Bearer ", "");
            String userType = JwtUtil.getUserTypeFromToken(token);
            String adminId = JwtUtil.getUserIdFromToken(token);
            
            if (adminId == null || !"admin".equals(userType)) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Unauthorized access - Admin only");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
            }

            String status = (String) request.get("status");
            String adminNotes = (String) request.get("adminNotes");

            if (status == null || status.trim().isEmpty()) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Status is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
            }

            // Validate status
            if (!"approved".equals(status) && !"rejected".equals(status)) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Status must be 'approved' or 'rejected'");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
            }

            // Check if subscription exists
            Subscription subscription = subscriptionService.findById(subscriptionId);
            if (subscription == null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Subscription not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response.toString());
            }

            // Check if subscription is still pending
            if (!"pending".equals(subscription.getStatus())) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Subscription has already been processed");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
            }

            // Update subscription status
            boolean updated = subscriptionService.updateStatus(subscriptionId, status, adminNotes);
            
            if (updated) {
                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("message", "Subscription " + status + " successfully");
                response.addProperty("subscriptionId", subscriptionId);
                response.addProperty("newStatus", status);
                
                // If approved, mention token credit
                if ("approved".equals(status)) {
                    response.addProperty("tokensCredited", subscription.getTokens());
                }
                
                return ResponseEntity.ok(response.toString());
            } else {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Failed to update subscription status");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
            }

        } catch (Exception e) {
            System.out.println("Error updating subscription status: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    // Get admin dashboard statistics
    @GetMapping("/stats")
    public ResponseEntity<String> getAdminStats(@RequestHeader("Authorization") String authHeader) {
        try {
            // Validate admin token
            String token = authHeader.replace("Bearer ", "");
            String userType = JwtUtil.getUserTypeFromToken(token);
            String adminId = JwtUtil.getUserIdFromToken(token);
            
            if (adminId == null || !"admin".equals(userType)) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Unauthorized access - Admin only");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
            }

            Map<String, Object> stats = subscriptionService.getSubscriptionStats();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Admin statistics retrieved successfully");
            
            JsonObject statsObj = new JsonObject();
            statsObj.addProperty("totalSubscriptions", (Number) stats.get("totalSubscriptions"));
            statsObj.addProperty("pendingSubscriptions", (Number) stats.get("pendingSubscriptions"));
            statsObj.addProperty("approvedSubscriptions", (Number) stats.get("approvedSubscriptions"));
            statsObj.addProperty("rejectedSubscriptions", (Number) stats.get("rejectedSubscriptions"));
            statsObj.addProperty("totalRevenue", (Number) stats.get("totalRevenue"));
            
            response.add("stats", statsObj);
            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error getting admin stats: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }
}