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

public class LivestockBackend {
    private static MongoDatabase database;
    private static Gson gson = new Gson();
    
    public static void main(String[] args) throws IOException {
        System.out.println("🚀 Starting Livestock360 Simple Java Backend...");
        
        // Connect to MongoDB Atlas
        try {
            String connectionString = "mongodb+srv://user1:user1@cluster0.lx8cqwh.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
            MongoClient mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase("livestock360");
            
            // Test connection
            database.runCommand(new Document("ping", 1));
            System.out.println("✅ Connected to MongoDB Atlas successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to connect to MongoDB Atlas: " + e.getMessage());
            return;
        }
        
        // Create HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(5000), 0);
        
        // Auth routes exactly like Node.js
        server.createContext("/api/auth/register", new RegisterHandler());
        server.createContext("/api/auth/login", new LoginHandler());
        
        // Default handler for other routes
        server.createContext("/", new DefaultHandler());
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("✅ Simple Java Backend running on http://localhost:5000");
        System.out.println("✅ Authentication endpoints available:");
        System.out.println("   POST /api/auth/register");
        System.out.println("   POST /api/auth/login");
        System.out.println("📱 Mobile app can now connect to Java backend!");
    }
    
    static class DefaultHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
            
            String response = "{\"message\":\"Livestock360 Java Backend is running\",\"status\":\"active\"}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
    
    static class RegisterHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            
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
                System.out.println("📥 Register request: " + body);
                
                // Parse JSON
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
                
                // Hash password
                String hashedPassword = hashPassword(password);
                
                // Generate profile image
                String profileImage = "https://ui-avatars.com/api/?name=" + 
                    java.net.URLEncoder.encode(name, StandardCharsets.UTF_8) + 
                    "&background=random&color=fff&size=256";
                
                // Create farmer document (exactly like Node.js)
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
                System.out.println("✅ Farmer saved to database: " + farmer.getObjectId("_id"));
                
                // Generate token
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
                
                System.out.println("✅ Farmer registered successfully: " + name + " (" + email + ")");
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }
    
    static class LoginHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            
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
                System.out.println("📥 Login request: " + body);
                
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
                
                System.out.println("✅ Farmer logged in successfully: " + email);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }
    
    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().add("Content-Type", "application/json");
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
        
        System.out.println("❌ Error " + statusCode + ": " + message);
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
