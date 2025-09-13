package com.livestock360.springbackend.controller;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.livestock360.springbackend.dto.RegisterRequest;
import com.livestock360.springbackend.dto.LoginRequest;
import com.livestock360.springbackend.model.Vet;
import com.livestock360.springbackend.service.VetService;
import com.livestock360.springbackend.utils.JwtUtil;
import com.livestock360.springbackend.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/vets")
@CrossOrigin(origins = "*")
public class VetController {
    
    @Autowired
    private VetService vetService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private Gson gson = new Gson();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /**
     * POST /api/vets/register - Register a new vet
     * Matches exactly the SimpleBackend.VetHandler.handleVetRegister()
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerVet(@RequestBody RegisterRequest request) {
        try {
            System.out.println("🔧 Vet registration request received for: " + request.getEmail());
            
            String name = request.getName();
            String email = request.getEmail();
            String password = request.getPassword();
            String specialty = request.getSpecialty();
            String licenseNumber = request.getLicenseNumber();
            String phoneNo = request.getPhoneNo() != null ? request.getPhoneNo() : request.getPhone();
            String location = request.getLocation();
            
            // Validation - exactly like SimpleBackend
            if (name == null || email == null || password == null || specialty == null || licenseNumber == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "All required fields must be provided");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (password.length() < 6) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Password must be at least 6 characters");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (name.length() < 3) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Name must be at least 3 characters");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Check if vet already exists by email or license number
            Vet existingVetByEmail = vetService.findByEmail(email.toLowerCase().trim());
            if (existingVetByEmail != null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Email already registered");
                return ResponseEntity.badRequest().body(error);
            }
            
            Vet existingVetByLicense = vetService.findByLicenseNumber(licenseNumber.trim().toUpperCase());
            if (existingVetByLicense != null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "License number already registered");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Create new vet - exactly like SimpleBackend
            Vet vet = new Vet();
            vet.setName(name.trim());
            vet.setEmail(email.toLowerCase().trim());
            vet.setPassword(PasswordUtil.hashPassword(password));
            vet.setSpecialty(specialty.trim());
            vet.setSpecialization(specialty.trim()); // Support both fields
            vet.setLicenseNumber(licenseNumber.trim().toUpperCase());
            vet.setPhoneNo(phoneNo != null ? phoneNo.trim() : "");
            vet.setPhone(phoneNo != null ? phoneNo.trim() : ""); // Support both fields
            vet.setLocation(location != null ? location.trim() : "");
            vet.setIsVerified(false);
            vet.setIsActive(true);
            vet.setRating(0.0);
            vet.setTotalReviews(0);
            vet.setTotalAppointments(0);
            vet.setCompletedAppointments(0);
            vet.setCancelledAppointments(0);
            vet.setUserType("vet");
            String currentDate = dateFormat.format(new Date());
            vet.setRegistrationDate(currentDate);
            vet.setCreatedAt(currentDate);
            vet.setUpdatedAt(currentDate);
            
            // Save vet
            Vet savedVet = vetService.save(vet);
            
            if (savedVet == null) {
                System.out.println("❌ Failed to save vet - checking for duplicate license number");
                
                // Check if it's a duplicate license number issue
                Vet existingVetWithLicense = vetService.findByLicenseNumber(licenseNumber);
                if (existingVetWithLicense != null) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "License number already exists");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }
                
                // Generic save failure
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Failed to register vet - internal server error");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            // Generate JWT token
            String token = JwtUtil.generateToken(savedVet.getId().toString(), savedVet.getEmail(), savedVet.getName(), "vet");
            
            // Prepare response - exactly like SimpleBackend
            Map<String, Object> vetData = new HashMap<>();
            vetData.put("_id", savedVet.getId());
            vetData.put("id", savedVet.getId());
            vetData.put("name", savedVet.getName());
            vetData.put("email", savedVet.getEmail());
            vetData.put("specialty", savedVet.getSpecialty());
            vetData.put("licenseNumber", savedVet.getLicenseNumber());
            vetData.put("phoneNo", savedVet.getPhoneNo());
            vetData.put("location", savedVet.getLocation());
            vetData.put("isVerified", savedVet.getIsVerified());
            vetData.put("userType", "vet");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Vet registered successfully");
            response.put("token", token);
            response.put("vet", vetData);
            
            System.out.println("✅ Vet registered successfully: " + email);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error during vet registration: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Internal server error during registration");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginVet(@RequestBody LoginRequest request) {
        try {
            System.out.println("Vet login request received");
            System.out.println("Email: " + request.getEmail());

            // Validate required fields
            if (request.getEmail() == null || request.getEmail().trim().isEmpty() ||
                request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Email and password are required");
                return ResponseEntity.badRequest().body(response.toString());
            }

            // Find vet by email
            Vet vet = vetService.findByEmail(request.getEmail());
            if (vet == null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Invalid email or password");
                return ResponseEntity.badRequest().body(response.toString());
            }

            // Verify password
            if (!PasswordUtil.verifyPassword(request.getPassword(), vet.getPassword())) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Invalid email or password");
                return ResponseEntity.badRequest().body(response.toString());
            }

            // Generate JWT token
            String token = JwtUtil.generateToken(vet.getId().toString(), vet.getEmail(), vet.getName(), "vet");

            // Create response
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Vet logged in successfully");
            response.addProperty("token", token);
            
            JsonObject vetJson = new JsonObject();
            vetJson.addProperty("id", vet.getId().toString());
            vetJson.addProperty("name", vet.getName());
            vetJson.addProperty("email", vet.getEmail());
            vetJson.addProperty("phone", vet.getPhone());
            vetJson.addProperty("location", vet.getLocation());
            vetJson.addProperty("specialty", vet.getSpecialty());
            vetJson.addProperty("licenseNumber", vet.getLicenseNumber());
            vetJson.addProperty("userType", vet.getUserType());
            
            response.add("vet", vetJson);

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Vet login error: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error during login");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<String> updateVetProfile(@RequestHeader("Authorization") String authHeader, @RequestBody RegisterRequest request) {
        try {
            System.out.println("Vet profile update request received");
            System.out.println("Request data: " + gson.toJson(request));

            // Extract JWT token from Authorization header
            String token = authHeader.replace("Bearer ", "");
            String vetId = JwtUtil.getUserIdFromToken(token);
            
            if (vetId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Access denied. Invalid or expired token.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(gson.toJson(response));
            }

            // Find vet by ID
            Vet vet = vetService.findById(vetId);
            if (vet == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Vet not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(response));
            }

            // Update fields if provided
            if (request.getName() != null && !request.getName().trim().isEmpty()) {
                vet.setName(request.getName().trim());
            }
            if (request.getSpecialty() != null && !request.getSpecialty().trim().isEmpty()) {
                vet.setSpecialty(request.getSpecialty().trim());
                vet.setSpecialization(request.getSpecialty().trim()); // Support both fields
            }
            if (request.getPhoneNo() != null && !request.getPhoneNo().trim().isEmpty()) {
                vet.setPhoneNo(request.getPhoneNo().trim());
                vet.setPhone(request.getPhoneNo().trim()); // Support both fields
            }
            if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
                vet.setLocation(request.getLocation().trim());
            }
            
            // Update timestamp
            String currentDate = dateFormat.format(new Date());
            vet.setUpdatedAt(currentDate);

            // Save updated vet
            Vet updatedVet = vetService.updateVet(vet);

            // Create response - exactly like SimpleBackend
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile updated successfully");
            
            Map<String, Object> vetData = new HashMap<>();
            vetData.put("_id", updatedVet.getId().toString());
            vetData.put("id", updatedVet.getId().toString());
            vetData.put("name", updatedVet.getName());
            vetData.put("email", updatedVet.getEmail());
            vetData.put("specialty", updatedVet.getSpecialty());
            vetData.put("phoneNo", updatedVet.getPhoneNo());
            vetData.put("location", updatedVet.getLocation());
            vetData.put("licenseNumber", updatedVet.getLicenseNumber());
            vetData.put("isVerified", updatedVet.getIsVerified());
            vetData.put("userType", "vet");
            
            response.put("vet", vetData);

            System.out.println("✅ Vet profile updated successfully: " + vetId);
            return ResponseEntity.ok(gson.toJson(response));

        } catch (Exception e) {
            System.out.println("❌ Error during vet profile update: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Internal server error during profile update");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(response));
        }
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<String> editVet(@PathVariable String id, @RequestBody RegisterRequest request) {
        try {
            System.out.println("Vet edit request received for ID: " + id);
            System.out.println("Request data: " + gson.toJson(request));

            // Find vet by ID
            Vet vet = vetService.findById(id);
            if (vet == null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Vet not found");
                return ResponseEntity.badRequest().body(response.toString());
            }

            // Update fields if provided
            if (request.getName() != null && !request.getName().trim().isEmpty()) {
                vet.setName(request.getName());
            }
            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                // Check if email is already taken by another vet
                Vet existingVet = vetService.findByEmail(request.getEmail());
                if (existingVet != null && !existingVet.getId().toString().equals(id)) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", false);
                    response.addProperty("message", "Email already registered");
                    return ResponseEntity.badRequest().body(response.toString());
                }
                vet.setEmail(request.getEmail());
            }
            
            // Handle phone field (support both phone and phoneNo)
            if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
                vet.setPhone(request.getPhone());
            } else if (request.getPhoneNo() != null && !request.getPhoneNo().trim().isEmpty()) {
                vet.setPhone(request.getPhoneNo());
            }
            
            if (request.getLocation() != null) {
                vet.setLocation(request.getLocation());
            }
            if (request.getSpecialty() != null) {
                vet.setSpecialty(request.getSpecialty());
            }
            if (request.getLicenseNumber() != null) {
                vet.setLicenseNumber(request.getLicenseNumber());
            }
            if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                vet.setPassword(PasswordUtil.hashPassword(request.getPassword()));
            }

            // Save updated vet
            Vet updatedVet = vetService.updateVet(vet);

            // Create response
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Vet profile updated successfully");
            
            JsonObject vetJson = new JsonObject();
            vetJson.addProperty("id", updatedVet.getId().toString());
            vetJson.addProperty("name", updatedVet.getName());
            vetJson.addProperty("email", updatedVet.getEmail());
            vetJson.addProperty("phone", updatedVet.getPhone());
            vetJson.addProperty("location", updatedVet.getLocation());
            vetJson.addProperty("specialty", updatedVet.getSpecialty());
            vetJson.addProperty("licenseNumber", updatedVet.getLicenseNumber());
            vetJson.addProperty("userType", updatedVet.getUserType());
            
            response.add("vet", vetJson);

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Vet edit error: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error during update");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchVets(
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String search) {
        try {
            System.out.println("Vet search request received. Specialty: " + specialty + 
                             ", Location: " + location + ", Search: " + search);

            // Use the enhanced search method in VetService
            List<Vet> vets = vetService.searchVets(search, location, specialty);

            // Convert to response format (limited fields for search, exactly like SimpleBackend)
            List<Map<String, Object>> vetResults = new ArrayList<>();
            for (Vet vet : vets) {
                Map<String, Object> vetData = new HashMap<>();
                vetData.put("_id", vet.getId().toString());
                vetData.put("name", vet.getName());
                vetData.put("specialty", vet.getSpecialty());
                vetData.put("location", vet.getLocation());
                
                // Handle rating safely (can be null)
                Double rating = vet.getRating();
                vetData.put("rating", rating != null ? rating : 0.0);
                
                // Handle totalReviews safely (can be null)
                Integer totalReviews = vet.getTotalReviews();
                vetData.put("totalReviews", totalReviews != null ? totalReviews : 0);
                
                // Handle consultationFee safely (can be null)
                Double consultationFee = vet.getConsultationFee();
                vetData.put("consultationFee", consultationFee != null ? consultationFee : 0.0);
                
                // Handle bio safely (can be null)
                String bio = vet.getBio();
                vetData.put("bio", bio != null ? bio : "");
                
                vetData.put("profileImage", vet.getProfileImage());
                
                vetResults.add(vetData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("vets", vetResults);

            System.out.println("✅ Retrieved " + vetResults.size() + " vets for search");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("Vet search error: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteVet(@PathVariable String id) {
        try {
            System.out.println("Vet delete request received for ID: " + id);

            // Find vet by ID
            Vet vet = vetService.findById(id);
            if (vet == null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Vet not found");
                return ResponseEntity.badRequest().body(response.toString());
            }

            // Delete vet
            vetService.deleteVet(id);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Vet profile deleted successfully");

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Vet delete error: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Internal server error during deletion");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }
}