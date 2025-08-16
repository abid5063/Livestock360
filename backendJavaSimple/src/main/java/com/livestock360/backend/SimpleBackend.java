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
}
