package com.livestock360.springbackend.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.livestock360.springbackend.dto.LoginRequest;
import com.livestock360.springbackend.dto.RefreshRequest;
import com.livestock360.springbackend.dto.RegisterRequest;
import com.livestock360.springbackend.model.Farmer;
import com.livestock360.springbackend.model.Vet;
import com.livestock360.springbackend.service.FarmerService;
import com.livestock360.springbackend.service.VetService;
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
    private final JwtUtil jwtUtil;
    private final Gson gson;

    @Autowired
    public AuthController(FarmerService farmerService, VetService vetService, JwtUtil jwtUtil) {
        this.farmerService = farmerService;
        this.vetService = vetService;
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
            } else {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Invalid user type. Must be 'farmer' or 'vet'");
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
            } else {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Invalid user type. Must be 'farmer' or 'vet'");
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
                    user.addProperty("userType", (String) claims.get("userType"));
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
            } else {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Invalid user type. Must be 'farmer' or 'vet'");
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
    public ResponseEntity<String> searchFarmers(@RequestParam String query) {
        try {
            System.out.println("Search farmers request received for query: " + query);

            List<Farmer> farmers = farmerService.searchFarmers(query);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Farmers retrieved successfully");
            response.add("farmers", gson.toJsonTree(farmers));
            
            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Search farmers error: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error during farmer search");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
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
            if (updatedVet.getLatitude() != null) vetData.addProperty("latitude", updatedVet.getLatitude());
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

    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }
}
