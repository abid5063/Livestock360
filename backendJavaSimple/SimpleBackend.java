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

public class SimpleBackend {
    private static MongoDatabase database;
    private static Gson gson = new Gson();
    
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
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("✅ Simple Java Backend running on http://localhost:5000");
        System.out.println("✅ Connected to MongoDB Atlas");
        System.out.println("✅ Auth endpoints available:");
        System.out.println("   POST /api/auth/register");
        System.out.println("   POST /api/auth/login");
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
                Map<String, String> request = gson.fromJson(body, Map.class);
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
                
                // Generate simple token (farmer ID)
                String token = "token_" + farmer.getObjectId("_id").toString();
                
                // Response (exactly like Node.js)
                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                
                Map<String, Object> farmerData = new HashMap<>();
                farmerData.put("_id", farmer.getObjectId("_id").toString());
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
                Map<String, String> request = gson.fromJson(body, Map.class);
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
                
                // Generate token
                String token = "token_" + farmer.getObjectId("_id").toString();
                
                // Response (exactly like Node.js)
                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                
                Map<String, Object> farmerData = new HashMap<>();
                farmerData.put("id", farmer.getObjectId("_id").toString());
                farmerData.put("name", farmer.getString("name"));
                farmerData.put("email", farmer.getString("email"));
                farmerData.put("profileImage", farmer.getString("profileImage"));
                farmerData.put("phoneNo", farmer.getString("phoneNo"));
                farmerData.put("location", farmer.getString("location"));
                
                response.put("farmer", farmerData);
                
                String jsonResponse = gson.toJson(response);
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
