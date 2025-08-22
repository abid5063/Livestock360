package com.livestock360.backend;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import com.google.gson.Gson;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.bson.types.ObjectId;
import static com.mongodb.client.model.Filters.*;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import io.jsonwebtoken.Claims;

public class SimpleBackend {
    private static MongoDatabase database;
    private static Gson gson = new Gson();
    private static final Type stringMapType = new TypeToken<Map<String, String>>(){}.getType();
    private static final Type objectMapType = new TypeToken<Map<String, Object>>(){}.getType();
    
    public static void main(String[] args) throws IOException {
        // Connect to MongoDB Atlas (same as Node.js backend)
        String connectionString = "mongodb+srv://abidhasan7116:YpLEaZIAY0AmR4j0@cluster0.lx8cqwh.mongodb.net/AGROLINK?retryWrites=true&w=majority&appName=Cluster0";
        MongoClient mongoClient = MongoClients.create(connectionString);
        database = mongoClient.getDatabase("AGROLINK");
        
        // Create HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(5000), 0);
        
        // Add CORS handler
        server.createContext("/", new CorsHandler());
        
        // Auth routes
        server.createContext("/api/auth/register", new RegisterHandler());
        server.createContext("/api/auth/login", new LoginHandler());
        server.createContext("/api/auth/refresh", new RefreshTokenHandler());
        server.createContext("/api/auth/edit", new EditProfileHandler());
        server.createContext("/api/auth/delete", new DeleteProfileHandler());
        server.createContext("/api/auth/farmers/search", new SearchFarmersHandler());
        server.createContext("/api/auth/farmers", new GetFarmersHandler());
        
        // Animal routes
        server.createContext("/api/animals", new AnimalHandler());
        
        // Task routes
        server.createContext("/api/tasks", new TaskHandler());
        
        // Vaccine routes
        server.createContext("/api/vaccines", new VaccineHandler());
        
        // Appointment routes
        server.createContext("/api/appointments", new AppointmentHandler());
        
        // Vet routes
        server.createContext("/api/vets", new VetHandler());
        
        // Message routes
        server.createContext("/api/messages", new MessageHandler());
        server.createContext("/api/messages/fix-corrupt-data", new MessageDataFixHandler()); // Temporary fix endpoint
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("✅ Simple Java Backend running on http://localhost:5000");
        System.out.println("✅ Connected to MongoDB Atlas");
        System.out.println("✅ Available endpoints:");
        System.out.println("   POST /api/auth/register");
        System.out.println("   POST /api/auth/login");
        System.out.println("   POST /api/auth/refresh");
        System.out.println("   PUT  /api/auth/edit/{id}");
        System.out.println("   DELETE /api/auth/delete/{id}");
        System.out.println("   GET  /api/auth/farmers/search");
        System.out.println("   GET  /api/auth/farmers");
        System.out.println("   GET  /api/animals");
        System.out.println("   POST /api/animals");
        System.out.println("   GET  /api/animals/{id}");
        System.out.println("   PUT  /api/animals/{id}");
        System.out.println("   DELETE /api/animals/{id}");
        System.out.println("   GET  /api/tasks");
        System.out.println("   POST /api/tasks");
        System.out.println("   GET  /api/tasks/{id}");
        System.out.println("   PUT  /api/tasks/{id}");
        System.out.println("   PATCH /api/tasks/{id}/toggle");
        System.out.println("   DELETE /api/tasks/{id}");
        System.out.println("   GET  /api/tasks/stats/overview");
        System.out.println("   GET  /api/vaccines");
        System.out.println("   POST /api/vaccines");
        System.out.println("   GET  /api/vaccines/animal/{animalId}");
        System.out.println("   GET  /api/vaccines/{id}");
        System.out.println("   PUT  /api/vaccines/{id}");
        System.out.println("   DELETE /api/vaccines/{id}");
        System.out.println("   POST /api/appointments");
        System.out.println("   GET  /api/appointments/farmer");
        System.out.println("   GET  /api/appointments/vet");
        System.out.println("   GET  /api/appointments/vet/{vetId}");
        System.out.println("   GET  /api/appointments/{id}");
        System.out.println("   PUT  /api/appointments/{id}");
        System.out.println("   DELETE /api/appointments/{id}");
        System.out.println("   DELETE /api/appointments/remove/{id}");
        System.out.println("   GET  /api/appointments/availability/{vetId}/{date}");
        System.out.println("   POST /api/vets/register");
        System.out.println("   POST /api/vets/login");
        System.out.println("   GET  /api/vets/profile");
        System.out.println("   PUT  /api/vets/profile");
        System.out.println("   GET  /api/vets/search");
        System.out.println("   PUT  /api/vets/edit/{id}");
        System.out.println("   DELETE /api/vets/delete/{id}");
        System.out.println("   POST /api/messages");
        System.out.println("   GET  /api/messages/conversation/{receiverId}/{receiverType}");
        System.out.println("   GET  /api/messages/conversations");
        System.out.println("   GET  /api/messages/vet");
        System.out.println("   GET  /api/messages/farmer");
        System.out.println("   PUT  /api/messages/{messageId}/read");
        System.out.println("   DELETE /api/messages/{messageId}");
        System.out.println("   GET  /api/messages/unread-count");
        System.out.println("   GET  /api/messages/search");
    }
    
    static class CorsHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
            
            // Default 404 response
            String response = "{\"message\":\"Endpoint not found\"}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(404, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
    
    static class RegisterHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
            
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                // Read request body
                InputStream inputStream = exchange.getRequestBody();
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                
                // Parse JSON (simple parsing)
                Map<String, String> request = gson.fromJson(body, stringMapType);
                String email = request.get("email");
                String name = request.get("name");
                String password = request.get("password");
                
                // Validation (exactly like Node.js)
                if (name == null || email == null || password == null) {
                    sendError(exchange, 400, "All fields are required");
                    return;
                }
                
                if (password.length() < 6) {
                    sendError(exchange, 400, "Password should be at least 6 characters long");
                    return;
                }
                
                if (name.length() < 3) {
                    sendError(exchange, 400, "name should be at least 3 characters long");
                    return;
                }
                
                // Check if email exists
                MongoCollection<Document> farmers = database.getCollection("farmers");
                Document existingEmail = farmers.find(new Document("email", email)).first();
                if (existingEmail != null) {
                    sendError(exchange, 400, "Email already exists");
                    return;
                }
                
                // Check if name exists
                Document existingName = farmers.find(new Document("name", name)).first();
                if (existingName != null) {
                    sendError(exchange, 400, "name already exists");
                    return;
                }
                
                // Hash password (simple SHA-256 with salt)
                String hashedPassword = hashPassword(password);
                
                // Generate profile image
                String profileImage = "https://ui-avatars.com/api/?name=" + 
                    java.net.URLEncoder.encode(name, StandardCharsets.UTF_8) + 
                    "&background=random&color=fff&size=256";
                
                // Create farmer document
                Document farmer = new Document()
                    .append("email", email)
                    .append("name", name)
                    .append("password", hashedPassword)
                    .append("profileImage", profileImage)
                    .append("location", "")
                    .append("phoneNo", "")
                    .append("creationDate", new Date());
                
                // Save to database
                farmers.insertOne(farmer);
                
                // Generate JWT token
                String farmerId = farmer.getObjectId("_id").toString();
                String token = JwtUtil.generateToken(farmerId, email, name);
                
                // Response (exactly like Node.js)
                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                
                Map<String, Object> farmerData = new HashMap<>();
                farmerData.put("_id", farmerId);
                farmerData.put("name", farmer.getString("name"));
                farmerData.put("email", farmer.getString("email"));
                farmerData.put("profileImage", farmer.getString("profileImage"));
                
                response.put("farmer", farmerData);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(201, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Farmer registered: " + name + " (" + email + ")");
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
    }
    
    static class LoginHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
            
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                // Read request body
                InputStream inputStream = exchange.getRequestBody();
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                
                // Parse JSON
                Map<String, String> request = gson.fromJson(body, stringMapType);
                String email = request.get("email");
                String password = request.get("password");
                
                // Validation
                if (email == null || password == null) {
                    sendError(exchange, 400, "All fields are required");
                    return;
                }
                
                // Find farmer
                MongoCollection<Document> farmers = database.getCollection("farmers");
                Document farmer = farmers.find(new Document("email", email)).first();
                if (farmer == null) {
                    sendError(exchange, 400, "Invalid credentials");
                    return;
                }
                
                // Check password
                if (!verifyPassword(password, farmer.getString("password"))) {
                    sendError(exchange, 400, "Invalid credentials");
                    return;
                }
                
                // Generate JWT token
                String farmerId = farmer.getObjectId("_id").toString();
                String farmerName = farmer.getString("name");
                String farmerEmail = farmer.getString("email");
                String token = JwtUtil.generateToken(farmerId, farmerEmail, farmerName);
                
                // Response (exactly like Node.js)
                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                
                Map<String, Object> farmerData = new HashMap<>();
                farmerData.put("id", farmerId);
                farmerData.put("name", farmerName);
                farmerData.put("email", farmerEmail);
                farmerData.put("profileImage", farmer.getString("profileImage"));
                farmerData.put("phoneNo", farmer.getString("phoneNo"));
                farmerData.put("location", farmer.getString("location"));

                response.put("farmer", farmerData);
                
                System.out.println("✅ Login response farmer data: " + gson.toJson(farmerData));                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Farmer logged in: " + email);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
    }
    
    /**
     * JWT Refresh Handler - /api/auth/refresh
     * Refreshes JWT token if close to expiry
     */
    static class RefreshTokenHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
            
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                // Authenticate current token
                String farmerId = authenticateRequest(exchange);
                if (farmerId == null) {
                    sendError(exchange, 401, "Access denied. Invalid or expired token.");
                    return;
                }
                
                // Get current token
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                String currentToken = authHeader.substring(7);
                
                // Try to refresh token
                String newToken = JwtUtil.refreshTokenIfNeeded(currentToken);
                if (newToken == null) {
                    sendError(exchange, 401, "Token has expired and cannot be refreshed");
                    return;
                }
                
                // Get farmer details for response
                String email = JwtUtil.getEmailFromToken(newToken);
                String name = JwtUtil.getNameFromToken(newToken);
                long remainingMinutes = JwtUtil.getRemainingMinutes(newToken);
                
                // Response
                Map<String, Object> response = new HashMap<>();
                response.put("token", newToken);
                response.put("refreshed", !newToken.equals(currentToken));
                response.put("expiresInMinutes", remainingMinutes);
                
                Map<String, Object> farmerData = new HashMap<>();
                farmerData.put("id", farmerId);
                farmerData.put("name", name);
                farmerData.put("email", email);
                
                response.put("farmer", farmerData);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Token refresh successful for farmer: " + farmerId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
    }
    
    static class EditProfileHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "PUT, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
            
            if (!"PUT".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                // Extract farmer ID from URL path
                String path = exchange.getRequestURI().getPath();
                System.out.println("📝 Edit request path: " + path);
                
                // More flexible pattern matching
                if (!path.startsWith("/api/auth/edit/")) {
                    sendError(exchange, 404, "Invalid edit URL format");
                    return;
                }
                String farmerIdFromUrl = path.substring("/api/auth/edit/".length());
                System.out.println("📝 Farmer ID from URL: " + farmerIdFromUrl);
                
                // Check if it's a valid ObjectId format (24 hex characters)
                if (farmerIdFromUrl.length() != 24 || !farmerIdFromUrl.matches("[a-f0-9]{24}")) {
                    sendError(exchange, 400, "Invalid farmer ID format. Expected 24-character MongoDB ObjectId, got: " + farmerIdFromUrl);
                    return;
                }
                
                // Get farmer from JWT token
                String farmerId = authenticateRequest(exchange);
                if (farmerId == null) {
                    sendError(exchange, 401, "Access denied. Invalid or expired token.");
                    return;
                }
                
                // Check if farmer can only edit their own profile
                if (!farmerId.equals(farmerIdFromUrl)) {
                    sendError(exchange, 403, "You can only edit your own profile");
                    return;
                }
                
                // Read request body
                InputStream inputStream = exchange.getRequestBody();
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                
                // Parse JSON
                Map<String, Object> updates = gson.fromJson(body, objectMapType);
                
                // Build update document
                Document updateDoc = new Document();
                if (updates.get("name") != null) updateDoc.append("name", updates.get("name"));
                if (updates.get("email") != null) updateDoc.append("email", updates.get("email"));
                if (updates.get("phoneNo") != null) updateDoc.append("phoneNo", updates.get("phoneNo"));
                if (updates.get("location") != null) updateDoc.append("location", updates.get("location"));
                if (updates.get("profileImage") != null) updateDoc.append("profileImage", updates.get("profileImage"));
                
                MongoCollection<Document> farmers = database.getCollection("farmers");
                Document result = farmers.findOneAndUpdate(
                    eq("_id", new ObjectId(farmerId)),
                    new Document("$set", updateDoc),
                    new com.mongodb.client.model.FindOneAndUpdateOptions().returnDocument(com.mongodb.client.model.ReturnDocument.AFTER)
                );
                
                if (result == null) {
                    sendError(exchange, 404, "Farmer not found");
                    return;
                }
                
                // Response
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Farmer updated successfully");
                
                Map<String, Object> farmerData = new HashMap<>();
                farmerData.put("id", result.getObjectId("_id").toString());
                farmerData.put("name", result.getString("name"));
                farmerData.put("email", result.getString("email"));
                farmerData.put("profileImage", result.getString("profileImage"));
                farmerData.put("phoneNo", result.getString("phoneNo"));
                farmerData.put("location", result.getString("location"));
                
                response.put("farmer", farmerData);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Farmer profile updated: " + result.getString("name"));
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
    }
    
    static class DeleteProfileHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
            
            if (!"DELETE".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                // Extract farmer ID from URL path
                String path = exchange.getRequestURI().getPath();
                System.out.println("🗑️ Delete request path: " + path);
                
                // More flexible pattern matching
                if (!path.startsWith("/api/auth/delete/")) {
                    sendError(exchange, 404, "Invalid delete URL format");
                    return;
                }
                String farmerIdFromUrl = path.substring("/api/auth/delete/".length());
                System.out.println("🗑️ Farmer ID from URL: " + farmerIdFromUrl);
                
                // Check if it's a valid ObjectId format (24 hex characters)
                if (farmerIdFromUrl.length() != 24 || !farmerIdFromUrl.matches("[a-f0-9]{24}")) {
                    sendError(exchange, 400, "Invalid farmer ID format. Expected 24-character MongoDB ObjectId, got: " + farmerIdFromUrl);
                    return;
                }
                
                // Get farmer from token
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendError(exchange, 401, "Access denied. No token provided.");
                    return;
                }
                
                String token = authHeader.substring(7);
                String farmerId = extractFarmerIdFromToken(token);
                if (farmerId == null) {
                    sendError(exchange, 401, "Invalid token");
                    return;
                }
                
                // Check if farmer can only delete their own profile
                if (!farmerId.equals(farmerIdFromUrl)) {
                    sendError(exchange, 403, "You can only delete your own profile");
                    return;
                }
                
                MongoCollection<Document> farmers = database.getCollection("farmers");
                Document result = farmers.findOneAndDelete(eq("_id", new ObjectId(farmerId)));
                
                if (result == null) {
                    sendError(exchange, 404, "Farmer not found");
                    return;
                }
                
                // Also delete all related data (animals, vaccines, tasks, etc.)
                MongoCollection<Document> animals = database.getCollection("animals");
                MongoCollection<Document> vaccines = database.getCollection("vaccines");
                MongoCollection<Document> tasks = database.getCollection("tasks");
                
                animals.deleteMany(eq("farmer", new ObjectId(farmerId)));
                vaccines.deleteMany(eq("farmer", new ObjectId(farmerId)));
                tasks.deleteMany(eq("farmer", new ObjectId(farmerId)));
                
                // Response
                Map<String, String> response = new HashMap<>();
                response.put("message", "Farmer deleted successfully");
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Farmer profile deleted: " + result.getString("name"));
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
    }
    
    static class SearchFarmersHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
            
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                // Get search query parameter
                String query = exchange.getRequestURI().getQuery();
                String searchTerm = "";
                if (query != null && query.contains("search=")) {
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (param.startsWith("search=")) {
                            searchTerm = java.net.URLDecoder.decode(param.substring(7), StandardCharsets.UTF_8);
                            break;
                        }
                    }
                }
                
                MongoCollection<Document> farmers = database.getCollection("farmers");
                List<Document> farmerList;
                
                if (searchTerm.isEmpty()) {
                    farmerList = farmers.find()
                        .projection(new Document("name", 1).append("location", 1).append("profileImage", 1).append("phoneNo", 1))
                        .limit(20)
                        .into(new ArrayList<>());
                } else {
                    // Search by name or location (case insensitive)
                    Document searchQuery = new Document("$or", 
                        java.util.Arrays.asList(
                            new Document("name", new Document("$regex", searchTerm).append("$options", "i")),
                            new Document("location", new Document("$regex", searchTerm).append("$options", "i"))
                        )
                    );
                    
                    farmerList = farmers.find(searchQuery)
                        .projection(new Document("name", 1).append("location", 1).append("profileImage", 1).append("phoneNo", 1))
                        .limit(20)
                        .into(new ArrayList<>());
                }
                
                // Convert to response format
                List<Map<String, Object>> response = new ArrayList<>();
                for (Document farmer : farmerList) {
                    Map<String, Object> farmerData = new HashMap<>();
                    farmerData.put("_id", farmer.getObjectId("_id").toString());
                    farmerData.put("name", farmer.getString("name"));
                    farmerData.put("location", farmer.getString("location"));
                    farmerData.put("profileImage", farmer.getString("profileImage"));
                    farmerData.put("phoneNo", farmer.getString("phoneNo"));
                    response.add(farmerData);
                }
                
                Map<String, Object> finalResponse = new HashMap<>();
                finalResponse.put("farmers", response);
                
                String jsonResponse = gson.toJson(finalResponse);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Farmers search completed: " + response.size() + " results");
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
    }
    
    static class GetFarmersHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
            
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                MongoCollection<Document> farmers = database.getCollection("farmers");
                List<Document> farmerList = farmers.find()
                    .projection(new Document("name", 1).append("location", 1).append("profileImage", 1).append("phoneNo", 1))
                    .limit(20)
                    .into(new ArrayList<>());
                
                // Convert to response format
                List<Map<String, Object>> response = new ArrayList<>();
                for (Document farmer : farmerList) {
                    Map<String, Object> farmerData = new HashMap<>();
                    farmerData.put("_id", farmer.getObjectId("_id").toString());
                    farmerData.put("name", farmer.getString("name"));
                    farmerData.put("location", farmer.getString("location"));
                    farmerData.put("profileImage", farmer.getString("profileImage"));
                    farmerData.put("phoneNo", farmer.getString("phoneNo"));
                    response.add(farmerData);
                }
                
                Map<String, Object> finalResponse = new HashMap<>();
                finalResponse.put("farmers", response);
                
                String jsonResponse = gson.toJson(finalResponse);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ All farmers retrieved: " + response.size() + " farmers");
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
    }
    
    static class AnimalHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
            
            try {
                // Get farmer from token
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendError(exchange, 401, "Access denied. No token provided.");
                    return;
                }
                
                String token = authHeader.substring(7);
                String farmerId = extractFarmerIdFromToken(token);
                if (farmerId == null) {
                    sendError(exchange, 401, "Invalid token");
                    return;
                }
                
                // Get farmer document
                MongoCollection<Document> farmers = database.getCollection("farmers");
                Document farmer = farmers.find(eq("_id", new ObjectId(farmerId))).first();
                if (farmer == null) {
                    sendError(exchange, 401, "Farmer not found");
                    return;
                }
                
                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();
                
                if ("GET".equals(method) && "/api/animals".equals(path)) {
                    handleGetAnimals(exchange, farmerId);
                } else if ("POST".equals(method) && "/api/animals".equals(path)) {
                    handleCreateAnimal(exchange, farmerId, farmer);
                } else if ("GET".equals(method) && path.matches("/api/animals/[a-f0-9]{24}")) {
                    String animalId = path.substring("/api/animals/".length());
                    handleGetAnimal(exchange, farmerId, animalId);
                } else if ("PUT".equals(method) && path.matches("/api/animals/[a-f0-9]{24}")) {
                    String animalId = path.substring("/api/animals/".length());
                    handleUpdateAnimal(exchange, farmerId, animalId);
                } else if ("DELETE".equals(method) && path.matches("/api/animals/[a-f0-9]{24}")) {
                    String animalId = path.substring("/api/animals/".length());
                    handleDeleteAnimal(exchange, farmerId, animalId);
                } else {
                    sendError(exchange, 404, "Endpoint not found");
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        private void handleGetAnimals(HttpExchange exchange, String farmerId) throws IOException {
            try {
                MongoCollection<Document> animals = database.getCollection("animals");
                List<Document> animalList = animals.find(eq("farmer", new ObjectId(farmerId)))
                    .sort(new Document("createdAt", -1))
                    .into(new ArrayList<>());
                
                // Convert to response format
                List<Map<String, Object>> response = new ArrayList<>();
                for (Document animal : animalList) {
                    Map<String, Object> animalData = new HashMap<>();
                    animalData.put("_id", animal.getObjectId("_id").toString());
                    animalData.put("name", animal.getString("name"));
                    animalData.put("type", animal.getString("type"));
                    animalData.put("breed", animal.getString("breed"));
                    animalData.put("age", animal.getInteger("age"));
                    animalData.put("gender", animal.getString("gender"));
                    animalData.put("details", animal.getString("details"));
                    animalData.put("photo_url", animal.getString("photo_url"));
                    animalData.put("createdAt", animal.getDate("createdAt"));
                    response.add(animalData);
                }
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Failed to fetch animals");
            }
        }
        
        private void handleCreateAnimal(HttpExchange exchange, String farmerId, Document farmer) throws IOException {
            try {
                // Read request body
                InputStream inputStream = exchange.getRequestBody();
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                
                // Parse JSON
                Map<String, Object> request = gson.fromJson(body, objectMapType);
                String name = (String) request.get("name");
                String type = (String) request.get("type");
                String breed = (String) request.get("breed");
                Object ageObj = request.get("age");
                String gender = (String) request.get("gender");
                String details = (String) request.get("details");
                String image = (String) request.get("image");
                
                // Validation
                if (name == null || type == null || breed == null || ageObj == null || gender == null) {
                    sendError(exchange, 400, "Please provide name, type, breed, age and gender");
                    return;
                }
                
                int age;
                try {
                    if (ageObj instanceof Double) {
                        age = ((Double) ageObj).intValue();
                    } else if (ageObj instanceof String) {
                        age = Integer.parseInt((String) ageObj);
                    } else {
                        age = (Integer) ageObj;
                    }
                } catch (Exception e) {
                    sendError(exchange, 400, "Invalid age format");
                    return;
                }
                
                // Create animal document
                Document animal = new Document()
                    .append("name", name)
                    .append("type", type)
                    .append("breed", breed)
                    .append("age", age)
                    .append("gender", gender)
                    .append("details", details != null ? details : "")
                    .append("photo_url", image != null ? image : "")
                    .append("farmer", new ObjectId(farmerId))
                    .append("createdAt", new Date());
                
                // Save to database
                MongoCollection<Document> animals = database.getCollection("animals");
                animals.insertOne(animal);
                
                // Response
                Map<String, Object> response = new HashMap<>();
                response.put("_id", animal.getObjectId("_id").toString());
                response.put("name", animal.getString("name"));
                response.put("type", animal.getString("type"));
                response.put("breed", animal.getString("breed"));
                response.put("age", animal.getInteger("age"));
                response.put("gender", animal.getString("gender"));
                response.put("photo_url", animal.getString("photo_url"));
                
                Map<String, Object> farmerData = new HashMap<>();
                farmerData.put("_id", farmerId);
                farmerData.put("name", farmer.getString("name"));
                response.put("farmer", farmerData);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(201, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Animal created: " + name + " for farmer " + farmer.getString("name"));
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Failed to create animal");
            }
        }
        
        private void handleGetAnimal(HttpExchange exchange, String farmerId, String animalId) throws IOException {
            try {
                MongoCollection<Document> animals = database.getCollection("animals");
                Document animal = animals.find(and(
                    eq("_id", new ObjectId(animalId)),
                    eq("farmer", new ObjectId(farmerId))
                )).first();
                
                if (animal == null) {
                    sendError(exchange, 404, "Animal not found in your farm");
                    return;
                }
                
                // Response
                Map<String, Object> response = new HashMap<>();
                response.put("_id", animal.getObjectId("_id").toString());
                response.put("name", animal.getString("name"));
                response.put("type", animal.getString("type"));
                response.put("breed", animal.getString("breed"));
                response.put("age", animal.getInteger("age"));
                response.put("gender", animal.getString("gender"));
                response.put("details", animal.getString("details"));
                response.put("photo_url", animal.getString("photo_url"));
                response.put("createdAt", animal.getDate("createdAt"));
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Failed to fetch animal");
            }
        }
        
        private void handleUpdateAnimal(HttpExchange exchange, String farmerId, String animalId) throws IOException {
            try {
                // Read request body
                InputStream inputStream = exchange.getRequestBody();
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                
                // Parse JSON
                Map<String, Object> updates = gson.fromJson(body, objectMapType);
                
                // Build update document
                Document updateDoc = new Document();
                if (updates.get("name") != null) updateDoc.append("name", updates.get("name"));
                if (updates.get("type") != null) updateDoc.append("type", updates.get("type"));
                if (updates.get("breed") != null) updateDoc.append("breed", updates.get("breed"));
                if (updates.get("age") != null) {
                    Object ageObj = updates.get("age");
                    int age = ageObj instanceof Double ? ((Double) ageObj).intValue() : (Integer) ageObj;
                    updateDoc.append("age", age);
                }
                if (updates.get("gender") != null) updateDoc.append("gender", updates.get("gender"));
                if (updates.get("details") != null) updateDoc.append("details", updates.get("details"));
                if (updates.get("photo_url") != null) updateDoc.append("photo_url", updates.get("photo_url"));
                
                MongoCollection<Document> animals = database.getCollection("animals");
                Document result = animals.findOneAndUpdate(
                    and(eq("_id", new ObjectId(animalId)), eq("farmer", new ObjectId(farmerId))),
                    new Document("$set", updateDoc),
                    new com.mongodb.client.model.FindOneAndUpdateOptions().returnDocument(com.mongodb.client.model.ReturnDocument.AFTER)
                );
                
                if (result == null) {
                    sendError(exchange, 404, "Animal not found in your farm");
                    return;
                }
                
                // Response
                Map<String, Object> response = new HashMap<>();
                response.put("_id", result.getObjectId("_id").toString());
                response.put("name", result.getString("name"));
                response.put("type", result.getString("type"));
                response.put("breed", result.getString("breed"));
                response.put("age", result.getInteger("age"));
                response.put("gender", result.getString("gender"));
                response.put("details", result.getString("details"));
                response.put("photo_url", result.getString("photo_url"));
                response.put("createdAt", result.getDate("createdAt"));
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Animal updated: " + result.getString("name"));
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Failed to update animal");
            }
        }
        
        private void handleDeleteAnimal(HttpExchange exchange, String farmerId, String animalId) throws IOException {
            try {
                MongoCollection<Document> animals = database.getCollection("animals");
                
                // Find the animal first
                Document animal = animals.find(and(
                    eq("_id", new ObjectId(animalId)),
                    eq("farmer", new ObjectId(farmerId))
                )).first();
                
                if (animal == null) {
                    sendError(exchange, 404, "Animal not found in your farm");
                    return;
                }
                
                // Delete related records (vaccines and tasks)
                MongoCollection<Document> vaccines = database.getCollection("vaccines");
                MongoCollection<Document> tasks = database.getCollection("tasks");
                
                vaccines.deleteMany(and(
                    eq("animal", new ObjectId(animalId)),
                    eq("farmer", new ObjectId(farmerId))
                ));
                
                tasks.deleteMany(and(
                    eq("animal", new ObjectId(animalId)),
                    eq("farmer", new ObjectId(farmerId))
                ));
                
                // Delete the animal
                animals.deleteOne(eq("_id", new ObjectId(animalId)));
                
                Map<String, String> response = new HashMap<>();
                response.put("message", "Animal removed successfully");
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Animal deleted: " + animal.getString("name"));
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Failed to delete animal");
            }
        }
    }
    
    private static String extractFarmerIdFromToken(String token) {
        // Extract farmer ID from JWT token
        return JwtUtil.getFarmerIdFromToken(token);
    }
    
    /**
     * Helper method to parse date strings safely
     */
    private static Date parseDate(String dateString) {
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
    
    /**
     * Enhanced authentication with JWT validation
     * @param exchange HTTP exchange containing headers
     * @return Farmer ID if authenticated, null if not
     */
    private static String authenticateRequest(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        
        // Validate JWT token
        if (JwtUtil.validateToken(token) == null) {
            System.err.println("❌ Invalid JWT token provided");
            return null;
        }
        
        // Check if token is expired
        if (JwtUtil.isTokenExpired(token)) {
            System.err.println("❌ JWT token has expired");
            return null;
        }
        
        String farmerId = JwtUtil.getFarmerIdFromToken(token);
        System.out.println("✅ JWT Authentication successful for farmer: " + farmerId);
        
        return farmerId;
    }
    
    private static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, String> error = new HashMap<>();
        error.put("message", message);
        String jsonResponse = gson.toJson(error);
        
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, jsonResponse.length());
        OutputStream os = exchange.getResponseBody();
        os.write(jsonResponse.getBytes());
        os.close();
    }
    
    private static String hashPassword(String password) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
            
            return Base64.getEncoder().encodeToString(salt) + ":" + 
                   Base64.getEncoder().encodeToString(hashedPassword);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    private static boolean verifyPassword(String password, String hashedPassword) {
        try {
            String[] parts = hashedPassword.split(":");
            if (parts.length != 2) return false;
            
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] hash = Base64.getDecoder().decode(parts[1]);
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] testHash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            
            return MessageDigest.isEqual(hash, testHash);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Task Handler - handles all task-related operations
     * Endpoints: GET, POST, PUT, PATCH, DELETE /api/tasks
     */
    static class TaskHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
            
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            
            try {
                // Route to appropriate handler based on method and path
                if ("POST".equals(method) && "/api/tasks".equals(path)) {
                    handleCreateTask(exchange);
                } else if ("GET".equals(method) && "/api/tasks".equals(path)) {
                    handleGetTasks(exchange);
                } else if ("GET".equals(method) && path.matches("/api/tasks/stats/overview")) {
                    handleGetTaskStats(exchange);
                } else if ("GET".equals(method) && path.matches("/api/tasks/[a-f0-9]{24}")) {
                    handleGetSingleTask(exchange);
                } else if ("PUT".equals(method) && path.matches("/api/tasks/[a-f0-9]{24}")) {
                    handleUpdateTask(exchange);
                } else if ("PATCH".equals(method) && path.matches("/api/tasks/[a-f0-9]{24}/toggle")) {
                    handleToggleTask(exchange);
                } else if ("DELETE".equals(method) && path.matches("/api/tasks/[a-f0-9]{24}")) {
                    handleDeleteTask(exchange);
                } else {
                    sendError(exchange, 404, "Task endpoint not found");
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // POST /api/tasks - Create a new task
        private void handleCreateTask(HttpExchange exchange) throws IOException {
            // Authenticate
            String farmerId = authenticateRequest(exchange);
            if (farmerId == null) {
                sendError(exchange, 401, "Access denied. Invalid or expired token.");
                return;
            }
            
            try {
                // Read request body
                InputStream inputStream = exchange.getRequestBody();
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                
                // Parse JSON
                Map<String, Object> request = gson.fromJson(body, objectMapType);
                String title = (String) request.get("title");
                String description = (String) request.get("description");
                String dueDate = (String) request.get("dueDate");
                String dueTime = (String) request.get("dueTime");
                Object estimatedCostObj = request.get("estimatedCost");
                String priority = (String) request.get("priority");
                String category = (String) request.get("category");
                String animalId = (String) request.get("animal");
                String notes = (String) request.get("notes");
                
                // Validate required fields
                if (title == null || dueDate == null || dueTime == null) {
                    sendError(exchange, 400, "Please provide title, due date, and due time");
                    return;
                }
                
                // Validate time format (HH:MM)
                if (!dueTime.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                    sendError(exchange, 400, "Time must be in HH:MM format");
                    return;
                }
                
                // Validate animal exists and belongs to farmer if provided
                if (animalId != null && !animalId.isEmpty()) {
                    MongoCollection<Document> animals = database.getCollection("animals");
                    Document animal = animals.find(and(
                        eq("_id", new ObjectId(animalId)),
                        eq("farmer", new ObjectId(farmerId))
                    )).first();
                    
                    if (animal == null) {
                        sendError(exchange, 400, "Animal not found in your farm");
                        return;
                    }
                }
                
                // Parse estimated cost
                double estimatedCost = 0.0;
                if (estimatedCostObj != null) {
                    if (estimatedCostObj instanceof Double) {
                        estimatedCost = (Double) estimatedCostObj;
                    } else if (estimatedCostObj instanceof Integer) {
                        estimatedCost = ((Integer) estimatedCostObj).doubleValue();
                    }
                }
                
                // Create task document
                Document task = new Document()
                    .append("title", title)
                    .append("description", description != null ? description : "")
                    .append("dueDate", parseDate(dueDate))
                    .append("dueTime", dueTime)
                    .append("estimatedCost", estimatedCost)
                    .append("priority", priority != null ? priority : "medium")
                    .append("status", "pending")
                    .append("category", category != null ? category : "other")
                    .append("farmer", new ObjectId(farmerId))
                    .append("isCompleted", false)
                    .append("notes", notes != null ? notes : "")
                    .append("createdAt", new Date())
                    .append("updatedAt", new Date());
                
                if (animalId != null && !animalId.isEmpty()) {
                    task.append("animal", new ObjectId(animalId));
                }
                
                // Save to database
                MongoCollection<Document> tasks = database.getCollection("tasks");
                tasks.insertOne(task);
                
                // Prepare response with animal details if present
                Map<String, Object> response = createTaskResponse(task);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(201, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Task created: " + title);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Failed to create task");
            }
        }
        
        // GET /api/tasks - Get all tasks with optional filters
        private void handleGetTasks(HttpExchange exchange) throws IOException {
            // Authenticate
            String farmerId = authenticateRequest(exchange);
            if (farmerId == null) {
                sendError(exchange, 401, "Access denied. Invalid or expired token.");
                return;
            }
            
            try {
                // Parse query parameters
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQueryParams(query);
                
                // Build MongoDB query
                Document mongoQuery = new Document("farmer", new ObjectId(farmerId));
                
                if (params.get("status") != null) {
                    mongoQuery.append("status", params.get("status"));
                }
                if (params.get("priority") != null) {
                    mongoQuery.append("priority", params.get("priority"));
                }
                if (params.get("category") != null) {
                    mongoQuery.append("category", params.get("category"));
                }
                if (params.get("animal") != null) {
                    mongoQuery.append("animal", new ObjectId(params.get("animal")));
                }
                
                // Date range filter
                if (params.get("dateFrom") != null || params.get("dateTo") != null) {
                    Document dateQuery = new Document();
                    if (params.get("dateFrom") != null) {
                        dateQuery.append("$gte", parseDate(params.get("dateFrom")));
                    }
                    if (params.get("dateTo") != null) {
                        dateQuery.append("$lte", parseDate(params.get("dateTo")));
                    }
                    mongoQuery.append("dueDate", dateQuery);
                }
                
                // Get tasks
                MongoCollection<Document> tasks = database.getCollection("tasks");
                List<Document> taskList = tasks.find(mongoQuery)
                    .sort(new Document("dueDate", 1).append("dueTime", 1))
                    .into(new ArrayList<>());
                
                // Convert to response format with animal details
                List<Map<String, Object>> response = new ArrayList<>();
                for (Document task : taskList) {
                    response.add(createTaskResponse(task));
                }
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Retrieved " + taskList.size() + " tasks for farmer: " + farmerId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Failed to fetch tasks");
            }
        }
        
        // GET /api/tasks/{id} - Get single task
        private void handleGetSingleTask(HttpExchange exchange) throws IOException {
            // Authenticate
            String farmerId = authenticateRequest(exchange);
            if (farmerId == null) {
                sendError(exchange, 401, "Access denied. Invalid or expired token.");
                return;
            }
            
            try {
                // Extract task ID from URL
                String path = exchange.getRequestURI().getPath();
                String taskId = path.substring(path.lastIndexOf('/') + 1);
                
                // Validate ObjectId format
                if (taskId.length() != 24 || !taskId.matches("[a-f0-9]{24}")) {
                    sendError(exchange, 400, "Invalid task ID format");
                    return;
                }
                
                // Get task
                MongoCollection<Document> tasks = database.getCollection("tasks");
                Document task = tasks.find(and(
                    eq("_id", new ObjectId(taskId)),
                    eq("farmer", new ObjectId(farmerId))
                )).first();
                
                if (task == null) {
                    sendError(exchange, 404, "Task not found");
                    return;
                }
                
                // Response with animal details
                Map<String, Object> response = createTaskResponse(task);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Retrieved task: " + taskId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Failed to fetch task");
            }
        }
        
        // PUT /api/tasks/{id} - Update task
        private void handleUpdateTask(HttpExchange exchange) throws IOException {
            // Authenticate
            String farmerId = authenticateRequest(exchange);
            if (farmerId == null) {
                sendError(exchange, 401, "Access denied. Invalid or expired token.");
                return;
            }
            
            try {
                // Extract task ID from URL
                String path = exchange.getRequestURI().getPath();
                String taskId = path.substring(path.lastIndexOf('/') + 1);
                
                // Read request body
                InputStream inputStream = exchange.getRequestBody();
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                
                // Parse JSON
                Map<String, Object> updates = gson.fromJson(body, objectMapType);
                
                // Validate animal exists and belongs to farmer if being updated
                String animalId = (String) updates.get("animal");
                if (animalId != null && !animalId.isEmpty()) {
                    MongoCollection<Document> animals = database.getCollection("animals");
                    Document animal = animals.find(and(
                        eq("_id", new ObjectId(animalId)),
                        eq("farmer", new ObjectId(farmerId))
                    )).first();
                    
                    if (animal == null) {
                        sendError(exchange, 400, "Animal not found in your farm");
                        return;
                    }
                }
                
                // Build update document
                Document updateDoc = new Document("updatedAt", new Date());
                if (updates.get("title") != null) updateDoc.append("title", updates.get("title"));
                if (updates.get("description") != null) updateDoc.append("description", updates.get("description"));
                if (updates.get("dueDate") != null) updateDoc.append("dueDate", parseDate((String) updates.get("dueDate")));
                if (updates.get("dueTime") != null) updateDoc.append("dueTime", updates.get("dueTime"));
                if (updates.get("estimatedCost") != null) {
                    Object costObj = updates.get("estimatedCost");
                    double cost = costObj instanceof Double ? (Double) costObj : ((Integer) costObj).doubleValue();
                    updateDoc.append("estimatedCost", cost);
                }
                if (updates.get("priority") != null) updateDoc.append("priority", updates.get("priority"));
                if (updates.get("status") != null) updateDoc.append("status", updates.get("status"));
                if (updates.get("category") != null) updateDoc.append("category", updates.get("category"));
                if (updates.get("notes") != null) updateDoc.append("notes", updates.get("notes"));
                if (updates.get("isCompleted") != null) {
                    boolean isCompleted = (Boolean) updates.get("isCompleted");
                    updateDoc.append("isCompleted", isCompleted);
                    if (isCompleted) {
                        updateDoc.append("completedAt", new Date());
                        updateDoc.append("status", "completed");
                    } else {
                        updateDoc.append("completedAt", null);
                    }
                }
                if (animalId != null) {
                    if (animalId.isEmpty()) {
                        updateDoc.append("animal", null);
                    } else {
                        updateDoc.append("animal", new ObjectId(animalId));
                    }
                }
                
                MongoCollection<Document> tasks = database.getCollection("tasks");
                Document result = tasks.findOneAndUpdate(
                    and(eq("_id", new ObjectId(taskId)), eq("farmer", new ObjectId(farmerId))),
                    new Document("$set", updateDoc),
                    new com.mongodb.client.model.FindOneAndUpdateOptions().returnDocument(com.mongodb.client.model.ReturnDocument.AFTER)
                );
                
                if (result == null) {
                    sendError(exchange, 404, "Task not found");
                    return;
                }
                
                // Response with animal details
                Map<String, Object> response = createTaskResponse(result);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Task updated: " + taskId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Failed to update task");
            }
        }
        
        // PATCH /api/tasks/{id}/toggle - Toggle task completion
        private void handleToggleTask(HttpExchange exchange) throws IOException {
            // Authenticate
            String farmerId = authenticateRequest(exchange);
            if (farmerId == null) {
                sendError(exchange, 401, "Access denied. Invalid or expired token.");
                return;
            }
            
            try {
                // Extract task ID from URL
                String path = exchange.getRequestURI().getPath();
                String taskId = path.split("/")[3]; // /api/tasks/{id}/toggle
                
                // Get current task
                MongoCollection<Document> tasks = database.getCollection("tasks");
                Document task = tasks.find(and(
                    eq("_id", new ObjectId(taskId)),
                    eq("farmer", new ObjectId(farmerId))
                )).first();
                
                if (task == null) {
                    sendError(exchange, 404, "Task not found");
                    return;
                }
                
                // Toggle completion
                boolean currentStatus = task.getBoolean("isCompleted", false);
                boolean newStatus = !currentStatus;
                
                Document updateDoc = new Document("isCompleted", newStatus)
                    .append("updatedAt", new Date());
                
                if (newStatus) {
                    updateDoc.append("completedAt", new Date())
                             .append("status", "completed");
                } else {
                    updateDoc.append("completedAt", null);
                    String currentStatusStr = task.getString("status");
                    if ("completed".equals(currentStatusStr)) {
                        updateDoc.append("status", "pending");
                    }
                }
                
                Document result = tasks.findOneAndUpdate(
                    and(eq("_id", new ObjectId(taskId)), eq("farmer", new ObjectId(farmerId))),
                    new Document("$set", updateDoc),
                    new com.mongodb.client.model.FindOneAndUpdateOptions().returnDocument(com.mongodb.client.model.ReturnDocument.AFTER)
                );
                
                // Response with animal details
                Map<String, Object> response = createTaskResponse(result);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Task toggled: " + taskId + " -> " + newStatus);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Failed to toggle task completion");
            }
        }
        
        // DELETE /api/tasks/{id} - Delete task
        private void handleDeleteTask(HttpExchange exchange) throws IOException {
            // Authenticate
            String farmerId = authenticateRequest(exchange);
            if (farmerId == null) {
                sendError(exchange, 401, "Access denied. Invalid or expired token.");
                return;
            }
            
            try {
                // Extract task ID from URL
                String path = exchange.getRequestURI().getPath();
                String taskId = path.substring(path.lastIndexOf('/') + 1);
                
                // Find and delete task
                MongoCollection<Document> tasks = database.getCollection("tasks");
                Document task = tasks.find(and(
                    eq("_id", new ObjectId(taskId)),
                    eq("farmer", new ObjectId(farmerId))
                )).first();
                
                if (task == null) {
                    sendError(exchange, 404, "Task not found");
                    return;
                }
                
                tasks.deleteOne(and(
                    eq("_id", new ObjectId(taskId)),
                    eq("farmer", new ObjectId(farmerId))
                ));
                
                // Response
                Map<String, String> response = new HashMap<>();
                response.put("message", "Task deleted successfully");
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Task deleted: " + taskId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Failed to delete task");
            }
        }
        
        // GET /api/tasks/stats/overview - Get task statistics
        private void handleGetTaskStats(HttpExchange exchange) throws IOException {
            // Authenticate
            String farmerId = authenticateRequest(exchange);
            if (farmerId == null) {
                sendError(exchange, 401, "Access denied. Invalid or expired token.");
                return;
            }
            
            try {
                MongoCollection<Document> tasks = database.getCollection("tasks");
                
                // Get all tasks for the farmer
                List<Document> allTasks = tasks.find(eq("farmer", new ObjectId(farmerId)))
                    .into(new ArrayList<>());
                
                // Calculate statistics
                int totalTasks = allTasks.size();
                int completedTasks = 0;
                int pendingTasks = 0;
                double totalEstimatedCost = 0.0;
                int overdueTasks = 0;
                
                Date now = new Date();
                
                for (Document task : allTasks) {
                    boolean isCompleted = task.getBoolean("isCompleted", false);
                    String status = task.getString("status");
                    Date dueDate = task.getDate("dueDate");
                    Double cost = task.getDouble("estimatedCost");
                    
                    if (isCompleted) completedTasks++;
                    if ("pending".equals(status)) pendingTasks++;
                    if (cost != null) totalEstimatedCost += cost;
                    
                    // Check if overdue (due date passed and not completed)
                    if (dueDate != null && dueDate.before(now) && !isCompleted) {
                        overdueTasks++;
                    }
                }
                
                // Response
                Map<String, Object> stats = new HashMap<>();
                stats.put("totalTasks", totalTasks);
                stats.put("completedTasks", completedTasks);
                stats.put("pendingTasks", pendingTasks);
                stats.put("totalEstimatedCost", totalEstimatedCost);
                stats.put("overdueTasks", overdueTasks);
                
                String jsonResponse = gson.toJson(stats);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Task stats retrieved for farmer: " + farmerId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Failed to fetch task statistics");
            }
        }
        
        // Helper method to create task response with animal details
        private Map<String, Object> createTaskResponse(Document task) {
            Map<String, Object> response = new HashMap<>();
            response.put("_id", task.getObjectId("_id").toString());
            response.put("title", task.getString("title"));
            response.put("description", task.getString("description"));
            response.put("dueDate", task.getDate("dueDate"));
            response.put("dueTime", task.getString("dueTime"));
            response.put("estimatedCost", task.getDouble("estimatedCost"));
            response.put("priority", task.getString("priority"));
            response.put("status", task.getString("status"));
            response.put("category", task.getString("category"));
            response.put("isCompleted", task.getBoolean("isCompleted"));
            response.put("notes", task.getString("notes"));
            response.put("createdAt", task.getDate("createdAt"));
            response.put("completedAt", task.getDate("completedAt"));
            
            // Add animal details if present
            ObjectId animalId = task.getObjectId("animal");
            if (animalId != null) {
                MongoCollection<Document> animals = database.getCollection("animals");
                Document animal = animals.find(eq("_id", animalId)).first();
                if (animal != null) {
                    Map<String, Object> animalData = new HashMap<>();
                    animalData.put("_id", animal.getObjectId("_id").toString());
                    animalData.put("name", animal.getString("name"));
                    animalData.put("type", animal.getString("type"));
                    animalData.put("breed", animal.getString("breed"));
                    response.put("animal", animalData);
                }
            }
            
            return response;
        }
        
        // Helper method to parse query parameters
        private Map<String, String> parseQueryParams(String query) {
            Map<String, String> params = new HashMap<>();
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        params.put(keyValue[0], keyValue[1]);
                    }
                }
            }
            return params;
        }
    }
    
    /**
     * Vaccine Handler - handles all vaccine-related operations
     * Endpoints: GET, POST, PUT, DELETE /api/vaccines
     */
    static class VaccineHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
            
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            
            try {
                // Route to appropriate handler based on method and path
                if ("POST".equals(method) && "/api/vaccines".equals(path)) {
                    handleCreateVaccine(exchange);
                } else if ("GET".equals(method) && "/api/vaccines".equals(path)) {
                    handleGetVaccines(exchange);
                } else if ("GET".equals(method) && path.matches("/api/vaccines/animal/[a-f0-9]{24}")) {
                    handleGetAnimalVaccines(exchange);
                } else if ("GET".equals(method) && path.matches("/api/vaccines/[a-f0-9]{24}")) {
                    handleGetSingleVaccine(exchange);
                } else if ("PUT".equals(method) && path.matches("/api/vaccines/[a-f0-9]{24}")) {
                    handleUpdateVaccine(exchange);
                } else if ("DELETE".equals(method) && path.matches("/api/vaccines/[a-f0-9]{24}")) {
                    handleDeleteVaccine(exchange);
                } else {
                    sendError(exchange, 404, "Vaccine endpoint not found");
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // POST /api/vaccines - Create a new vaccine record
        private void handleCreateVaccine(HttpExchange exchange) throws IOException {
            // Authenticate
            String farmerId = authenticateRequest(exchange);
            if (farmerId == null) {
                sendError(exchange, 401, "Access denied. Invalid or expired token.");
                return;
            }
            
            try {
                // Read request body
                InputStream inputStream = exchange.getRequestBody();
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                
                // Parse JSON
                Map<String, Object> request = gson.fromJson(body, objectMapType);
                String vaccineName = (String) request.get("vaccine_name");
                String animalId = (String) request.get("animal_id");
                String vaccineDate = (String) request.get("vaccine_date");
                String notes = (String) request.get("notes");
                String nextDueDate = (String) request.get("next_due_date");
                
                // Validate required fields
                if (vaccineName == null || animalId == null || vaccineDate == null) {
                    sendError(exchange, 400, "Please provide vaccine name, animal ID, and vaccine date");
                    return;
                }
                
                // Verify animal exists and belongs to farmer
                MongoCollection<Document> animals = database.getCollection("animals");
                Document animal = animals.find(and(
                    eq("_id", new ObjectId(animalId)),
                    eq("farmer", new ObjectId(farmerId))
                )).first();
                
                if (animal == null) {
                    sendError(exchange, 404, "Animal not found in your farm");
                    return;
                }
                
                // Create vaccine document
                Document vaccine = new Document()
                    .append("vaccine_name", vaccineName)
                    .append("animal", new ObjectId(animalId))
                    .append("animal_name", animal.getString("name"))
                    .append("vaccine_date", parseDate(vaccineDate))
                    .append("notes", notes != null ? notes : "")
                    .append("farmer", new ObjectId(farmerId))
                    .append("createdAt", new Date())
                    .append("updatedAt", new Date());
                
                if (nextDueDate != null && !nextDueDate.isEmpty()) {
                    vaccine.append("next_due_date", parseDate(nextDueDate));
                }
                
                // Save to database
                MongoCollection<Document> vaccines = database.getCollection("vaccines");
                vaccines.insertOne(vaccine);
                
                // Prepare response with animal details
                Map<String, Object> response = createVaccineResponse(vaccine);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(201, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Vaccine record created: " + vaccineName + " for " + animal.getString("name"));
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Failed to create vaccine record");
            }
        }
        
        // GET /api/vaccines - Get all vaccine records for farmer
        private void handleGetVaccines(HttpExchange exchange) throws IOException {
            // Authenticate
            String farmerId = authenticateRequest(exchange);
            if (farmerId == null) {
                sendError(exchange, 401, "Access denied. Invalid or expired token.");
                return;
            }
            
            try {
                // Get vaccines
                MongoCollection<Document> vaccines = database.getCollection("vaccines");
                List<Document> vaccineList = vaccines.find(eq("farmer", new ObjectId(farmerId)))
                    .sort(new Document("vaccine_date", -1))
                    .into(new ArrayList<>());
                
                // Convert to response format with animal details
                List<Map<String, Object>> response = new ArrayList<>();
                for (Document vaccine : vaccineList) {
                    response.add(createVaccineResponse(vaccine));
                }
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Retrieved " + vaccineList.size() + " vaccine records for farmer: " + farmerId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Failed to fetch vaccines");
            }
        }
        
        // GET /api/vaccines/animal/{animalId} - Get vaccines for specific animal
        private void handleGetAnimalVaccines(HttpExchange exchange) throws IOException {
            // Authenticate
            String farmerId = authenticateRequest(exchange);
            if (farmerId == null) {
                sendError(exchange, 401, "Access denied. Invalid or expired token.");
                return;
            }
            
            try {
                // Extract animal ID from URL
                String path = exchange.getRequestURI().getPath();
                String animalId = path.substring(path.lastIndexOf('/') + 1);
                
                // Validate ObjectId format
                if (animalId.length() != 24 || !animalId.matches("[a-f0-9]{24}")) {
                    sendError(exchange, 400, "Invalid animal ID format");
                    return;
                }
                
                // Verify animal belongs to farmer
                MongoCollection<Document> animals = database.getCollection("animals");
                Document animal = animals.find(and(
                    eq("_id", new ObjectId(animalId)),
                    eq("farmer", new ObjectId(farmerId))
                )).first();
                
                if (animal == null) {
                    sendError(exchange, 404, "Animal not found in your farm");
                    return;
                }
                
                // Get vaccines for this animal
                MongoCollection<Document> vaccines = database.getCollection("vaccines");
                List<Document> vaccineList = vaccines.find(and(
                    eq("animal", new ObjectId(animalId)),
                    eq("farmer", new ObjectId(farmerId))
                ))
                .sort(new Document("vaccine_date", -1))
                .into(new ArrayList<>());
                
                // Convert to response format with animal details
                List<Map<String, Object>> response = new ArrayList<>();
                for (Document vaccine : vaccineList) {
                    response.add(createVaccineResponse(vaccine));
                }
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Retrieved " + vaccineList.size() + " vaccine records for animal: " + animalId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Failed to fetch animal vaccines");
            }
        }
        
        // GET /api/vaccines/{id} - Get single vaccine record
        private void handleGetSingleVaccine(HttpExchange exchange) throws IOException {
            // Authenticate
            String farmerId = authenticateRequest(exchange);
            if (farmerId == null) {
                sendError(exchange, 401, "Access denied. Invalid or expired token.");
                return;
            }
            
            try {
                // Extract vaccine ID from URL
                String path = exchange.getRequestURI().getPath();
                String vaccineId = path.substring(path.lastIndexOf('/') + 1);
                
                // Validate ObjectId format
                if (vaccineId.length() != 24 || !vaccineId.matches("[a-f0-9]{24}")) {
                    sendError(exchange, 400, "Invalid vaccine ID format");
                    return;
                }
                
                // Get vaccine
                MongoCollection<Document> vaccines = database.getCollection("vaccines");
                Document vaccine = vaccines.find(and(
                    eq("_id", new ObjectId(vaccineId)),
                    eq("farmer", new ObjectId(farmerId))
                )).first();
                
                if (vaccine == null) {
                    sendError(exchange, 404, "Vaccine record not found");
                    return;
                }
                
                // Response with animal details
                Map<String, Object> response = createVaccineResponse(vaccine);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Retrieved vaccine record: " + vaccineId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Failed to fetch vaccine record");
            }
        }
        
        // PUT /api/vaccines/{id} - Update vaccine record
        private void handleUpdateVaccine(HttpExchange exchange) throws IOException {
            // Authenticate
            String farmerId = authenticateRequest(exchange);
            if (farmerId == null) {
                sendError(exchange, 401, "Access denied. Invalid or expired token.");
                return;
            }
            
            try {
                // Extract vaccine ID from URL
                String path = exchange.getRequestURI().getPath();
                String vaccineId = path.substring(path.lastIndexOf('/') + 1);
                
                // Read request body
                InputStream inputStream = exchange.getRequestBody();
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                
                // Parse JSON
                Map<String, Object> updates = gson.fromJson(body, objectMapType);
                
                // Handle animal_id update and validation
                String newAnimalId = (String) updates.get("animal_id");
                if (newAnimalId != null && !newAnimalId.isEmpty()) {
                    MongoCollection<Document> animals = database.getCollection("animals");
                    Document animal = animals.find(and(
                        eq("_id", new ObjectId(newAnimalId)),
                        eq("farmer", new ObjectId(farmerId))
                    )).first();
                    
                    if (animal == null) {
                        sendError(exchange, 404, "Animal not found in your farm");
                        return;
                    }
                    
                    updates.put("animal", newAnimalId);
                    updates.put("animal_name", animal.getString("name"));
                    updates.remove("animal_id");
                }
                
                // Build update document
                Document updateDoc = new Document("updatedAt", new Date());
                if (updates.get("vaccine_name") != null) updateDoc.append("vaccine_name", updates.get("vaccine_name"));
                if (updates.get("vaccine_date") != null) updateDoc.append("vaccine_date", parseDate((String) updates.get("vaccine_date")));
                if (updates.get("notes") != null) updateDoc.append("notes", updates.get("notes"));
                if (updates.get("next_due_date") != null) {
                    String nextDueDateStr = (String) updates.get("next_due_date");
                    if (nextDueDateStr.isEmpty()) {
                        updateDoc.append("next_due_date", null);
                    } else {
                        updateDoc.append("next_due_date", parseDate(nextDueDateStr));
                    }
                }
                if (updates.get("animal") != null) updateDoc.append("animal", new ObjectId((String) updates.get("animal")));
                if (updates.get("animal_name") != null) updateDoc.append("animal_name", updates.get("animal_name"));
                
                MongoCollection<Document> vaccines = database.getCollection("vaccines");
                Document result = vaccines.findOneAndUpdate(
                    and(eq("_id", new ObjectId(vaccineId)), eq("farmer", new ObjectId(farmerId))),
                    new Document("$set", updateDoc),
                    new com.mongodb.client.model.FindOneAndUpdateOptions().returnDocument(com.mongodb.client.model.ReturnDocument.AFTER)
                );
                
                if (result == null) {
                    sendError(exchange, 404, "Vaccine record not found");
                    return;
                }
                
                // Response with animal details
                Map<String, Object> response = createVaccineResponse(result);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Vaccine record updated: " + vaccineId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Failed to update vaccine record");
            }
        }
        
        // DELETE /api/vaccines/{id} - Delete vaccine record
        private void handleDeleteVaccine(HttpExchange exchange) throws IOException {
            // Authenticate
            String farmerId = authenticateRequest(exchange);
            if (farmerId == null) {
                sendError(exchange, 401, "Access denied. Invalid or expired token.");
                return;
            }
            
            try {
                // Extract vaccine ID from URL
                String path = exchange.getRequestURI().getPath();
                String vaccineId = path.substring(path.lastIndexOf('/') + 1);
                
                // Find and delete vaccine
                MongoCollection<Document> vaccines = database.getCollection("vaccines");
                Document vaccine = vaccines.find(and(
                    eq("_id", new ObjectId(vaccineId)),
                    eq("farmer", new ObjectId(farmerId))
                )).first();
                
                if (vaccine == null) {
                    sendError(exchange, 404, "Vaccine record not found");
                    return;
                }
                
                vaccines.deleteOne(and(
                    eq("_id", new ObjectId(vaccineId)),
                    eq("farmer", new ObjectId(farmerId))
                ));
                
                // Response
                Map<String, String> response = new HashMap<>();
                response.put("message", "Vaccine record deleted successfully");
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Vaccine record deleted: " + vaccineId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Failed to delete vaccine record");
            }
        }
        
        // Helper method to create vaccine response with animal details
        private Map<String, Object> createVaccineResponse(Document vaccine) {
            Map<String, Object> response = new HashMap<>();
            response.put("_id", vaccine.getObjectId("_id").toString());
            response.put("vaccine_name", vaccine.getString("vaccine_name"));
            response.put("animal_name", vaccine.getString("animal_name"));
            response.put("vaccine_date", vaccine.getDate("vaccine_date"));
            response.put("notes", vaccine.getString("notes"));
            response.put("next_due_date", vaccine.getDate("next_due_date"));
            response.put("createdAt", vaccine.getDate("createdAt"));
            
            // Add animal details
            ObjectId animalId = vaccine.getObjectId("animal");
            if (animalId != null) {
                MongoCollection<Document> animals = database.getCollection("animals");
                Document animal = animals.find(eq("_id", animalId)).first();
                if (animal != null) {
                    Map<String, Object> animalData = new HashMap<>();
                    animalData.put("_id", animal.getObjectId("_id").toString());
                    animalData.put("name", animal.getString("name"));
                    animalData.put("type", animal.getString("type"));
                    animalData.put("breed", animal.getString("breed"));
                    response.put("animal", animalData);
                }
            }
            
            return response;
        }
    }
    
    /**
     * Vet Handler - handles all vet-related operations
     * Endpoints: GET, POST, PUT, DELETE /api/vets/*
     */
    static class VetHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
            
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            
            try {
                // Route to appropriate handler based on method and path
                if ("POST".equals(method) && "/api/vets/register".equals(path)) {
                    handleVetRegister(exchange);
                } else if ("POST".equals(method) && "/api/vets/login".equals(path)) {
                    handleVetLogin(exchange);
                } else if ("GET".equals(method) && "/api/vets/profile".equals(path)) {
                    handleGetVetProfile(exchange);
                } else if ("PUT".equals(method) && "/api/vets/profile".equals(path)) {
                    handleUpdateVetProfile(exchange);
                } else if ("GET".equals(method) && "/api/vets/search".equals(path)) {
                    handleSearchVets(exchange);
                } else if ("PUT".equals(method) && path.matches("/api/vets/edit/[a-f0-9]{24}")) {
                    handleEditVet(exchange);
                } else if ("DELETE".equals(method) && path.matches("/api/vets/delete/[a-f0-9]{24}")) {
                    handleDeleteVet(exchange);
                } else {
                    sendError(exchange, 404, "Vet endpoint not found");
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // POST /api/vets/register - Register a new vet
        private void handleVetRegister(HttpExchange exchange) throws IOException {
            try {
                // Read request body
                InputStream inputStream = exchange.getRequestBody();
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                
                // Parse JSON
                Map<String, Object> request = gson.fromJson(body, objectMapType);
                String name = (String) request.get("name");
                String email = (String) request.get("email");
                String password = (String) request.get("password");
                String specialty = (String) request.get("specialty");
                String licenseNumber = (String) request.get("licenseNumber");
                String phoneNo = (String) request.get("phoneNo");
                String location = (String) request.get("location");
                
                // Validation
                if (name == null || email == null || password == null || specialty == null || licenseNumber == null) {
                    sendError(exchange, 400, "All required fields must be provided");
                    return;
                }
                
                if (password.length() < 6) {
                    sendError(exchange, 400, "Password must be at least 6 characters");
                    return;
                }
                
                if (name.length() < 3) {
                    sendError(exchange, 400, "Name must be at least 3 characters");
                    return;
                }
                
                // Check if vet already exists
                MongoCollection<Document> vets = database.getCollection("vets");
                Document existingVet = vets.find(or(
                    eq("email", email.toLowerCase().trim()),
                    eq("licenseNumber", licenseNumber.trim().toUpperCase())
                )).first();
                
                if (existingVet != null) {
                    if (email.toLowerCase().trim().equals(existingVet.getString("email"))) {
                        sendError(exchange, 400, "Email already registered");
                        return;
                    }
                    if (licenseNumber.trim().toUpperCase().equals(existingVet.getString("licenseNumber"))) {
                        sendError(exchange, 400, "License number already registered");
                        return;
                    }
                }
                
                // Hash password
                String hashedPassword = hashPassword(password);
                
                // Create vet document
                Document vet = new Document()
                    .append("name", name.trim())
                    .append("email", email.toLowerCase().trim())
                    .append("password", hashedPassword)
                    .append("specialty", specialty.trim())
                    .append("licenseNumber", licenseNumber.trim().toUpperCase())
                    .append("phoneNo", phoneNo != null ? phoneNo.trim() : "")
                    .append("location", location != null ? location.trim() : "")
                    .append("isVerified", false)
                    .append("isActive", true)
                    .append("rating", 0.0)
                    .append("totalReviews", 0)
                    .append("totalAppointments", 0)
                    .append("completedAppointments", 0)
                    .append("cancelledAppointments", 0)
                    .append("registrationDate", new Date())
                    .append("createdAt", new Date())
                    .append("updatedAt", new Date());
                
                // Save to database
                vets.insertOne(vet);
                
                // Generate JWT token
                String vetId = vet.getObjectId("_id").toString();
                String token = JwtUtil.generateToken(vetId, email, name, "vet");
                
                // Prepare response
                Map<String, Object> vetData = new HashMap<>();
                vetData.put("_id", vetId);
                vetData.put("id", vetId);
                vetData.put("name", vet.getString("name"));
                vetData.put("email", vet.getString("email"));
                vetData.put("specialty", vet.getString("specialty"));
                vetData.put("licenseNumber", vet.getString("licenseNumber"));
                vetData.put("phoneNo", vet.getString("phoneNo"));
                vetData.put("location", vet.getString("location"));
                vetData.put("isVerified", vet.getBoolean("isVerified"));
                vetData.put("userType", "vet");
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Vet registered successfully");
                response.put("token", token);
                response.put("vet", vetData);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(201, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Vet registered: " + email);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // POST /api/vets/login - Login vet
        private void handleVetLogin(HttpExchange exchange) throws IOException {
            try {
                // Read request body
                InputStream inputStream = exchange.getRequestBody();
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                
                // Parse JSON
                Map<String, String> request = gson.fromJson(body, stringMapType);
                String email = request.get("email");
                String password = request.get("password");
                
                // Validation
                if (email == null || password == null) {
                    sendError(exchange, 400, "Email and password are required");
                    return;
                }
                
                // Find vet by email
                MongoCollection<Document> vets = database.getCollection("vets");
                Document vet = vets.find(eq("email", email.toLowerCase().trim())).first();
                if (vet == null) {
                    sendError(exchange, 400, "Invalid credentials");
                    return;
                }
                
                // Check password
                if (!verifyPassword(password, vet.getString("password"))) {
                    sendError(exchange, 400, "Invalid credentials");
                    return;
                }
                
                // Update last login
                vets.updateOne(
                    eq("_id", vet.getObjectId("_id")),
                    new Document("$set", new Document("lastLogin", new Date()))
                );
                
                // Generate JWT token
                String vetId = vet.getObjectId("_id").toString();
                String vetName = vet.getString("name");
                String vetEmail = vet.getString("email");
                String token = JwtUtil.generateToken(vetId, vetEmail, vetName, "vet");
                
                // Prepare response
                Map<String, Object> vetData = new HashMap<>();
                vetData.put("_id", vetId);
                vetData.put("id", vetId);
                vetData.put("name", vetName);
                vetData.put("email", vetEmail);
                vetData.put("specialty", vet.getString("specialty"));
                vetData.put("licenseNumber", vet.getString("licenseNumber"));
                vetData.put("phoneNo", vet.getString("phoneNo"));
                vetData.put("location", vet.getString("location"));
                vetData.put("isVerified", vet.getBoolean("isVerified"));
                vetData.put("userType", "vet");
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Login successful");
                response.put("token", token);
                response.put("vet", vetData);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Vet logged in: " + email);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // GET /api/vets/profile - Get vet profile
        private void handleGetVetProfile(HttpExchange exchange) throws IOException {
            try {
                // Authenticate with JWT
                String vetId = authenticateRequest(exchange);
                if (vetId == null) {
                    sendError(exchange, 401, "Access denied. Invalid or expired token.");
                    return;
                }
                
                // Get vet profile
                MongoCollection<Document> vets = database.getCollection("vets");
                Document vet = vets.find(eq("_id", new ObjectId(vetId))).first();
                if (vet == null) {
                    sendError(exchange, 404, "Vet not found");
                    return;
                }
                
                // Create response without password
                Map<String, Object> response = createVetResponse(vet);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Retrieved vet profile: " + vetId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // PUT /api/vets/profile - Update vet profile
        private void handleUpdateVetProfile(HttpExchange exchange) throws IOException {
            try {
                // Authenticate with JWT
                String vetId = authenticateRequest(exchange);
                if (vetId == null) {
                    sendError(exchange, 401, "Access denied. Invalid or expired token.");
                    return;
                }
                
                // Read request body
                InputStream inputStream = exchange.getRequestBody();
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                
                // Parse JSON
                Map<String, Object> updates = gson.fromJson(body, objectMapType);
                
                // Build update document
                Document updateDoc = new Document("updatedAt", new Date());
                if (updates.get("name") != null) updateDoc.append("name", ((String) updates.get("name")).trim());
                if (updates.get("specialty") != null) updateDoc.append("specialty", ((String) updates.get("specialty")).trim());
                if (updates.get("phoneNo") != null) updateDoc.append("phoneNo", ((String) updates.get("phoneNo")).trim());
                if (updates.get("location") != null) updateDoc.append("location", ((String) updates.get("location")).trim());
                
                MongoCollection<Document> vets = database.getCollection("vets");
                Document result = vets.findOneAndUpdate(
                    eq("_id", new ObjectId(vetId)),
                    new Document("$set", updateDoc),
                    new com.mongodb.client.model.FindOneAndUpdateOptions().returnDocument(com.mongodb.client.model.ReturnDocument.AFTER)
                );
                
                if (result == null) {
                    sendError(exchange, 404, "Vet not found");
                    return;
                }
                
                // Response
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Profile updated successfully");
                response.put("vet", createVetResponse(result));
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Vet profile updated: " + vetId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // GET /api/vets/search - Search for vets
        private void handleSearchVets(HttpExchange exchange) throws IOException {
            try {
                // Parse query parameters
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQueryParams(query);
                
                String specialty = params.get("specialty");
                String location = params.get("location");
                String search = params.get("search");
                
                // Build MongoDB query
                Document mongoQuery = new Document();
                
                if (specialty != null && !specialty.isEmpty()) {
                    mongoQuery.append("specialty", specialty);
                }
                
                List<Document> orConditions = new ArrayList<>();
                
                if (location != null && !location.isEmpty()) {
                    orConditions.add(new Document("location", new Document("$regex", location).append("$options", "i")));
                }
                
                if (search != null && !search.isEmpty()) {
                    orConditions.add(new Document("name", new Document("$regex", search).append("$options", "i")));
                    orConditions.add(new Document("specialty", new Document("$regex", search).append("$options", "i")));
                    orConditions.add(new Document("bio", new Document("$regex", search).append("$options", "i")));
                }
                
                if (!orConditions.isEmpty()) {
                    mongoQuery.append("$or", orConditions);
                }
                
                // Search vets
                MongoCollection<Document> vets = database.getCollection("vets");
                List<Document> vetList = vets.find(mongoQuery)
                    .sort(new Document("rating", -1).append("totalReviews", -1))
                    .limit(20)
                    .into(new ArrayList<>());
                
                // Convert to response format (limited fields for search)
                List<Map<String, Object>> vetResults = new ArrayList<>();
                for (Document vet : vetList) {
                    Map<String, Object> vetData = new HashMap<>();
                    vetData.put("_id", vet.getObjectId("_id").toString());
                    vetData.put("name", vet.getString("name"));
                    vetData.put("specialty", vet.getString("specialty"));
                    vetData.put("location", vet.getString("location"));
                    
                    // Safe conversion for rating (can be Integer or Double)
                    Object ratingObj = vet.get("rating");
                    if (ratingObj instanceof Integer) {
                        vetData.put("rating", ((Integer) ratingObj).doubleValue());
                    } else if (ratingObj instanceof Double) {
                        vetData.put("rating", ratingObj);
                    } else {
                        vetData.put("rating", 0.0);
                    }
                    
                    vetData.put("totalReviews", vet.getInteger("totalReviews"));
                    
                    // Safe conversion for consultationFee (can be Integer or Double)
                    Object feeObj = vet.get("consultationFee");
                    if (feeObj instanceof Integer) {
                        vetData.put("consultationFee", ((Integer) feeObj).doubleValue());
                    } else if (feeObj instanceof Double) {
                        vetData.put("consultationFee", feeObj);
                    } else {
                        vetData.put("consultationFee", 0.0);
                    }
                    
                    vetData.put("bio", vet.getString("bio"));
                    vetData.put("profileImage", vet.getString("profileImage"));
                    vetResults.add(vetData);
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("vets", vetResults);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Retrieved " + vetResults.size() + " vets for search");
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // PUT /api/vets/edit/{id} - Edit vet profile (authenticated)
        private void handleEditVet(HttpExchange exchange) throws IOException {
            try {
                // Authenticate with JWT
                String authenticatedVetId = authenticateRequest(exchange);
                if (authenticatedVetId == null) {
                    sendError(exchange, 401, "Access denied. Invalid or expired token.");
                    return;
                }
                
                // Extract vet ID from URL
                String path = exchange.getRequestURI().getPath();
                String vetIdFromUrl = path.substring(path.lastIndexOf('/') + 1);
                
                // Check if vet can only edit their own profile
                if (!authenticatedVetId.equals(vetIdFromUrl)) {
                    sendError(exchange, 403, "You can only edit your own profile");
                    return;
                }
                
                // Read request body
                InputStream inputStream = exchange.getRequestBody();
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                
                // Parse JSON
                Map<String, Object> updates = gson.fromJson(body, objectMapType);
                
                // Debug: Log the received updates
                System.out.println("🔍 Received updates for vet " + vetIdFromUrl + ":");
                for (Map.Entry<String, Object> entry : updates.entrySet()) {
                    System.out.println("   " + entry.getKey() + " = " + entry.getValue());
                }
                
                // Build update document
                Document updateDoc = new Document("updatedAt", new Date());
                for (Map.Entry<String, Object> entry : updates.entrySet()) {
                    if (!"password".equals(entry.getKey())) { // Don't allow password updates via this endpoint
                        // Allow all updates for optional fields, even if empty
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        
                        // For core required fields, check if not empty
                        if (key.equals("name") || key.equals("email") || key.equals("specialty")) {
                            if (value != null && !value.toString().trim().isEmpty()) {
                                updateDoc.append(key, value);
                                System.out.println("✅ Adding required field: " + key + " = " + value);
                            } else {
                                System.out.println("⚠️  Skipping empty required field: " + key + " = " + value);
                            }
                        } else {
                            // For optional fields (profileImage, experience, etc.), allow empty values
                            updateDoc.append(key, value);
                            System.out.println("✅ Adding optional field: " + key + " = " + value);
                        }
                    }
                }
                
                MongoCollection<Document> vets = database.getCollection("vets");
                Document result = vets.findOneAndUpdate(
                    eq("_id", new ObjectId(vetIdFromUrl)),
                    new Document("$set", updateDoc),
                    new com.mongodb.client.model.FindOneAndUpdateOptions().returnDocument(com.mongodb.client.model.ReturnDocument.AFTER)
                );
                
                if (result == null) {
                    sendError(exchange, 404, "Vet not found or unauthorized");
                    return;
                }
                
                // Response
                Map<String, Object> vetData = new HashMap<>();
                vetData.put("_id", result.getObjectId("_id").toString());
                vetData.put("id", result.getObjectId("_id").toString());
                vetData.put("name", result.getString("name"));
                vetData.put("email", result.getString("email"));
                vetData.put("profileImage", result.getString("profileImage"));
                vetData.put("phoneNo", result.getString("phoneNo"));
                vetData.put("location", result.getString("location"));
                vetData.put("specialty", result.getString("specialty"));
                // Keep experience as-is from database (could be string or number)
                vetData.put("experience", result.get("experience"));
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Vet profile updated successfully");
                response.put("vet", vetData);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Vet profile edited: " + vetIdFromUrl);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // DELETE /api/vets/delete/{id} - Delete vet profile (authenticated)
        private void handleDeleteVet(HttpExchange exchange) throws IOException {
            try {
                // Authenticate with JWT
                String authenticatedVetId = authenticateRequest(exchange);
                if (authenticatedVetId == null) {
                    sendError(exchange, 401, "Access denied. Invalid or expired token.");
                    return;
                }
                
                // Extract vet ID from URL
                String path = exchange.getRequestURI().getPath();
                String vetIdFromUrl = path.substring(path.lastIndexOf('/') + 1);
                
                // Check if vet can only delete their own profile
                if (!authenticatedVetId.equals(vetIdFromUrl)) {
                    sendError(exchange, 403, "You can only delete your own profile");
                    return;
                }
                
                // Find and delete vet
                MongoCollection<Document> vets = database.getCollection("vets");
                Document vet = vets.find(eq("_id", new ObjectId(vetIdFromUrl))).first();
                
                if (vet == null) {
                    sendError(exchange, 404, "Vet not found or unauthorized");
                    return;
                }
                
                vets.deleteOne(eq("_id", new ObjectId(vetIdFromUrl)));
                
                // Response
                Map<String, String> response = new HashMap<>();
                response.put("message", "Vet profile deleted successfully");
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Vet profile deleted: " + vetIdFromUrl);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // Helper method to create vet response without password
        private Map<String, Object> createVetResponse(Document vet) {
            Map<String, Object> response = new HashMap<>();
            response.put("_id", vet.getObjectId("_id").toString());
            response.put("name", vet.getString("name"));
            response.put("email", vet.getString("email"));
            response.put("specialty", vet.getString("specialty"));
            response.put("licenseNumber", vet.getString("licenseNumber"));
            response.put("phoneNo", vet.getString("phoneNo"));
            response.put("location", vet.getString("location"));
            response.put("isVerified", vet.getBoolean("isVerified"));
            response.put("isActive", vet.getBoolean("isActive"));
            response.put("rating", vet.getDouble("rating"));
            response.put("totalReviews", vet.getInteger("totalReviews"));
            response.put("totalAppointments", vet.getInteger("totalAppointments"));
            response.put("completedAppointments", vet.getInteger("completedAppointments"));
            response.put("cancelledAppointments", vet.getInteger("cancelledAppointments"));
            response.put("registrationDate", vet.getDate("registrationDate"));
            response.put("lastLogin", vet.getDate("lastLogin"));
            response.put("createdAt", vet.getDate("createdAt"));
            response.put("profileImage", vet.getString("profileImage"));
            response.put("bio", vet.getString("bio"));
            response.put("consultationFee", vet.getDouble("consultationFee"));
            response.put("emergencyFee", vet.getDouble("emergencyFee"));
            response.put("yearsOfExperience", vet.getInteger("yearsOfExperience"));
            response.put("emergencyAvailable", vet.getBoolean("emergencyAvailable"));
            return response;
        }
        
        // Helper method to parse query parameters
        private Map<String, String> parseQueryParams(String query) {
            Map<String, String> params = new HashMap<>();
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        try {
                            params.put(keyValue[0], java.net.URLDecoder.decode(keyValue[1], "UTF-8"));
                        } catch (Exception e) {
                            params.put(keyValue[0], keyValue[1]);
                        }
                    }
                }
            }
            return params;
        }
    }

    /**
     * Appointment Handler - handles all appointment-related operations
     * Endpoints: GET, POST, PUT, DELETE /api/appointments/*
     */
    static class AppointmentHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
            
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            
            try {
                // Route to appropriate handler based on method and path
                if ("POST".equals(method) && "/api/appointments".equals(path)) {
                    handleCreateAppointment(exchange);
                } else if ("GET".equals(method) && "/api/appointments/farmer".equals(path)) {
                    handleGetFarmerAppointments(exchange);
                } else if ("GET".equals(method) && "/api/appointments/vet".equals(path)) {
                    handleGetVetAppointments(exchange);
                } else if ("GET".equals(method) && path.matches("/api/appointments/vet/[a-f0-9]{24}")) {
                    handleGetVetAppointmentsByVetId(exchange);
                } else if ("GET".equals(method) && path.matches("/api/appointments/[a-f0-9]{24}")) {
                    handleGetSingleAppointment(exchange);
                } else if ("PUT".equals(method) && path.matches("/api/appointments/[a-f0-9]{24}")) {
                    handleUpdateAppointment(exchange);
                } else if ("DELETE".equals(method) && path.matches("/api/appointments/[a-f0-9]{24}")) {
                    handleCancelAppointment(exchange);
                } else if ("DELETE".equals(method) && path.matches("/api/appointments/remove/[a-f0-9]{24}")) {
                    handleDeleteAppointment(exchange);
                } else if ("GET".equals(method) && path.matches("/api/appointments/availability/[a-f0-9]{24}/[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
                    handleGetAvailability(exchange);
                } else {
                    sendError(exchange, 404, "Appointment endpoint not found");
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // POST /api/appointments - Create new appointment
        private void handleCreateAppointment(HttpExchange exchange) throws IOException {
            try {
                // Authenticate with JWT
                String authenticatedUserId = authenticateRequest(exchange);
                if (authenticatedUserId == null) {
                    sendError(exchange, 401, "Access denied. Invalid or expired token.");
                    return;
                }
                
                // Get user type from JWT
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                String token = authHeader.substring(7);
                String userType = JwtUtil.getUserTypeFromToken(token);
                
                if (!"farmer".equals(userType) && !"vet".equals(userType)) {
                    sendError(exchange, 403, "Only farmers and vets can create appointments");
                    return;
                }
                
                // Read request body
                InputStream inputStream = exchange.getRequestBody();
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                
                // Parse JSON
                Map<String, Object> request = gson.fromJson(body, objectMapType);
                
                Document appointmentData = new Document();
                MongoCollection<Document> appointments = database.getCollection("appointments");
                
                if ("farmer".equals(userType)) {
                    // Farmer creating appointment
                    String vetId = (String) request.get("vetId");
                    String animalId = (String) request.get("animalId");
                    String scheduledDate = (String) request.get("scheduledDate");
                    String scheduledTime = (String) request.get("scheduledTime");
                    String symptoms = (String) request.get("symptoms");
                    
                    // Validation
                    if (vetId == null || animalId == null || scheduledDate == null || scheduledTime == null || symptoms == null) {
                        sendError(exchange, 400, "Vet ID, Animal ID, scheduled date, time, and symptoms are required");
                        return;
                    }
                    
                    // Verify vet exists
                    MongoCollection<Document> vets = database.getCollection("vets");
                    Document vet = vets.find(eq("_id", new ObjectId(vetId))).first();
                    if (vet == null) {
                        sendError(exchange, 400, "Vet not found or not available");
                        return;
                    }
                    
                    appointmentData
                        .append("farmerId", new ObjectId(authenticatedUserId))
                        .append("vetId", new ObjectId(vetId))
                        .append("animalId", new ObjectId(animalId))
                        .append("appointmentType", request.getOrDefault("appointmentType", "consultation"))
                        .append("priority", request.getOrDefault("priority", "normal"))
                        .append("scheduledDate", parseDate(scheduledDate))
                        .append("scheduledTime", scheduledTime)
                        .append("duration", ((Number) request.getOrDefault("duration", 30)).intValue())
                        .append("symptoms", symptoms.trim())
                        .append("description", request.get("description") != null ? ((String) request.get("description")).trim() : "")
                        .append("status", "pending")
                        .append("createdAt", new Date())
                        .append("updatedAt", new Date());
                        
                } else {
                    // Vet creating appointment
                    String farmerId = (String) request.get("farmer");
                    String animalName = (String) request.get("animalName");
                    String date = (String) request.get("date");
                    String time = (String) request.get("time");
                    String reason = (String) request.get("reason");
                    
                    // Validation
                    if (farmerId == null || animalName == null || date == null || time == null || reason == null) {
                        sendError(exchange, 400, "Farmer ID, animal name, date, time, and reason are required");
                        return;
                    }
                    
                    // Verify farmer exists
                    MongoCollection<Document> farmers = database.getCollection("farmers");
                    Document farmer = farmers.find(eq("_id", new ObjectId(farmerId))).first();
                    if (farmer == null) {
                        sendError(exchange, 400, "Farmer not found");
                        return;
                    }
                    
                    appointmentData
                        .append("farmerId", new ObjectId(farmerId))
                        .append("vetId", new ObjectId(authenticatedUserId))
                        .append("animalName", animalName.trim())
                        .append("appointmentType", "consultation")
                        .append("priority", "normal")
                        .append("scheduledDate", parseDate(date))
                        .append("scheduledTime", time)
                        .append("duration", 30)
                        .append("symptoms", reason.trim())
                        .append("description", request.get("notes") != null ? ((String) request.get("notes")).trim() : "")
                        .append("status", request.getOrDefault("status", "pending"))
                        .append("createdAt", new Date())
                        .append("updatedAt", new Date());
                }
                
                // Save appointment
                appointments.insertOne(appointmentData);
                
                // Create response
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Appointment created successfully");
                response.put("appointment", createAppointmentResponse(appointmentData));
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(201, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Appointment created successfully");
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // GET /api/appointments/farmer - Get farmer's appointments
        private void handleGetFarmerAppointments(HttpExchange exchange) throws IOException {
            try {
                // Authenticate with JWT
                String farmerId = authenticateRequest(exchange);
                if (farmerId == null) {
                    sendError(exchange, 401, "Access denied. Invalid or expired token.");
                    return;
                }
                
                // Verify user is a farmer
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                String token = authHeader.substring(7);
                String userType = JwtUtil.getUserTypeFromToken(token);
                
                if (!"farmer".equals(userType)) {
                    sendError(exchange, 403, "Access denied");
                    return;
                }
                
                // Parse query parameters
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQueryParams(query);
                String status = params.get("status");
                int page = parseInt(params.getOrDefault("page", "1"));
                int limit = parseInt(params.getOrDefault("limit", "10"));
                
                // Build query
                Document queryDoc = new Document("farmerId", new ObjectId(farmerId));
                if (status != null && !status.isEmpty()) {
                    queryDoc.append("status", status);
                }
                
                MongoCollection<Document> appointments = database.getCollection("appointments");
                
                // Get appointments with pagination
                List<Document> appointmentList = appointments.find(queryDoc)
                    .sort(new Document("createdAt", -1))
                    .skip((page - 1) * limit)
                    .limit(limit)
                    .into(new ArrayList<>());
                
                long total = appointments.countDocuments(queryDoc);
                
                // Format response
                List<Map<String, Object>> formattedAppointments = new ArrayList<>();
                for (Document appointment : appointmentList) {
                    formattedAppointments.add(createAppointmentResponse(appointment));
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("appointments", formattedAppointments);
                
                Map<String, Object> pagination = new HashMap<>();
                pagination.put("current", page);
                pagination.put("pages", Math.ceil((double) total / limit));
                pagination.put("total", total);
                response.put("pagination", pagination);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Retrieved " + appointmentList.size() + " appointments for farmer: " + farmerId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // GET /api/appointments/vet - Get vet's appointments
        private void handleGetVetAppointments(HttpExchange exchange) throws IOException {
            try {
                // Authenticate with JWT
                String vetId = authenticateRequest(exchange);
                if (vetId == null) {
                    sendError(exchange, 401, "Access denied. Invalid or expired token.");
                    return;
                }
                
                // Verify user is a vet
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                String token = authHeader.substring(7);
                String userType = JwtUtil.getUserTypeFromToken(token);
                
                if (!"vet".equals(userType)) {
                    sendError(exchange, 403, "Access denied");
                    return;
                }
                
                // Parse query parameters
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQueryParams(query);
                String status = params.get("status");
                String date = params.get("date");
                int page = parseInt(params.getOrDefault("page", "1"));
                int limit = parseInt(params.getOrDefault("limit", "10"));
                
                // Build query
                Document queryDoc = new Document("vetId", new ObjectId(vetId));
                if (status != null && !status.isEmpty()) {
                    queryDoc.append("status", status);
                }
                if (date != null && !date.isEmpty()) {
                    Date selectedDate = parseDate(date);
                    Date startOfDay = new Date(selectedDate.getTime());
                    Date endOfDay = new Date(selectedDate.getTime() + 24 * 60 * 60 * 1000 - 1);
                    queryDoc.append("scheduledDate", new Document("$gte", startOfDay).append("$lte", endOfDay));
                }
                
                MongoCollection<Document> appointments = database.getCollection("appointments");
                
                // Get appointments with pagination
                List<Document> appointmentList = appointments.find(queryDoc)
                    .sort(new Document("scheduledDate", 1).append("scheduledTime", 1))
                    .skip((page - 1) * limit)
                    .limit(limit)
                    .into(new ArrayList<>());
                
                long total = appointments.countDocuments(queryDoc);
                
                // Format response for vet dashboard
                List<Map<String, Object>> formattedAppointments = new ArrayList<>();
                for (Document appointment : appointmentList) {
                    Map<String, Object> appointmentData = createAppointmentResponse(appointment);
                    
                    // Add additional fields for vet dashboard
                    appointmentData.put("farmerName", getFarmerName(appointment.getObjectId("farmerId")));
                    appointmentData.put("animalType", getAnimalType(appointment.getObjectId("animalId")));
                    appointmentData.put("animalName", getAnimalName(appointment.getObjectId("animalId"), appointment.getString("animalName")));
                    appointmentData.put("date", appointment.getDate("scheduledDate"));
                    appointmentData.put("id", appointment.getObjectId("_id").toString());
                    
                    formattedAppointments.add(appointmentData);
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("appointments", formattedAppointments);
                
                Map<String, Object> pagination = new HashMap<>();
                pagination.put("current", page);
                pagination.put("pages", Math.ceil((double) total / limit));
                pagination.put("total", total);
                response.put("pagination", pagination);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Retrieved " + appointmentList.size() + " appointments for vet: " + vetId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // GET /api/appointments/vet/{vetId} - Get vet's appointments by vet ID (legacy route)
        private void handleGetVetAppointmentsByVetId(HttpExchange exchange) throws IOException {
            // This is essentially the same as handleGetVetAppointments but validates the vetId parameter
            handleGetVetAppointments(exchange);
        }
        
        // GET /api/appointments/{id} - Get single appointment
        private void handleGetSingleAppointment(HttpExchange exchange) throws IOException {
            try {
                // Authenticate with JWT
                String userId = authenticateRequest(exchange);
                if (userId == null) {
                    sendError(exchange, 401, "Access denied. Invalid or expired token.");
                    return;
                }
                
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                String token = authHeader.substring(7);
                String userType = JwtUtil.getUserTypeFromToken(token);
                
                // Extract appointment ID from URL
                String path = exchange.getRequestURI().getPath();
                String appointmentId = path.substring(path.lastIndexOf('/') + 1);
                
                MongoCollection<Document> appointments = database.getCollection("appointments");
                Document appointment = appointments.find(eq("_id", new ObjectId(appointmentId))).first();
                
                if (appointment == null) {
                    sendError(exchange, 404, "Appointment not found");
                    return;
                }
                
                // Check access permissions
                boolean hasAccess = false;
                if ("farmer".equals(userType) && appointment.getObjectId("farmerId").toString().equals(userId)) {
                    hasAccess = true;
                } else if ("vet".equals(userType) && appointment.getObjectId("vetId").toString().equals(userId)) {
                    hasAccess = true;
                }
                
                if (!hasAccess) {
                    sendError(exchange, 403, "Access denied");
                    return;
                }
                
                Map<String, Object> response = createAppointmentResponse(appointment);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Retrieved appointment: " + appointmentId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // PUT /api/appointments/{id} - Update appointment status
        private void handleUpdateAppointment(HttpExchange exchange) throws IOException {
            try {
                // Authenticate with JWT
                String userId = authenticateRequest(exchange);
                if (userId == null) {
                    sendError(exchange, 401, "Access denied. Invalid or expired token.");
                    return;
                }
                
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                String token = authHeader.substring(7);
                String userType = JwtUtil.getUserTypeFromToken(token);
                
                if (!"vet".equals(userType)) {
                    sendError(exchange, 403, "Only vets can update appointment status");
                    return;
                }
                
                // Extract appointment ID from URL
                String path = exchange.getRequestURI().getPath();
                String appointmentId = path.substring(path.lastIndexOf('/') + 1);
                
                // Read request body
                InputStream inputStream = exchange.getRequestBody();
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                
                // Parse JSON
                Map<String, Object> updates = gson.fromJson(body, objectMapType);
                
                MongoCollection<Document> appointments = database.getCollection("appointments");
                Document appointment = appointments.find(
                    and(eq("_id", new ObjectId(appointmentId)), eq("vetId", new ObjectId(userId)))
                ).first();
                
                if (appointment == null) {
                    sendError(exchange, 404, "Appointment not found");
                    return;
                }
                
                // Build update document
                Document updateDoc = new Document("updatedAt", new Date());
                if (updates.get("status") != null) updateDoc.append("status", updates.get("status"));
                if (updates.get("diagnosis") != null) updateDoc.append("diagnosis", updates.get("diagnosis"));
                if (updates.get("treatment") != null) updateDoc.append("treatment", updates.get("treatment"));
                if (updates.get("prescription") != null) updateDoc.append("prescription", updates.get("prescription"));
                if (updates.get("vetNotes") != null) updateDoc.append("vetNotes", updates.get("vetNotes"));
                if (updates.get("followUpRequired") != null) updateDoc.append("followUpRequired", updates.get("followUpRequired"));
                if (updates.get("followUpDate") != null) updateDoc.append("followUpDate", parseDate((String) updates.get("followUpDate")));
                
                appointments.updateOne(
                    eq("_id", new ObjectId(appointmentId)),
                    new Document("$set", updateDoc)
                );
                
                // Update vet statistics if status changed
                String newStatus = (String) updates.get("status");
                if ("completed".equals(newStatus)) {
                    MongoCollection<Document> vets = database.getCollection("vets");
                    vets.updateOne(
                        eq("_id", new ObjectId(userId)),
                        new Document("$inc", new Document("completedAppointments", 1))
                    );
                } else if ("cancelled".equals(newStatus)) {
                    MongoCollection<Document> vets = database.getCollection("vets");
                    vets.updateOne(
                        eq("_id", new ObjectId(userId)),
                        new Document("$inc", new Document("cancelledAppointments", 1))
                    );
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Appointment updated successfully");
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Appointment updated: " + appointmentId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // DELETE /api/appointments/{id} - Cancel appointment
        private void handleCancelAppointment(HttpExchange exchange) throws IOException {
            try {
                // Authenticate with JWT
                String userId = authenticateRequest(exchange);
                if (userId == null) {
                    sendError(exchange, 401, "Access denied. Invalid or expired token.");
                    return;
                }
                
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                String token = authHeader.substring(7);
                String userType = JwtUtil.getUserTypeFromToken(token);
                
                // Extract appointment ID from URL
                String path = exchange.getRequestURI().getPath();
                String appointmentId = path.substring(path.lastIndexOf('/') + 1);
                
                // Read request body for cancellation reason
                InputStream inputStream = exchange.getRequestBody();
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, Object> request = new HashMap<>();
                if (!body.isEmpty()) {
                    request = gson.fromJson(body, objectMapType);
                }
                
                MongoCollection<Document> appointments = database.getCollection("appointments");
                Document appointment = appointments.find(eq("_id", new ObjectId(appointmentId))).first();
                
                if (appointment == null) {
                    sendError(exchange, 404, "Appointment not found");
                    return;
                }
                
                // Check permissions
                boolean hasPermission = false;
                if ("farmer".equals(userType) && appointment.getObjectId("farmerId").toString().equals(userId)) {
                    hasPermission = true;
                } else if ("vet".equals(userType) && appointment.getObjectId("vetId").toString().equals(userId)) {
                    hasPermission = true;
                }
                
                if (!hasPermission) {
                    sendError(exchange, 403, "Access denied");
                    return;
                }
                
                // Update appointment to cancelled
                Document updateDoc = new Document()
                    .append("status", "cancelled")
                    .append("cancelledBy", userType)
                    .append("cancellationReason", request.get("reason"))
                    .append("cancelledAt", new Date())
                    .append("updatedAt", new Date());
                
                appointments.updateOne(
                    eq("_id", new ObjectId(appointmentId)),
                    new Document("$set", updateDoc)
                );
                
                // Update vet statistics if cancelled by vet
                if ("vet".equals(userType)) {
                    MongoCollection<Document> vets = database.getCollection("vets");
                    vets.updateOne(
                        eq("_id", new ObjectId(userId)),
                        new Document("$inc", new Document("cancelledAppointments", 1))
                    );
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Appointment cancelled successfully");
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Appointment cancelled: " + appointmentId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // DELETE /api/appointments/remove/{id} - Delete appointment permanently
        private void handleDeleteAppointment(HttpExchange exchange) throws IOException {
            try {
                // Authenticate with JWT
                String userId = authenticateRequest(exchange);
                if (userId == null) {
                    sendError(exchange, 401, "Access denied. Invalid or expired token.");
                    return;
                }
                
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                String token = authHeader.substring(7);
                String userType = JwtUtil.getUserTypeFromToken(token);
                
                // Extract appointment ID from URL
                String path = exchange.getRequestURI().getPath();
                String appointmentId = path.substring(path.lastIndexOf('/') + 1);
                
                MongoCollection<Document> appointments = database.getCollection("appointments");
                Document appointment = appointments.find(eq("_id", new ObjectId(appointmentId))).first();
                
                if (appointment == null) {
                    sendError(exchange, 404, "Appointment not found");
                    return;
                }
                
                // Check permissions
                boolean hasPermission = false;
                if ("farmer".equals(userType) && appointment.getObjectId("farmerId").toString().equals(userId)) {
                    hasPermission = true;
                } else if ("vet".equals(userType) && appointment.getObjectId("vetId").toString().equals(userId)) {
                    hasPermission = true;
                }
                
                if (!hasPermission) {
                    sendError(exchange, 403, "Access denied");
                    return;
                }
                
                // Delete appointment permanently
                appointments.deleteOne(eq("_id", new ObjectId(appointmentId)));
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Appointment deleted successfully");
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Appointment deleted: " + appointmentId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // GET /api/appointments/availability/{vetId}/{date} - Get available time slots
        private void handleGetAvailability(HttpExchange exchange) throws IOException {
            try {
                // Authenticate with JWT
                String userId = authenticateRequest(exchange);
                if (userId == null) {
                    sendError(exchange, 401, "Access denied. Invalid or expired token.");
                    return;
                }
                
                // Extract vetId and date from URL
                String path = exchange.getRequestURI().getPath();
                String[] pathParts = path.split("/");
                String vetId = pathParts[pathParts.length - 2];
                String dateStr = pathParts[pathParts.length - 1];
                
                // Verify vet exists
                MongoCollection<Document> vets = database.getCollection("vets");
                Document vet = vets.find(eq("_id", new ObjectId(vetId))).first();
                if (vet == null) {
                    sendError(exchange, 404, "Vet not found or not available");
                    return;
                }
                
                // Parse date
                Date selectedDate = parseDate(dateStr);
                
                // Get existing appointments for the day
                MongoCollection<Document> appointments = database.getCollection("appointments");
                Date startOfDay = new Date(selectedDate.getTime());
                Date endOfDay = new Date(selectedDate.getTime() + 24 * 60 * 60 * 1000 - 1);
                
                List<Document> existingAppointments = appointments.find(
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
                
                Map<String, Object> response = new HashMap<>();
                response.put("availableSlots", availableSlots);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Retrieved availability for vet: " + vetId + " on " + dateStr);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // Helper method to create appointment response
        private Map<String, Object> createAppointmentResponse(Document appointment) {
            Map<String, Object> response = new HashMap<>();
            response.put("_id", appointment.getObjectId("_id").toString());
            response.put("farmerId", appointment.getObjectId("farmerId").toString());
            response.put("vetId", appointment.getObjectId("vetId").toString());
            if (appointment.getObjectId("animalId") != null) {
                response.put("animalId", appointment.getObjectId("animalId").toString());
            }
            response.put("animalName", appointment.getString("animalName"));
            response.put("appointmentType", appointment.getString("appointmentType"));
            response.put("priority", appointment.getString("priority"));
            response.put("scheduledDate", appointment.getDate("scheduledDate"));
            response.put("scheduledTime", appointment.getString("scheduledTime"));
            response.put("duration", appointment.getInteger("duration"));
            response.put("symptoms", appointment.getString("symptoms"));
            response.put("description", appointment.getString("description"));
            response.put("status", appointment.getString("status"));
            response.put("diagnosis", appointment.getString("diagnosis"));
            response.put("treatment", appointment.getString("treatment"));
            response.put("prescription", appointment.getString("prescription"));
            response.put("vetNotes", appointment.getString("vetNotes"));
            response.put("followUpRequired", appointment.getBoolean("followUpRequired"));
            response.put("followUpDate", appointment.getDate("followUpDate"));
            response.put("cancelledBy", appointment.getString("cancelledBy"));
            response.put("cancellationReason", appointment.getString("cancellationReason"));
            response.put("cancelledAt", appointment.getDate("cancelledAt"));
            response.put("createdAt", appointment.getDate("createdAt"));
            response.put("updatedAt", appointment.getDate("updatedAt"));
            return response;
        }
        
        // Helper method to get farmer name
        private String getFarmerName(ObjectId farmerId) {
            try {
                if (farmerId == null) return "Unknown Farmer";
                MongoCollection<Document> farmers = database.getCollection("farmers");
                Document farmer = farmers.find(eq("_id", farmerId)).first();
                return farmer != null ? farmer.getString("name") : "Unknown Farmer";
            } catch (Exception e) {
                return "Unknown Farmer";
            }
        }
        
        // Helper method to get animal type
        private String getAnimalType(ObjectId animalId) {
            try {
                if (animalId == null) return "Unknown";
                MongoCollection<Document> animals = database.getCollection("animals");
                Document animal = animals.find(eq("_id", animalId)).first();
                return animal != null ? animal.getString("species") : "Unknown";
            } catch (Exception e) {
                return "Unknown";
            }
        }
        
        // Helper method to get animal name
        private String getAnimalName(ObjectId animalId, String fallbackName) {
            try {
                if (animalId == null) return fallbackName != null ? fallbackName : "Unknown";
                MongoCollection<Document> animals = database.getCollection("animals");
                Document animal = animals.find(eq("_id", animalId)).first();
                return animal != null ? animal.getString("name") : (fallbackName != null ? fallbackName : "Unknown");
            } catch (Exception e) {
                return fallbackName != null ? fallbackName : "Unknown";
            }
        }
        
        // Helper method to parse integer safely
        private int parseInt(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        
        // Helper method to parse query parameters
        private Map<String, String> parseQueryParams(String query) {
            Map<String, String> params = new HashMap<>();
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        try {
                            params.put(keyValue[0], java.net.URLDecoder.decode(keyValue[1], "UTF-8"));
                        } catch (Exception e) {
                            params.put(keyValue[0], keyValue[1]);
                        }
                    }
                }
            }
            return params;
        }
    }
    
    /**
     * Message Handler - handles all message-related operations
     * Endpoints: GET, POST, PUT, DELETE /api/messages/*
     */
    static class MessageHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
            
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            
            System.out.println("🔍 Message API Request: " + method + " " + path);
            
            try {
                // Route to appropriate handler based on method and path
                if ("POST".equals(method) && "/api/messages".equals(path)) {
                    handleSendMessage(exchange);
                } else if ("GET".equals(method) && path.matches("/api/messages/conversation/[a-f0-9]{24}/(farmer|vet)")) {
                    handleGetConversation(exchange);
                } else if ("GET".equals(method) && "/api/messages/conversations".equals(path)) {
                    handleGetConversations(exchange);
                } else if ("GET".equals(method) && "/api/messages/vet".equals(path)) {
                    handleGetVetMessages(exchange);
                } else if ("GET".equals(method) && "/api/messages/farmer".equals(path)) {
                    handleGetFarmerMessages(exchange);
                } else if ("PUT".equals(method) && path.matches("/api/messages/[a-f0-9]{24}/read")) {
                    handleMarkAsRead(exchange);
                } else if ("DELETE".equals(method) && path.matches("/api/messages/[a-f0-9]{24}")) {
                    handleDeleteMessage(exchange);
                } else if ("GET".equals(method) && "/api/messages/unread-count".equals(path)) {
                    handleGetUnreadCount(exchange);
                } else if ("GET".equals(method) && "/api/messages/search".equals(path)) {
                    handleSearchMessages(exchange);
                } else {
                    sendError(exchange, 404, "Message endpoint not found");
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // POST /api/messages - Send a message
        private void handleSendMessage(HttpExchange exchange) throws IOException {
            try {
                // Authenticate user
                Map<String, Object> authResult = authenticateUser(exchange);
                if (authResult.containsKey("error")) {
                    sendError(exchange, 401, (String) authResult.get("error"));
                    return;
                }
                
                String userId = (String) authResult.get("userId");
                String userType = (String) authResult.get("userType");
                
                System.out.println("? Message API Request: POST /api/messages");
                System.out.println("? Sender: " + userType + " " + userId);
                
                // Read request body
                InputStream inputStream = exchange.getRequestBody();
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                
                // Parse JSON
                Map<String, Object> request = gson.fromJson(body, objectMapType);
                String receiverId = (String) request.get("receiverId");
                String receiverType = (String) request.get("receiverType");
                String content = (String) request.get("content");
                String messageType = (String) request.getOrDefault("messageType", "text");
                String priority = (String) request.getOrDefault("priority", "normal");
                
                System.out.println("? Receiver: " + receiverType + " " + receiverId);
                System.out.println("? Content: " + content);
                
                // Validation
                if (receiverId == null || receiverType == null || content == null || content.trim().isEmpty()) {
                    sendError(exchange, 400, "Receiver ID, receiver type, and content are required");
                    return;
                }
                
                if (!Arrays.asList("farmer", "vet").contains(receiverType)) {
                    sendError(exchange, 400, "Invalid receiver type");
                    return;
                }
                
                // Verify receiver exists
                MongoCollection<Document> receiverCollection = receiverType.equals("farmer") ? 
                    database.getCollection("farmers") : database.getCollection("vets");
                Document receiver = receiverCollection.find(eq("_id", new ObjectId(receiverId))).first();
                
                if (receiver == null) {
                    sendError(exchange, 404, "Receiver not found");
                    return;
                }
                
                // Generate conversation ID (consistent for both participants)
                List<String> participantIds = Arrays.asList(userId, receiverId);
                Collections.sort(participantIds);
                String conversationId = participantIds.get(0) + "_" + participantIds.get(1);
                
                // Create message document
                Document message = new Document()
                    .append("senderId", new ObjectId(userId))
                    .append("senderType", userType)
                    .append("receiverId", new ObjectId(receiverId))
                    .append("receiverType", receiverType)
                    .append("conversationId", conversationId)
                    .append("content", content.trim())
                    .append("messageType", messageType)
                    .append("priority", priority)
                    .append("isRead", false)
                    .append("isDeleted", false)
                    .append("createdAt", new Date())
                    .append("updatedAt", new Date());
                
                // Save to database
                MongoCollection<Document> messages = database.getCollection("messages");
                messages.insertOne(message);
                
                // Get sender and receiver info for response
                MongoCollection<Document> senderCollection = userType.equals("farmer") ? 
                    database.getCollection("farmers") : database.getCollection("vets");
                Document senderInfo = senderCollection.find(eq("_id", new ObjectId(userId))).first();
                Document receiverInfo = receiver;
                
                if (senderInfo == null) {
                    sendError(exchange, 404, "Sender not found in database");
                    return;
                }
                
                // Prepare response
                Map<String, Object> responseMessage = new HashMap<>();
                responseMessage.put("_id", message.getObjectId("_id").toString());
                responseMessage.put("senderId", userId);
                responseMessage.put("senderType", userType);
                responseMessage.put("receiverId", receiverId);
                responseMessage.put("receiverType", receiverType);
                responseMessage.put("conversationId", conversationId);
                responseMessage.put("content", content.trim());
                responseMessage.put("messageType", messageType);
                responseMessage.put("priority", priority);
                responseMessage.put("isRead", false);
                responseMessage.put("createdAt", message.getDate("createdAt"));
                
                Map<String, Object> senderData = new HashMap<>();
                senderData.put("name", senderInfo.getString("name"));
                senderData.put("email", senderInfo.getString("email"));
                if (userType.equals("vet")) {
                    senderData.put("specialty", senderInfo.getString("specialty"));
                }
                responseMessage.put("senderInfo", senderData);
                
                Map<String, Object> receiverData = new HashMap<>();
                receiverData.put("name", receiverInfo.getString("name"));
                receiverData.put("email", receiverInfo.getString("email"));
                if (receiverType.equals("vet")) {
                    receiverData.put("specialty", receiverInfo.getString("specialty"));
                }
                responseMessage.put("receiverInfo", receiverData);
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Message sent successfully");
                response.put("data", responseMessage);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(201, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Message sent from " + userType + " " + userId + " to " + receiverType + " " + receiverId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // GET /api/messages/conversation/{receiverId}/{receiverType} - Get conversation
        private void handleGetConversation(HttpExchange exchange) throws IOException {
            try {
                // Authenticate user
                Map<String, Object> authResult = authenticateUser(exchange);
                if (authResult.containsKey("error")) {
                    sendError(exchange, 401, (String) authResult.get("error"));
                    return;
                }
                
                String userId = (String) authResult.get("userId");
                String userType = (String) authResult.get("userType");
                
                // Extract parameters from URL
                String path = exchange.getRequestURI().getPath();
                String[] pathParts = path.split("/");
                String receiverId = pathParts[4];
                String receiverType = pathParts[5];
                
                // Parse query parameters
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQueryParams(query);
                int page = Integer.parseInt(params.getOrDefault("page", "1"));
                int limit = Integer.parseInt(params.getOrDefault("limit", "50"));
                
                // Generate conversation ID
                List<String> participantIds = Arrays.asList(userId, receiverId);
                Collections.sort(participantIds);
                String conversationId = participantIds.get(0) + "_" + participantIds.get(1);
                
                // Get messages from database
                MongoCollection<Document> messages = database.getCollection("messages");
                List<Document> messageList = messages.find(eq("conversationId", conversationId))
                    .sort(new Document("createdAt", -1))
                    .limit(limit)
                    .skip((page - 1) * limit)
                    .into(new ArrayList<>());
                
                // Mark messages as read (messages received by current user)
                messages.updateMany(
                    and(
                        eq("conversationId", conversationId),
                        eq("receiverId", new ObjectId(userId)),
                        eq("receiverType", userType),
                        eq("isRead", false)
                    ),
                    new Document("$set", new Document("isRead", true).append("updatedAt", new Date()))
                );
                
                // Get participant info and validate receiver exists
                MongoCollection<Document> participantCollection = receiverType.equals("farmer") ? 
                    database.getCollection("farmers") : database.getCollection("vets");
                Document participantInfo = participantCollection.find(eq("_id", new ObjectId(receiverId))).first();
                
                // Validate that the receiver exists in the specified collection
                if (participantInfo == null) {
                    sendError(exchange, 404, "Receiver not found or invalid receiver type");
                    return;
                }
                
                // Convert messages to response format
                List<Map<String, Object>> responseMessages = new ArrayList<>();
                for (Document msg : messageList) {
                    Map<String, Object> messageData = new HashMap<>();
                    messageData.put("_id", msg.getObjectId("_id").toString());
                    messageData.put("senderId", msg.getObjectId("senderId").toString());
                    messageData.put("senderType", msg.getString("senderType"));
                    messageData.put("receiverId", msg.getObjectId("receiverId").toString());
                    messageData.put("receiverType", msg.getString("receiverType"));
                    messageData.put("content", msg.getString("content"));
                    messageData.put("messageType", msg.getString("messageType"));
                    messageData.put("isRead", msg.getBoolean("isRead"));
                    messageData.put("createdAt", msg.getDate("createdAt"));
                    responseMessages.add(messageData);
                    
                    // Debug: log message details
                    System.out.println("? Message: " + msg.getString("content") + 
                                     " (from " + msg.getString("senderType") + " " + msg.getObjectId("senderId").toString() + 
                                     " to " + msg.getString("receiverType") + " " + msg.getObjectId("receiverId").toString() + ")");
                }
                
                // Prepare participant info
                Map<String, Object> participant = new HashMap<>();
                participant.put("name", participantInfo.getString("name"));
                participant.put("email", participantInfo.getString("email"));
                participant.put("phoneNo", participantInfo.getString("phoneNo"));
                if (receiverType.equals("vet")) {
                    participant.put("specialty", participantInfo.getString("specialty"));
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("messages", responseMessages);
                response.put("participant", participant);
                
                Map<String, Object> pagination = new HashMap<>();
                pagination.put("current", page);
                pagination.put("limit", limit);
                pagination.put("hasMore", messageList.size() == limit);
                response.put("pagination", pagination);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Retrieved conversation between " + userType + " " + userId + " and " + receiverType + " " + receiverId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // GET /api/messages/conversations - Get user's conversations list
        private void handleGetConversations(HttpExchange exchange) throws IOException {
            try {
                // Authenticate user
                Map<String, Object> authResult = authenticateUser(exchange);
                if (authResult.containsKey("error")) {
                    sendError(exchange, 401, (String) authResult.get("error"));
                    return;
                }
                
                String userId = (String) authResult.get("userId");
                String userType = (String) authResult.get("userType");
                
                System.out.println("? Message API Request: GET /api/messages/conversations");
                System.out.println("? Requesting user: " + userType + " " + userId);
                
                // Get user's conversations using aggregation
                MongoCollection<Document> messages = database.getCollection("messages");
                
                // Aggregation pipeline to get latest message per conversation
                List<Document> pipeline = Arrays.asList(
                    new Document("$match", new Document("$or", Arrays.asList(
                        new Document("senderId", new ObjectId(userId)).append("senderType", userType),
                        new Document("receiverId", new ObjectId(userId)).append("receiverType", userType)
                    )).append("isDeleted", false)),
                    new Document("$sort", new Document("createdAt", -1)),
                    new Document("$group", new Document("_id", "$conversationId")
                        .append("lastMessage", new Document("$first", "$$ROOT"))
                        .append("totalMessages", new Document("$sum", 1))
                        .append("unreadCount", new Document("$sum", 
                            new Document("$cond", Arrays.asList(
                                new Document("$and", Arrays.asList(
                                    new Document("$eq", Arrays.asList("$receiverId", new ObjectId(userId))),
                                    new Document("$eq", Arrays.asList("$receiverType", userType)),
                                    new Document("$eq", Arrays.asList("$isRead", false))
                                )),
                                1,
                                0
                            ))
                        ))
                    ),
                    new Document("$sort", new Document("lastMessage.createdAt", -1))
                );
                
                List<Document> conversations = messages.aggregate(pipeline).into(new ArrayList<>());
                
                // Process conversations and get participant info
                List<Map<String, Object>> responseConversations = new ArrayList<>();
                for (Document conv : conversations) {
                    Document lastMessage = (Document) conv.get("lastMessage");
                    
                    // Determine the other participant
                    boolean isUserSender = lastMessage.getObjectId("senderId").toString().equals(userId);
                    String otherUserId = isUserSender ? 
                        lastMessage.getObjectId("receiverId").toString() : 
                        lastMessage.getObjectId("senderId").toString();
                    String otherUserType = isUserSender ? 
                        lastMessage.getString("receiverType") : 
                        lastMessage.getString("senderType");
                    
                    System.out.println("? Last message details: senderId=" + lastMessage.getObjectId("senderId").toString() + 
                                     ", receiverId=" + lastMessage.getObjectId("receiverId").toString() + 
                                     ", senderType=" + lastMessage.getString("senderType") + 
                                     ", receiverType=" + lastMessage.getString("receiverType"));
                    System.out.println("? Current user: " + userType + " " + userId + ", isUserSender=" + isUserSender);
                    System.out.println("? Processing conversation - Other participant: " + otherUserType + " " + otherUserId);
                    
                    // Additional validation to ensure we're not using the same user as participant
                    if (otherUserId.equals(userId)) {
                        System.out.println("? ERROR: Other participant ID is same as current user ID! Skipping conversation.");
                        continue;
                    }
                    
                    // Get participant info
                    MongoCollection<Document> participantCollection = otherUserType.equals("farmer") ? 
                        database.getCollection("farmers") : database.getCollection("vets");
                    Document participantInfo = participantCollection.find(eq("_id", new ObjectId(otherUserId))).first();
                    
                    if (participantInfo != null) {
                        Map<String, Object> participant = new HashMap<>();
                        participant.put("id", otherUserId);
                        participant.put("name", participantInfo.getString("name"));
                        participant.put("email", participantInfo.getString("email"));
                        participant.put("phoneNo", participantInfo.getString("phoneNo"));
                        participant.put("type", otherUserType); // Add participant type
                        if (otherUserType.equals("vet")) {
                            participant.put("specialty", participantInfo.getString("specialty"));
                        }
                        
                        System.out.println("? Conversation participant - ID: " + otherUserId + ", Type: " + otherUserType + ", Name: " + participantInfo.getString("name"));
                        
                        Map<String, Object> lastMsg = new HashMap<>();
                        lastMsg.put("content", lastMessage.getString("content"));
                        lastMsg.put("timestamp", lastMessage.getDate("createdAt"));
                        lastMsg.put("isFromUser", isUserSender);
                        lastMsg.put("messageType", lastMessage.getString("messageType"));
                        
                        Map<String, Object> conversation = new HashMap<>();
                        conversation.put("conversationId", conv.getString("_id"));
                        conversation.put("participant", participant);
                        conversation.put("lastMessage", lastMsg);
                        conversation.put("unreadCount", conv.getInteger("unreadCount"));
                        conversation.put("totalMessages", conv.getInteger("totalMessages"));
                        
                        responseConversations.add(conversation);
                    }
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("conversations", responseConversations);
                
                String jsonResponse = gson.toJson(response);
                System.out.println("? Conversations JSON response: " + jsonResponse);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Retrieved " + responseConversations.size() + " conversations for " + userType + " " + userId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // GET /api/messages/vet - Get messages for vet dashboard
        private void handleGetVetMessages(HttpExchange exchange) throws IOException {
            try {
                // Authenticate user
                Map<String, Object> authResult = authenticateUser(exchange);
                if (authResult.containsKey("error")) {
                    sendError(exchange, 401, (String) authResult.get("error"));
                    return;
                }
                
                String userId = (String) authResult.get("userId");
                String userType = (String) authResult.get("userType");
                
                if (!"vet".equals(userType)) {
                    sendError(exchange, 403, "Access denied");
                    return;
                }
                
                // Parse query parameters
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQueryParams(query);
                int limit = Integer.parseInt(params.getOrDefault("limit", "10"));
                
                // Get recent unread messages for the vet
                MongoCollection<Document> messages = database.getCollection("messages");
                List<Document> messageList = messages.find(
                    and(
                        eq("receiverId", new ObjectId(userId)),
                        eq("receiverType", "vet"),
                        eq("isRead", false),
                        eq("isDeleted", false)
                    )
                ).sort(new Document("createdAt", -1))
                .limit(limit)
                .into(new ArrayList<>());
                
                // Get sender info for each message
                List<Map<String, Object>> dashboardMessages = new ArrayList<>();
                for (Document msg : messageList) {
                    String senderId = msg.getObjectId("senderId").toString();
                    String senderType = msg.getString("senderType");
                    
                    MongoCollection<Document> senderCollection = senderType.equals("farmer") ? 
                        database.getCollection("farmers") : database.getCollection("vets");
                    Document senderInfo = senderCollection.find(eq("_id", new ObjectId(senderId))).first();
                    
                    if (senderInfo != null) {
                        Map<String, Object> dashboardMessage = new HashMap<>();
                        dashboardMessage.put("id", msg.getObjectId("_id").toString());
                        if (senderType.equals("farmer")) {
                            dashboardMessage.put("farmerName", senderInfo.getString("name"));
                        } else {
                            dashboardMessage.put("vetName", senderInfo.getString("name"));
                        }
                        dashboardMessage.put("content", msg.getString("content"));
                        dashboardMessage.put("timestamp", msg.getDate("createdAt"));
                        dashboardMessage.put("read", msg.getBoolean("isRead"));
                        dashboardMessage.put("priority", msg.getString("priority"));
                        
                        dashboardMessages.add(dashboardMessage);
                    }
                }
                
                String jsonResponse = gson.toJson(dashboardMessages);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Retrieved " + dashboardMessages.size() + " vet dashboard messages for " + userId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // GET /api/messages/farmer - Get messages for farmer dashboard
        private void handleGetFarmerMessages(HttpExchange exchange) throws IOException {
            try {
                // Authenticate user
                Map<String, Object> authResult = authenticateUser(exchange);
                if (authResult.containsKey("error")) {
                    sendError(exchange, 401, (String) authResult.get("error"));
                    return;
                }
                
                String userId = (String) authResult.get("userId");
                String userType = (String) authResult.get("userType");
                
                if (!"farmer".equals(userType)) {
                    sendError(exchange, 403, "Access denied");
                    return;
                }
                
                // Parse query parameters
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQueryParams(query);
                int limit = Integer.parseInt(params.getOrDefault("limit", "10"));
                
                // Get recent unread messages for the farmer
                MongoCollection<Document> messages = database.getCollection("messages");
                List<Document> messageList = messages.find(
                    and(
                        eq("receiverId", new ObjectId(userId)),
                        eq("receiverType", "farmer"),
                        eq("isRead", false),
                        eq("isDeleted", false)
                    )
                ).sort(new Document("createdAt", -1))
                .limit(limit)
                .into(new ArrayList<>());
                
                // Get sender info for each message
                List<Map<String, Object>> dashboardMessages = new ArrayList<>();
                for (Document msg : messageList) {
                    String senderId = msg.getObjectId("senderId").toString();
                    String senderType = msg.getString("senderType");
                    
                    MongoCollection<Document> senderCollection = senderType.equals("farmer") ? 
                        database.getCollection("farmers") : database.getCollection("vets");
                    Document senderInfo = senderCollection.find(eq("_id", new ObjectId(senderId))).first();
                    
                    if (senderInfo != null) {
                        Map<String, Object> dashboardMessage = new HashMap<>();
                        dashboardMessage.put("id", msg.getObjectId("_id").toString());
                        if (senderType.equals("vet")) {
                            dashboardMessage.put("vetName", senderInfo.getString("name"));
                            dashboardMessage.put("vetSpecialty", senderInfo.getString("specialty"));
                        } else {
                            dashboardMessage.put("farmerName", senderInfo.getString("name"));
                        }
                        dashboardMessage.put("content", msg.getString("content"));
                        dashboardMessage.put("timestamp", msg.getDate("createdAt"));
                        dashboardMessage.put("read", msg.getBoolean("isRead"));
                        dashboardMessage.put("priority", msg.getString("priority"));
                        
                        dashboardMessages.add(dashboardMessage);
                    }
                }
                
                String jsonResponse = gson.toJson(dashboardMessages);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Retrieved " + dashboardMessages.size() + " farmer dashboard messages for " + userId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // PUT /api/messages/{messageId}/read - Mark message as read
        private void handleMarkAsRead(HttpExchange exchange) throws IOException {
            try {
                // Authenticate user
                Map<String, Object> authResult = authenticateUser(exchange);
                if (authResult.containsKey("error")) {
                    sendError(exchange, 401, (String) authResult.get("error"));
                    return;
                }
                
                String userId = (String) authResult.get("userId");
                String userType = (String) authResult.get("userType");
                
                // Extract message ID from URL
                String path = exchange.getRequestURI().getPath();
                String messageId = path.split("/")[3];
                
                // Find and update message
                MongoCollection<Document> messages = database.getCollection("messages");
                Document message = messages.find(eq("_id", new ObjectId(messageId))).first();
                
                if (message == null) {
                    sendError(exchange, 404, "Message not found");
                    return;
                }
                
                // Check if user is the receiver of this message
                if (!message.getObjectId("receiverId").toString().equals(userId) || 
                    !message.getString("receiverType").equals(userType)) {
                    sendError(exchange, 403, "Access denied");
                    return;
                }
                
                // Mark as read
                messages.updateOne(
                    eq("_id", new ObjectId(messageId)),
                    new Document("$set", new Document("isRead", true).append("updatedAt", new Date()))
                );
                
                Map<String, String> response = new HashMap<>();
                response.put("message", "Message marked as read");
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Message " + messageId + " marked as read by " + userType + " " + userId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // DELETE /api/messages/{messageId} - Delete message
        private void handleDeleteMessage(HttpExchange exchange) throws IOException {
            try {
                // Authenticate user
                Map<String, Object> authResult = authenticateUser(exchange);
                if (authResult.containsKey("error")) {
                    sendError(exchange, 401, (String) authResult.get("error"));
                    return;
                }
                
                String userId = (String) authResult.get("userId");
                String userType = (String) authResult.get("userType");
                
                // Extract message ID from URL
                String path = exchange.getRequestURI().getPath();
                String messageId = path.split("/")[3];
                
                // Find message
                MongoCollection<Document> messages = database.getCollection("messages");
                Document message = messages.find(eq("_id", new ObjectId(messageId))).first();
                
                if (message == null) {
                    sendError(exchange, 404, "Message not found");
                    return;
                }
                
                // Check if user is the sender of this message
                if (!message.getObjectId("senderId").toString().equals(userId) || 
                    !message.getString("senderType").equals(userType)) {
                    sendError(exchange, 403, "Access denied. You can only delete your own messages.");
                    return;
                }
                
                // Soft delete (mark as deleted)
                messages.updateOne(
                    eq("_id", new ObjectId(messageId)),
                    new Document("$set", new Document("isDeleted", true).append("updatedAt", new Date()))
                );
                
                Map<String, String> response = new HashMap<>();
                response.put("message", "Message deleted successfully");
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Message " + messageId + " deleted by " + userType + " " + userId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // GET /api/messages/unread-count - Get unread message count
        private void handleGetUnreadCount(HttpExchange exchange) throws IOException {
            try {
                // Authenticate user
                Map<String, Object> authResult = authenticateUser(exchange);
                if (authResult.containsKey("error")) {
                    sendError(exchange, 401, (String) authResult.get("error"));
                    return;
                }
                
                String userId = (String) authResult.get("userId");
                String userType = (String) authResult.get("userType");
                
                // Count unread messages
                MongoCollection<Document> messages = database.getCollection("messages");
                long unreadCount = messages.countDocuments(
                    and(
                        eq("receiverId", new ObjectId(userId)),
                        eq("receiverType", userType),
                        eq("isRead", false),
                        eq("isDeleted", false)
                    )
                );
                
                Map<String, Object> response = new HashMap<>();
                response.put("unreadCount", unreadCount);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Retrieved unread count " + unreadCount + " for " + userType + " " + userId);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // GET /api/messages/search - Search messages
        private void handleSearchMessages(HttpExchange exchange) throws IOException {
            try {
                // Authenticate user
                Map<String, Object> authResult = authenticateUser(exchange);
                if (authResult.containsKey("error")) {
                    sendError(exchange, 401, (String) authResult.get("error"));
                    return;
                }
                
                String userId = (String) authResult.get("userId");
                String userType = (String) authResult.get("userType");
                
                // Parse query parameters
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQueryParams(query);
                String searchQuery = params.get("q");
                String type = params.get("type");
                int page = Integer.parseInt(params.getOrDefault("page", "1"));
                int limit = Integer.parseInt(params.getOrDefault("limit", "20"));
                
                if (searchQuery == null || searchQuery.trim().isEmpty()) {
                    sendError(exchange, 400, "Search query is required");
                    return;
                }
                
                // Build search criteria
                Document searchCriteria = new Document("$or", Arrays.asList(
                    new Document("senderId", new ObjectId(userId)).append("senderType", userType),
                    new Document("receiverId", new ObjectId(userId)).append("receiverType", userType)
                )).append("content", new Document("$regex", searchQuery).append("$options", "i"))
                .append("isDeleted", false);
                
                if (type != null && !type.trim().isEmpty()) {
                    searchCriteria.append("messageType", type);
                }
                
                // Search messages
                MongoCollection<Document> messages = database.getCollection("messages");
                List<Document> searchResults = messages.find(searchCriteria)
                    .sort(new Document("createdAt", -1))
                    .limit(limit)
                    .skip((page - 1) * limit)
                    .into(new ArrayList<>());
                
                long total = messages.countDocuments(searchCriteria);
                
                // Convert to response format
                List<Map<String, Object>> responseMessages = new ArrayList<>();
                for (Document msg : searchResults) {
                    Map<String, Object> messageData = new HashMap<>();
                    messageData.put("_id", msg.getObjectId("_id").toString());
                    messageData.put("senderId", msg.getObjectId("senderId").toString());
                    messageData.put("senderType", msg.getString("senderType"));
                    messageData.put("receiverId", msg.getObjectId("receiverId").toString());
                    messageData.put("receiverType", msg.getString("receiverType"));
                    messageData.put("content", msg.getString("content"));
                    messageData.put("messageType", msg.getString("messageType"));
                    messageData.put("isRead", msg.getBoolean("isRead"));
                    messageData.put("createdAt", msg.getDate("createdAt"));
                    responseMessages.add(messageData);
                }
                
                Map<String, Object> pagination = new HashMap<>();
                pagination.put("current", page);
                pagination.put("pages", Math.ceil((double) total / limit));
                pagination.put("total", total);
                
                Map<String, Object> response = new HashMap<>();
                response.put("messages", responseMessages);
                response.put("pagination", pagination);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("✅ Search returned " + responseMessages.size() + " messages for query: " + searchQuery);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
        
        // Helper method to authenticate user and get user info
        private Map<String, Object> authenticateUser(HttpExchange exchange) {
            Map<String, Object> result = new HashMap<>();
            
            try {
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    System.out.println("? Authentication failed: No token provided");
                    result.put("error", "Access denied. No token provided.");
                    return result;
                }
                
                String token = authHeader.substring(7);
                Claims claims = JwtUtil.validateToken(token);
                
                if (claims == null) {
                    System.out.println("? Authentication failed: Invalid or expired token");
                    result.put("error", "Invalid or expired token");
                    return result;
                }
                
                String userId = claims.getSubject();
                String userType = (String) claims.get("type");
                
                System.out.println("? JWT Authentication successful for " + userType + ": " + userId);
                
                if (userId == null || userType == null) {
                    System.out.println("? Authentication failed: Invalid token format - userId=" + userId + ", userType=" + userType);
                    result.put("error", "Invalid token format");
                    return result;
                }
                
                result.put("userId", userId);
                result.put("userType", userType);
                return result;
                
            } catch (Exception e) {
                System.out.println("? Authentication exception: " + e.getMessage());
                result.put("error", "Authentication failed");
                return result;
            }
        }
        
        // Helper method to parse query parameters
        private Map<String, String> parseQueryParams(String query) {
            Map<String, String> params = new HashMap<>();
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        try {
                            params.put(keyValue[0], java.net.URLDecoder.decode(keyValue[1], "UTF-8"));
                        } catch (Exception e) {
                            params.put(keyValue[0], keyValue[1]);
                        }
                    }
                }
            }
            return params;
        }
    }
    
    // Temporary handler to fix corrupted message data
    static class MessageDataFixHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                fixCorruptedMessageData(exchange);
            } else {
                sendError(exchange, 405, "Method not allowed");
            }
        }
        
        private void fixCorruptedMessageData(HttpExchange exchange) throws IOException {
            try {
                System.out.println("? Starting message data corruption fix...");
                
                MongoCollection<Document> messages = database.getCollection("messages");
                MongoCollection<Document> farmers = database.getCollection("farmers");
                MongoCollection<Document> vets = database.getCollection("vets");
                
                // Get all messages
                List<Document> allMessages = messages.find().into(new ArrayList<>());
                int fixedCount = 0;
                
                for (Document message : allMessages) {
                    String senderId = message.getObjectId("senderId").toString();
                    String currentSenderType = message.getString("senderType");
                    String receiverId = message.getObjectId("receiverId").toString();
                    String currentReceiverType = message.getString("receiverType");
                    
                    // Check actual sender type
                    String actualSenderType = null;
                    if (farmers.find(eq("_id", new ObjectId(senderId))).first() != null) {
                        actualSenderType = "farmer";
                    } else if (vets.find(eq("_id", new ObjectId(senderId))).first() != null) {
                        actualSenderType = "vet";
                    }
                    
                    // Check actual receiver type
                    String actualReceiverType = null;
                    if (farmers.find(eq("_id", new ObjectId(receiverId))).first() != null) {
                        actualReceiverType = "farmer";
                    } else if (vets.find(eq("_id", new ObjectId(receiverId))).first() != null) {
                        actualReceiverType = "vet";
                    }
                    
                    // Fix if incorrect
                    boolean needsUpdate = false;
                    Document updateDoc = new Document();
                    
                    if (actualSenderType != null && !actualSenderType.equals(currentSenderType)) {
                        updateDoc.append("senderType", actualSenderType);
                        needsUpdate = true;
                        System.out.println("? Fixing sender type for message " + message.getObjectId("_id") + 
                                         ": " + currentSenderType + " -> " + actualSenderType);
                    }
                    
                    if (actualReceiverType != null && !actualReceiverType.equals(currentReceiverType)) {
                        updateDoc.append("receiverType", actualReceiverType);
                        needsUpdate = true;
                        System.out.println("? Fixing receiver type for message " + message.getObjectId("_id") + 
                                         ": " + currentReceiverType + " -> " + actualReceiverType);
                    }
                    
                    if (needsUpdate) {
                        messages.updateOne(
                            eq("_id", message.getObjectId("_id")),
                            new Document("$set", updateDoc)
                        );
                        fixedCount++;
                    }
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Message data fix completed");
                response.put("fixedMessages", fixedCount);
                
                String jsonResponse = gson.toJson(response);
                exchange.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
                
                System.out.println("? Message data fix completed. Fixed " + fixedCount + " messages.");
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }
    }
}
