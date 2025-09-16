package com.livestock360.springbackend.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.livestock360.springbackend.dto.LoginRequest;
import com.livestock360.springbackend.dto.RefreshRequest;
import com.livestock360.springbackend.dto.RegisterRequest;
import com.livestock360.springbackend.model.Farmer;
import com.livestock360.springbackend.model.Vet;
import com.livestock360.springbackend.model.Customer;
import com.livestock360.springbackend.service.FarmerService;
import com.livestock360.springbackend.service.VetService;
import com.livestock360.springbackend.service.CustomerService;
import com.livestock360.springbackend.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final FarmerService farmerService;
    private final VetService vetService;
    private final CustomerService customerService;
    private final JwtUtil jwtUtil;
    private final Gson gson;

    @Autowired
    public AuthController(FarmerService farmerService, VetService vetService, CustomerService customerService, JwtUtil jwtUtil) {
        this.farmerService = farmerService;
        this.vetService = vetService;
        this.customerService = customerService;
        this.jwtUtil = jwtUtil;
        this.gson = new Gson();
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request,
                                         @RequestParam(required = false) String userType) {
        try {
            System.out.println("Registration request received for userType: " + userType);
            System.out.println("Request data: " + gson.toJson(request));

            // Validate required fields
            if (request.getName() == null || request.getName().trim().isEmpty() ||
                request.getEmail() == null || request.getEmail().trim().isEmpty() ||
                request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Name, email, and password are required");
                return ResponseEntity.badRequest().body(response.toString());
            }

            // Validate email format
            if (!isValidEmail(request.getEmail())) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Invalid email format");
                return ResponseEntity.badRequest().body(response.toString());
            }

            // Default userType to farmer if not specified
            if (userType == null || userType.trim().isEmpty()) {
                userType = "farmer";
            }

            if ("farmer".equals(userType)) {
                return registerFarmer(request);
            } else if ("vet".equals(userType)) {
                return registerVet(request);
            } else if ("customer".equals(userType)) {
                return registerCustomer(request);
            } else {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Invalid user type. Must be 'farmer', 'vet', or 'customer'");
                return ResponseEntity.badRequest().body(response.toString());
            }

        } catch (Exception e) {
            System.out.println("Registration error: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error during registration");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request,
                                      @RequestParam(required = false) String userType) {
        try {
            System.out.println("Login request received for userType: " + userType);
            System.out.println("Email: " + request.getEmail());

            // Validate required fields
            if (request.getEmail() == null || request.getEmail().trim().isEmpty() ||
                request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Email and password are required");
                return ResponseEntity.badRequest().body(response.toString());
            }

            // Default userType to farmer if not specified
            if (userType == null || userType.trim().isEmpty()) {
                userType = "farmer";
            }

            if ("farmer".equals(userType)) {
                return loginFarmer(request);
            } else if ("vet".equals(userType)) {
                return loginVet(request);
            } else if ("customer".equals(userType)) {
                return loginCustomer(request);
            } else {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Invalid user type. Must be 'farmer', 'vet', or 'customer'");
                return ResponseEntity.badRequest().body(response.toString());
            }

        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error during login");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshToken(@RequestBody RefreshRequest request) {
        try {
            System.out.println("Token refresh request received");

            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Token is required");
                return ResponseEntity.badRequest().body(response.toString());
            }

            String refreshedToken = jwtUtil.refreshTokenIfNeeded(request.getToken());

            if (refreshedToken != null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("message", "Token refreshed successfully");
                response.addProperty("token", refreshedToken);
                
                // Extract user info from token
                Claims claims = jwtUtil.extractClaims(refreshedToken);
                if (claims != null) {
                    JsonObject user = new JsonObject();
                    user.addProperty("id", claims.getSubject());
                    user.addProperty("name", (String) claims.get("name"));
                    user.addProperty("email", (String) claims.get("email"));
                    user.addProperty("userType", (String) claims.get("type"));
                    response.add("user", user);
                }
                
                return ResponseEntity.ok(response.toString());
            } else {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Invalid or expired token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
            }

        } catch (Exception e) {
            System.out.println("Token refresh error: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error during token refresh");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<String> editUser(@PathVariable String id,
                                         @RequestBody RegisterRequest request,
                                         @RequestParam(required = false) String userType) {
        try {
            System.out.println("Edit user request received for ID: " + id + ", userType: " + userType);

            // Default userType to farmer if not specified
            if (userType == null || userType.trim().isEmpty()) {
                userType = "farmer";
            }

            if ("farmer".equals(userType)) {
                return editFarmer(id, request);
            } else if ("vet".equals(userType)) {
                return editVet(id, request);
            } else if ("customer".equals(userType)) {
                return editCustomer(id, request);
            } else {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Invalid user type. Must be 'farmer', 'vet', or 'customer'");
                return ResponseEntity.badRequest().body(response.toString());
            }

        } catch (Exception e) {
            System.out.println("Edit user error: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error during user edit");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(@RequestBody Map<String, Object> updates,
                                              @RequestHeader("Authorization") String authHeader) {
        try {
            System.out.println("Profile update request received");
            System.out.println("Updates: " + gson.toJson(updates));

            // Extract token from Authorization header
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Authorization header missing or invalid");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
            }

            String token = authHeader.substring(7); // Remove "Bearer " prefix
            Claims claims = jwtUtil.extractClaims(token);
            
            if (claims == null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Invalid or expired token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
            }

            String userId = claims.getSubject();
            String userType = (String) claims.get("type");
            
            System.out.println("User ID: " + userId + ", User Type: " + userType);

            if ("customer".equals(userType)) {
                return updateCustomerProfile(userId, updates);
            } else if ("farmer".equals(userType)) {
                return updateFarmerProfile(userId, updates);
            } else if ("vet".equals(userType)) {
                return updateVetProfile(userId, updates);
            } else {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Invalid user type in token");
                return ResponseEntity.badRequest().body(response.toString());
            }

        } catch (Exception e) {
            System.out.println("Profile update error: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error during profile update");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable String id,
                                           @RequestParam(required = false) String userType) {
        try {
            System.out.println("Delete user request received for ID: " + id + ", userType: " + userType);

            // Default userType to farmer if not specified
            if (userType == null || userType.trim().isEmpty()) {
                userType = "farmer";
            }

            boolean deleted = false;
            if ("farmer".equals(userType)) {
                deleted = farmerService.deleteById(id);
            } else if ("vet".equals(userType)) {
                deleted = vetService.deleteById(id);
            } else {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Invalid user type. Must be 'farmer' or 'vet'");
                return ResponseEntity.badRequest().body(response.toString());
            }

            JsonObject response = new JsonObject();
            if (deleted) {
                response.addProperty("success", true);
                response.addProperty("message", "User deleted successfully");
                return ResponseEntity.ok(response.toString());
            } else {
                response.addProperty("success", false);
                response.addProperty("message", "User not found or could not be deleted");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response.toString());
            }

        } catch (Exception e) {
            System.out.println("Delete user error: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error during user deletion");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    @GetMapping("/farmers/search")
    public ResponseEntity<String> searchFarmers(@RequestParam(required = false, defaultValue = "") String search) {
        try {
            System.out.println("Search farmers request received for search term: '" + search + "'");

            List<Map<String, Object>> farmers = farmerService.searchFarmersWithProjection(search);
            
            // Response format exactly like SimpleBackend
            Map<String, Object> response = new HashMap<>();
            response.put("farmers", farmers);
            
            return ResponseEntity.ok(gson.toJson(response));

        } catch (Exception e) {
            System.out.println("Search farmers error: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(errorResponse));
        }
    }

    @GetMapping("/farmers")
    public ResponseEntity<String> getAllFarmers() {
        try {
            System.out.println("Get all farmers request received");

            List<Farmer> farmers = farmerService.getAllFarmers();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Farmers retrieved successfully");
            response.add("farmers", gson.toJsonTree(farmers));
            
            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Get all farmers error: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error during farmers retrieval");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    // Private helper methods
    
    private ResponseEntity<String> registerFarmer(RegisterRequest request) {
        try {
            System.out.println("Starting farmer registration process...");
            System.out.println("Email: " + request.getEmail() + ", Name: " + request.getName());
            
            // Check if farmer already exists
            Farmer existingFarmer = farmerService.findByEmail(request.getEmail());
            System.out.println("Existing farmer check: " + (existingFarmer != null ? "Found" : "Not found"));
            
            if (existingFarmer != null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Farmer with this email already exists");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response.toString());
            }

            // Create new farmer
            System.out.println("Creating new farmer...");
            // Handle both phone and phoneNo fields for compatibility
            String phoneValue = request.getPhone() != null ? request.getPhone() : 
                               (request.getPhoneNo() != null ? request.getPhoneNo() : "");
            
            Farmer farmer = farmerService.createFarmer(
                request.getName(),
                request.getEmail(),
                request.getPassword(),
                phoneValue,
                request.getLocation() != null ? request.getLocation() : "",
                request.getAddress() != null ? request.getAddress() : "",
                request.getProfilePicture() != null ? request.getProfilePicture() : ""
            );
            
            System.out.println("Farmer creation result: " + (farmer != null ? "Success" : "Failed"));

            if (farmer != null) {
                System.out.println("Generating JWT token...");
                // Generate JWT token
                String token = jwtUtil.generateToken(
                    farmer.getId().toString(),
                    farmer.getName(),
                    farmer.getEmail()
                );
                
                System.out.println("Token generated successfully. Farmer ID: " + farmer.getId().toString());

                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("message", "Farmer registered successfully");
                response.addProperty("token", token);
                
                JsonObject farmerData = new JsonObject();
                farmerData.addProperty("id", farmer.getId().toString());
                farmerData.addProperty("name", farmer.getName());
                farmerData.addProperty("email", farmer.getEmail());
                farmerData.addProperty("phone", farmer.getPhone());
                farmerData.addProperty("location", farmer.getLocation());
                farmerData.addProperty("address", farmer.getAddress());
                farmerData.addProperty("profilePicture", farmer.getProfilePicture());
                farmerData.addProperty("userType", "farmer");
                response.add("farmer", farmerData);
                
                return ResponseEntity.status(HttpStatus.CREATED).body(response.toString());
            } else {
                System.out.println("Farmer creation failed - farmerService.createFarmer returned null");
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Failed to register farmer");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
            }
        } catch (Exception e) {
            System.out.println("Exception in registerFarmer: " + e.getMessage());
            e.printStackTrace();
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Failed to register farmer - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    private ResponseEntity<String> registerVet(RegisterRequest request) {
        // Check if vet already exists
        if (vetService.findByEmail(request.getEmail()) != null) {
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Vet with this email already exists");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response.toString());
        }

        // Create new vet
        // Handle both phone and phoneNo fields for compatibility
        String phoneValue = request.getPhone() != null ? request.getPhone() : 
                           (request.getPhoneNo() != null ? request.getPhoneNo() : "");
        
        Vet vet = vetService.createVet(
            request.getName(),
            request.getEmail(),
            request.getPassword(),
            phoneValue,
            request.getLocation() != null ? request.getLocation() : "",
            request.getAddress() != null ? request.getAddress() : "",
            request.getProfilePicture() != null ? request.getProfilePicture() : "",
            request.getSpecialization() != null ? request.getSpecialization() : "",
            request.getLatitude(),
            request.getLongitude()
        );

        if (vet != null) {
            // Generate JWT token
            String token = jwtUtil.generateTokenForVet(
                vet.getId().toString(),
                vet.getName(),
                vet.getEmail()
            );

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Vet registered successfully");
            response.addProperty("token", token);
            
            JsonObject vetData = new JsonObject();
            vetData.addProperty("id", vet.getId().toString());
            vetData.addProperty("name", vet.getName());
            vetData.addProperty("email", vet.getEmail());
            vetData.addProperty("phone", vet.getPhone());
            vetData.addProperty("location", vet.getLocation());
            vetData.addProperty("address", vet.getAddress());
            vetData.addProperty("profilePicture", vet.getProfilePicture());
            vetData.addProperty("specialization", vet.getSpecialization());
            if (vet.getLatitude() != null) vetData.addProperty("latitude", vet.getLatitude());
            if (vet.getLongitude() != null) vetData.addProperty("longitude", vet.getLongitude());
            vetData.addProperty("userType", "vet");
            response.add("vet", vetData);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response.toString());
        } else {
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Failed to register vet");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    private ResponseEntity<String> registerCustomer(RegisterRequest request) {
        try {
            System.out.println("Starting customer registration process...");
            System.out.println("Email: " + request.getEmail() + ", Name: " + request.getName());
            
            // Check if customer already exists
            Customer existingCustomer = customerService.findByEmail(request.getEmail());
            System.out.println("Existing customer check: " + (existingCustomer != null ? "Found" : "Not found"));
            
            if (existingCustomer != null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Customer with this email already exists");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response.toString());
            }

            // Create new customer
            System.out.println("Creating new customer...");
            Customer customer = new Customer();
            customer.setName(request.getName());
            customer.setEmail(request.getEmail());
            customer.setPhone(request.getPhone() != null ? request.getPhone() : "");
            customer.setLocation(request.getLocation() != null ? request.getLocation() : "");
            customer.setAddress(request.getAddress() != null ? request.getAddress() : "");
            customer.setProfilePicture(request.getProfilePicture() != null ? request.getProfilePicture() : "");
            
            // Set password with salt
            customer = customerService.setPassword(customer, request.getPassword());
            
            // Set default values for marketplace fields
            customer.setCustomerType("individual");
            customer.setMaxBudget(0.0);
            customer.setTotalPurchases(0);
            customer.setTotalSpent(0.0);
            customer.setIsVerified(false);
            
            // Save customer
            customer = customerService.save(customer);
            
            System.out.println("Customer creation result: " + (customer != null ? "Success" : "Failed"));

            if (customer != null) {
                System.out.println("Generating JWT token...");
                // Generate JWT token
                String token = jwtUtil.generateTokenForCustomer(
                    customer.getId().toString(),
                    customer.getName(),
                    customer.getEmail()
                );
                
                System.out.println("Token generated successfully. Customer ID: " + customer.getId().toString());

                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("message", "Customer registered successfully");
                response.addProperty("token", token);
                
                JsonObject customerData = new JsonObject();
                customerData.addProperty("id", customer.getId().toString());
                customerData.addProperty("name", customer.getName());
                customerData.addProperty("email", customer.getEmail());
                customerData.addProperty("phone", customer.getPhone());
                customerData.addProperty("location", customer.getLocation());
                customerData.addProperty("address", customer.getAddress());
                customerData.addProperty("profilePicture", customer.getProfilePicture());
                customerData.addProperty("customerType", customer.getCustomerType());
                customerData.addProperty("maxBudget", customer.getMaxBudget());
                customerData.addProperty("isVerified", customer.getIsVerified());
                customerData.addProperty("userType", "customer");
                response.add("customer", customerData);
                
                return ResponseEntity.status(HttpStatus.CREATED).body(response.toString());
            } else {
                System.out.println("Customer creation failed - customerService.save returned null");
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Failed to register customer");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
            }
        } catch (Exception e) {
            System.out.println("Exception in registerCustomer: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error during customer registration");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    private ResponseEntity<String> loginFarmer(LoginRequest request) {
        Farmer farmer = farmerService.findByEmail(request.getEmail());
        
        if (farmer == null) {
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
        }

        if (!farmerService.verifyPassword(farmer, request.getPassword())) {
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(
            farmer.getId().toString(),
            farmer.getName(),
            farmer.getEmail()
        );

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Farmer logged in successfully");
        response.addProperty("token", token);
        
        JsonObject farmerData = new JsonObject();
        farmerData.addProperty("id", farmer.getId().toString());
        farmerData.addProperty("name", farmer.getName());
        farmerData.addProperty("email", farmer.getEmail());
        farmerData.addProperty("phone", farmer.getPhone());
        farmerData.addProperty("location", farmer.getLocation());
        farmerData.addProperty("address", farmer.getAddress());
        farmerData.addProperty("profilePicture", farmer.getProfilePicture());
        farmerData.addProperty("userType", "farmer");
        response.add("farmer", farmerData);
        
        return ResponseEntity.ok(response.toString());
    }

    private ResponseEntity<String> loginVet(LoginRequest request) {
        Vet vet = vetService.findByEmail(request.getEmail());
        
        if (vet == null) {
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
        }

        if (!vetService.verifyPassword(vet, request.getPassword())) {
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
        }

        // Generate JWT token
        String token = jwtUtil.generateTokenForVet(
            vet.getId().toString(),
            vet.getName(),
            vet.getEmail()
        );

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Vet logged in successfully");
        response.addProperty("token", token);
        
        JsonObject vetData = new JsonObject();
        vetData.addProperty("id", vet.getId().toString());
        vetData.addProperty("name", vet.getName());
        vetData.addProperty("email", vet.getEmail());
        vetData.addProperty("phone", vet.getPhone());
        vetData.addProperty("location", vet.getLocation());
        vetData.addProperty("address", vet.getAddress());
        vetData.addProperty("profilePicture", vet.getProfilePicture());
        vetData.addProperty("specialization", vet.getSpecialization());
        if (vet.getLatitude() != null) vetData.addProperty("latitude", vet.getLatitude());
        if (vet.getLongitude() != null) vetData.addProperty("longitude", vet.getLongitude());
        vetData.addProperty("userType", "vet");
        response.add("vet", vetData);
        
        return ResponseEntity.ok(response.toString());
    }

    private ResponseEntity<String> loginCustomer(LoginRequest request) {
        Customer customer = customerService.findByEmail(request.getEmail());
        
        if (customer == null) {
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
        }

        if (!customerService.verifyPassword(customer, request.getPassword())) {
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
        }

        // Generate JWT token
        String token = jwtUtil.generateTokenForCustomer(
            customer.getId().toString(),
            customer.getName(),
            customer.getEmail()
        );
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Customer login successful");
        response.addProperty("token", token);
        
        JsonObject customerData = new JsonObject();
        customerData.addProperty("id", customer.getId().toString());
        customerData.addProperty("name", customer.getName());
        customerData.addProperty("email", customer.getEmail());
        customerData.addProperty("phone", customer.getPhone());
        customerData.addProperty("location", customer.getLocation());
        customerData.addProperty("address", customer.getAddress());
        customerData.addProperty("profilePicture", customer.getProfilePicture());
        customerData.addProperty("customerType", customer.getCustomerType());
        customerData.addProperty("maxBudget", customer.getMaxBudget());
        customerData.addProperty("totalPurchases", customer.getTotalPurchases());
        customerData.addProperty("totalSpent", customer.getTotalSpent());
        customerData.addProperty("isVerified", customer.getIsVerified());
        customerData.addProperty("userType", "customer");
        response.add("customer", customerData);
        
        return ResponseEntity.ok(response.toString());
    }

    private ResponseEntity<String> editFarmer(String id, RegisterRequest request) {
        Farmer farmer = farmerService.findById(id);
        
        if (farmer == null) {
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Farmer not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response.toString());
        }

        // Update farmer details
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            farmer.setName(request.getName());
        }
        // Handle both phone and phoneNo fields for compatibility
        String phoneValue = request.getPhone() != null ? request.getPhone() : request.getPhoneNo();
        if (phoneValue != null) {
            farmer.setPhone(phoneValue);
        }
        if (request.getLocation() != null) {
            farmer.setLocation(request.getLocation());
        }
        if (request.getAddress() != null) {
            farmer.setAddress(request.getAddress());
        }
        if (request.getProfilePicture() != null) {
            farmer.setProfilePicture(request.getProfilePicture());
        }

        Farmer updatedFarmer = farmerService.save(farmer);
        
        if (updatedFarmer != null) {
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Farmer updated successfully");
            
            JsonObject farmerData = new JsonObject();
            farmerData.addProperty("id", updatedFarmer.getId().toString());
            farmerData.addProperty("name", updatedFarmer.getName());
            farmerData.addProperty("email", updatedFarmer.getEmail());
            farmerData.addProperty("phone", updatedFarmer.getPhone());
            farmerData.addProperty("location", updatedFarmer.getLocation());
            farmerData.addProperty("address", updatedFarmer.getAddress());
            farmerData.addProperty("profilePicture", updatedFarmer.getProfilePicture());
            farmerData.addProperty("userType", "farmer");
            response.add("farmer", farmerData);
            
            return ResponseEntity.ok(response.toString());
        } else {
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Failed to update farmer");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    private ResponseEntity<String> editVet(String id, RegisterRequest request) {
        Vet vet = vetService.findById(id);
        
        if (vet == null) {
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Vet not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response.toString());
        }

        // Update vet details
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            vet.setName(request.getName());
        }
        // Handle both phone and phoneNo fields for compatibility
        String phoneValue = request.getPhone() != null ? request.getPhone() : request.getPhoneNo();
        if (phoneValue != null) {
            vet.setPhone(phoneValue);
        }
        if (request.getLocation() != null) {
            vet.setLocation(request.getLocation());
        }
        if (request.getAddress() != null) {
            vet.setAddress(request.getAddress());
        }
        if (request.getProfilePicture() != null) {
            vet.setProfilePicture(request.getProfilePicture());
        }
        if (request.getSpecialization() != null) {
            vet.setSpecialization(request.getSpecialization());
        }
        if (request.getLatitude() != null) {
            vet.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            vet.setLongitude(request.getLongitude());
        }

        Vet updatedVet = vetService.save(vet);
        
        if (updatedVet != null) {
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Vet updated successfully");
            
            JsonObject vetData = new JsonObject();
            vetData.addProperty("id", updatedVet.getId().toString());
            vetData.addProperty("name", updatedVet.getName());
            vetData.addProperty("email", updatedVet.getEmail());
            vetData.addProperty("phone", updatedVet.getPhone());
            vetData.addProperty("location", updatedVet.getLocation());
            vetData.addProperty("address", updatedVet.getAddress());
            vetData.addProperty("profilePicture", updatedVet.getProfilePicture());
            vetData.addProperty("specialization", updatedVet.getSpecialization());
            if (updatedVet.getLatitude() != null) vetData.addProperty("latitude", updatedVet.getLongitude());
            if (updatedVet.getLongitude() != null) vetData.addProperty("longitude", updatedVet.getLongitude());
            vetData.addProperty("userType", "vet");
            response.add("vet", vetData);
            
            return ResponseEntity.ok(response.toString());
        } else {
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Failed to update vet");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    private ResponseEntity<String> editCustomer(String id, RegisterRequest request) {
        Customer customer = customerService.findById(id);
        
        if (customer == null) {
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Customer not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response.toString());
        }

        // Update customer details
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            customer.setName(request.getName());
        }
        // Handle both phone and phoneNo fields for compatibility
        String phoneValue = request.getPhone() != null ? request.getPhone() : request.getPhoneNo();
        if (phoneValue != null) {
            customer.setPhone(phoneValue);
        }
        if (request.getLocation() != null) {
            customer.setLocation(request.getLocation());
        }
        if (request.getAddress() != null) {
            customer.setAddress(request.getAddress());
        }
        if (request.getProfilePicture() != null) {
            customer.setProfilePicture(request.getProfilePicture());
        }

        Customer updatedCustomer = customerService.save(customer);
        
        if (updatedCustomer != null) {
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Customer updated successfully");
            
            JsonObject customerData = new JsonObject();
            customerData.addProperty("id", updatedCustomer.getId().toString());
            customerData.addProperty("name", updatedCustomer.getName());
            customerData.addProperty("email", updatedCustomer.getEmail());
            customerData.addProperty("phone", updatedCustomer.getPhone());
            customerData.addProperty("location", updatedCustomer.getLocation());
            customerData.addProperty("address", updatedCustomer.getAddress());
            customerData.addProperty("profilePicture", updatedCustomer.getProfilePicture());
            customerData.addProperty("customerType", updatedCustomer.getCustomerType());
            customerData.addProperty("maxBudget", updatedCustomer.getMaxBudget());
            customerData.addProperty("totalPurchases", updatedCustomer.getTotalPurchases());
            customerData.addProperty("totalSpent", updatedCustomer.getTotalSpent());
            customerData.addProperty("isVerified", updatedCustomer.getIsVerified());
            customerData.addProperty("userType", "customer");
            response.add("customer", customerData);
            
            return ResponseEntity.ok(response.toString());
        } else {
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Failed to update customer");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    private ResponseEntity<String> updateCustomerProfile(String userId, Map<String, Object> updates) {
        Customer customer = customerService.findById(userId);
        
        if (customer == null) {
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Customer not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response.toString());
        }

        // Update customer details from the map
        if (updates.containsKey("name") && updates.get("name") != null) {
            customer.setName(updates.get("name").toString());
        }
        if (updates.containsKey("phone") && updates.get("phone") != null) {
            customer.setPhone(updates.get("phone").toString());
        }
        if (updates.containsKey("location") && updates.get("location") != null) {
            customer.setLocation(updates.get("location").toString());
        }
        if (updates.containsKey("address") && updates.get("address") != null) {
            customer.setAddress(updates.get("address").toString());
        }
        if (updates.containsKey("profilePicture") && updates.get("profilePicture") != null) {
            customer.setProfilePicture(updates.get("profilePicture").toString());
        }
        if (updates.containsKey("customerType") && updates.get("customerType") != null) {
            customer.setCustomerType(updates.get("customerType").toString());
        }
        if (updates.containsKey("maxBudget") && updates.get("maxBudget") != null) {
            Object budgetValue = updates.get("maxBudget");
            if (budgetValue instanceof Number) {
                customer.setMaxBudget(((Number) budgetValue).doubleValue());
            } else if (budgetValue instanceof String) {
                try {
                    customer.setMaxBudget(Double.parseDouble((String) budgetValue));
                } catch (NumberFormatException e) {
                    // Keep existing value if parsing fails
                }
            }
        }
        if (updates.containsKey("businessName") && updates.get("businessName") != null) {
            customer.setBusinessName(updates.get("businessName").toString());
        }
        if (updates.containsKey("businessLicense") && updates.get("businessLicense") != null) {
            customer.setBusinessLicense(updates.get("businessLicense").toString());
        }

        Customer updatedCustomer = customerService.save(customer);
        
        if (updatedCustomer != null) {
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Customer profile updated successfully");
            
            JsonObject customerData = new JsonObject();
            customerData.addProperty("id", updatedCustomer.getId().toString());
            customerData.addProperty("name", updatedCustomer.getName());
            customerData.addProperty("email", updatedCustomer.getEmail());
            customerData.addProperty("phone", updatedCustomer.getPhone());
            customerData.addProperty("location", updatedCustomer.getLocation());
            customerData.addProperty("address", updatedCustomer.getAddress());
            customerData.addProperty("profilePicture", updatedCustomer.getProfilePicture());
            customerData.addProperty("customerType", updatedCustomer.getCustomerType());
            customerData.addProperty("maxBudget", updatedCustomer.getMaxBudget());
            customerData.addProperty("totalPurchases", updatedCustomer.getTotalPurchases());
            customerData.addProperty("totalSpent", updatedCustomer.getTotalSpent());
            customerData.addProperty("isVerified", updatedCustomer.getIsVerified());
            if (updatedCustomer.getBusinessName() != null) {
                customerData.addProperty("businessName", updatedCustomer.getBusinessName());
            }
            if (updatedCustomer.getBusinessLicense() != null) {
                customerData.addProperty("businessLicense", updatedCustomer.getBusinessLicense());
            }
            customerData.addProperty("userType", "customer");
            response.add("customer", customerData);
            
            return ResponseEntity.ok(response.toString());
        } else {
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Failed to update customer profile");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    private ResponseEntity<String> updateFarmerProfile(String userId, Map<String, Object> updates) {
        // Similar implementation for farmer profile updates
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("message", "Farmer profile update not implemented yet");
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(response.toString());
    }

    private ResponseEntity<String> updateVetProfile(String userId, Map<String, Object> updates) {
        // Similar implementation for vet profile updates
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("message", "Vet profile update not implemented yet");
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(response.toString());
    }

    @GetMapping("/farmers/token-balance")
    public ResponseEntity<String> getTokenBalance(@RequestHeader("Authorization") String authHeader) {
        try {
            System.out.println("Token balance request received");

            // Extract token from Authorization header
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Authorization header missing or invalid");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
            }

            String token = authHeader.substring(7); // Remove "Bearer " prefix
            Claims claims = jwtUtil.extractClaims(token);
            
            if (claims == null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Invalid or expired token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
            }

            String userId = claims.getSubject();
            String userType = (String) claims.get("type");

            // Verify this is a farmer token
            if (!"farmer".equals(userType)) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "This endpoint is only for farmers");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response.toString());
            }

            // Find the farmer
            Farmer farmer = farmerService.findById(userId);
            if (farmer == null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Farmer not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response.toString());
            }

            // Return token balance
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("tokenBalance", farmer.getTokenCount() != null ? farmer.getTokenCount() : 0);
            response.addProperty("userId", userId);
            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Token balance error: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error during token balance retrieval");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    @PostMapping("/farmers/deduct-tokens")
    public ResponseEntity<String> deductTokens(@RequestBody Map<String, Object> request,
                                              @RequestHeader("Authorization") String authHeader) {
        try {
            System.out.println("Token deduction request received");
            System.out.println("Request: " + gson.toJson(request));

            // Extract token from Authorization header
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Authorization header missing or invalid");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
            }

            String token = authHeader.substring(7); // Remove "Bearer " prefix
            Claims claims = jwtUtil.extractClaims(token);
            
            if (claims == null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Invalid or expired token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
            }

            // Validate request parameters
            if (!request.containsKey("userId") || !request.containsKey("amount") || !request.containsKey("featureUsed")) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "userId, amount, and featureUsed are required");
                return ResponseEntity.badRequest().body(response.toString());
            }

            String userId = request.get("userId").toString();
            int amount = Integer.parseInt(request.get("amount").toString());
            String featureUsed = request.get("featureUsed").toString();

            // Verify the authenticated user is making the request for themselves
            String tokenUserId = claims.getSubject();
            if (!tokenUserId.equals(userId)) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Unauthorized to deduct tokens for this user");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response.toString());
            }

            // Find the farmer
            Farmer farmer = farmerService.findById(userId);
            if (farmer == null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Farmer not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response.toString());
            }

            // Check if farmer has enough tokens
            if (farmer.getTokenCount() < amount) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Insufficient tokens");
                response.addProperty("currentBalance", farmer.getTokenCount());
                response.addProperty("requiredAmount", amount);
                return ResponseEntity.badRequest().body(response.toString());
            }

            // Deduct tokens
            farmer.setTokenCount(farmer.getTokenCount() - amount);
            Farmer updatedFarmer = farmerService.save(farmer);

            if (updatedFarmer != null) {
                // Log the token usage
                System.out.println("Token deduction: User " + userId + " used " + amount + " tokens for " + featureUsed + ". New balance: " + updatedFarmer.getTokenCount());

                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("message", "Tokens deducted successfully");
                response.addProperty("newBalance", updatedFarmer.getTokenCount());
                response.addProperty("deductedAmount", amount);
                response.addProperty("featureUsed", featureUsed);
                return ResponseEntity.ok(response.toString());
            } else {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Failed to update token balance");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
            }

        } catch (NumberFormatException e) {
            System.out.println("Invalid amount format: " + e.getMessage());
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Invalid amount format");
            return ResponseEntity.badRequest().body(response.toString());
        } catch (Exception e) {
            System.out.println("Token deduction error: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error during token deduction");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }
}
