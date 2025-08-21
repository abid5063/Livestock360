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
import org.bson.types.ObjectId;
import static com.mongodb.client.model.Filters.*;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;

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
}
