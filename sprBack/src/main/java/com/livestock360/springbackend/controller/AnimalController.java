package com.livestock360.springbackend.controller;

import com.livestock360.springbackend.model.Animal;
import com.livestock360.springbackend.model.Farmer;
import com.livestock360.springbackend.service.AnimalService;
import com.livestock360.springbackend.service.CloudinaryService;
import com.livestock360.springbackend.service.FarmerService;
import com.livestock360.springbackend.service.TaskService;
import com.livestock360.springbackend.service.VaccineService;
import com.livestock360.springbackend.utils.JwtUtil;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/animals")
@CrossOrigin(origins = "*")
public class AnimalController {

    @Autowired
    private AnimalService animalService;
    
    @Autowired
    private FarmerService farmerService;
    
    @Autowired
    private CloudinaryService cloudinaryService;
    
    @Autowired
    private VaccineService vaccineService;
    
    @Autowired
    private TaskService taskService;

    /**
     * GET /api/animals - Get all animals for authenticated farmer
     * Matches exactly the SimpleBackend.AnimalHandler.handleGetAnimals()
     */
    @GetMapping
    public ResponseEntity<?> getAnimals(@RequestHeader("Authorization") String authHeader) {
        try {
            // Authenticate farmer
            String farmerId = authenticateRequest(authHeader);
            if (farmerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "Access denied. No token provided."));
            }
            
            // Get farmer to verify existence
            Farmer farmer = farmerService.findById(farmerId);
            if (farmer == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "Farmer not found"));
            }
            
            // Get animals for this farmer
            List<Animal> animals = animalService.findByFarmerId(farmerId);
            
            // REVERTED: Back to manual HashMap construction but optimized
            List<Map<String, Object>> response = new ArrayList<>(animals.size());
            for (Animal animal : animals) {
                Map<String, Object> animalData = new HashMap<>(8); // Pre-sized HashMap
                animalData.put("_id", animal.getId().toString());
                animalData.put("name", animal.getName());
                animalData.put("type", animal.getType());
                animalData.put("breed", animal.getBreed());
                animalData.put("age", animal.getAge());
                animalData.put("gender", animal.getGender());
                animalData.put("details", animal.getDetails());
                animalData.put("photo_url", animal.getPhoto_url());
                response.add(animalData);
            }
            
            System.out.println("✅ Retrieved " + animals.size() + " animals for farmer: " + farmer.getName());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error getting animals: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("message", "Failed to fetch animals"));
        }
    }

    /**
     * POST /api/animals - Create a new animal
     * Matches exactly the SimpleBackend.AnimalHandler.handleCreateAnimal()
     */
    @PostMapping
    public ResponseEntity<?> createAnimal(@RequestHeader("Authorization") String authHeader,
                                        @RequestBody Map<String, Object> request) {
        try {
            System.out.println("🐄 Create animal request received");
            
            // Authenticate farmer
            String farmerId = authenticateRequest(authHeader);
            if (farmerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "Access denied. No token provided."));
            }
            
            // Get farmer to verify existence
            Farmer farmer = farmerService.findById(farmerId);
            if (farmer == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "Farmer not found"));
            }
            
            // Extract request data
            String name = (String) request.get("name");
            String type = (String) request.get("type");
            String breed = (String) request.get("breed");
            Object ageObj = request.get("age");
            String gender = (String) request.get("gender");
            String details = (String) request.get("details");
            String image = (String) request.get("image");
            
            // Validation (exactly like SimpleBackend)
            if (name == null || type == null || breed == null || ageObj == null || gender == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "Please provide name, type, breed, age and gender"));
            }
            
            // Convert age to integer
            int age;
            try {
                if (ageObj instanceof Double) {
                    age = ((Double) ageObj).intValue();
                } else if (ageObj instanceof Integer) {
                    age = (Integer) ageObj;
                } else {
                    age = Integer.parseInt(ageObj.toString());
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "Invalid age format"));
            }
            
            // Handle image upload if provided - replicating Node.js logic
            String photo_url = "";
            if (image != null && !image.trim().isEmpty()) {
                System.out.println("typeof image: " + image.getClass().getSimpleName());
                System.out.println("image starts with: " + (image.length() > 30 ? image.substring(0, 30) : image));
                try {
                    photo_url = cloudinaryService.uploadImage(image);
                } catch (Exception uploadError) {
                    System.err.println("❌ Error uploading image to Cloudinary: " + uploadError.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Collections.singletonMap("message", "Failed to upload image"));
                }
            }
            
            // Create animal
            Animal animal = new Animal();
            animal.setName(name);
            animal.setType(type);
            animal.setBreed(breed);
            animal.setAge(age);
            animal.setGender(gender);
            animal.setDetails(details != null ? details : "");
            animal.setPhoto_url(photo_url);
            animal.setFarmer(new ObjectId(farmerId));
            animal.setCreatedAt(new Date());
            
            // Save animal
            Animal savedAnimal = animalService.save(animal);
            if (savedAnimal == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Failed to create animal"));
            }
            
            // Response (exactly like SimpleBackend)
            Map<String, Object> response = new HashMap<>();
            response.put("_id", savedAnimal.getId().toString());
            response.put("name", savedAnimal.getName());
            response.put("type", savedAnimal.getType());
            response.put("breed", savedAnimal.getBreed());
            response.put("age", savedAnimal.getAge());
            response.put("gender", savedAnimal.getGender());
            response.put("photo_url", savedAnimal.getPhoto_url());
            
            Map<String, Object> farmerData = new HashMap<>();
            farmerData.put("_id", farmerId);
            farmerData.put("name", farmer.getName());
            response.put("farmer", farmerData);
            
            System.out.println("✅ Animal created: " + name + " for farmer " + farmer.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error creating animal: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("message", "Failed to create animal"));
        }
    }

    /**
     * GET /api/animals/{id} - Get specific animal by ID
     * Matches exactly the SimpleBackend.AnimalHandler.handleGetAnimal()
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAnimal(@RequestHeader("Authorization") String authHeader,
                                     @PathVariable String id) {
        try {
            System.out.println("🐄 Get animal request received for ID: " + id);
            
            // Authenticate farmer
            String farmerId = authenticateRequest(authHeader);
            if (farmerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "Access denied. No token provided."));
            }
            
            // Get animal (ensure it belongs to the authenticated farmer)
            Animal animal = animalService.findByFarmerAndId(farmerId, id);
            if (animal == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("message", "Animal not found"));
            }
            
            // Response (exactly like SimpleBackend)
            Map<String, Object> response = new HashMap<>();
            response.put("_id", animal.getId().toString());
            response.put("name", animal.getName());
            response.put("type", animal.getType());
            response.put("breed", animal.getBreed());
            response.put("age", animal.getAge());
            response.put("gender", animal.getGender());
            response.put("details", animal.getDetails());
            response.put("photo_url", animal.getPhoto_url());
            response.put("createdAt", animal.getCreatedAt());
            
            System.out.println("✅ Animal retrieved: " + animal.getName());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error getting animal: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("message", "Failed to fetch animal"));
        }
    }

    /**
     * PUT /api/animals/{id} - Update specific animal
     * Matches exactly the SimpleBackend.AnimalHandler.handleUpdateAnimal()
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAnimal(@RequestHeader("Authorization") String authHeader,
                                        @PathVariable String id,
                                        @RequestBody Map<String, Object> updates) {
        try {
            System.out.println("📝 Update animal request received for ID: " + id);
            
            // Authenticate farmer
            String farmerId = authenticateRequest(authHeader);
            if (farmerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "Access denied. No token provided."));
            }
            
            // Get existing animal (ensure it belongs to the authenticated farmer)
            Animal animal = animalService.findByFarmerAndId(farmerId, id);
            if (animal == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("message", "Animal not found"));
            }
            
            // Update fields (exactly like SimpleBackend)
            if (updates.get("name") != null) animal.setName((String) updates.get("name"));
            if (updates.get("type") != null) animal.setType((String) updates.get("type"));
            if (updates.get("breed") != null) animal.setBreed((String) updates.get("breed"));
            if (updates.get("age") != null) {
                Object ageObj = updates.get("age");
                try {
                    int age = (ageObj instanceof Double) ? ((Double) ageObj).intValue() : (Integer) ageObj;
                    animal.setAge(age);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.singletonMap("message", "Invalid age format"));
                }
            }
            if (updates.get("gender") != null) animal.setGender((String) updates.get("gender"));
            if (updates.get("details") != null) animal.setDetails((String) updates.get("details"));
            if (updates.get("photo_url") != null) animal.setPhoto_url((String) updates.get("photo_url"));
            
            // Save updated animal
            Animal updatedAnimal = animalService.updateAnimal(animal);
            if (updatedAnimal == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Failed to update animal"));
            }
            
            // Response (exactly like SimpleBackend)
            Map<String, Object> response = new HashMap<>();
            response.put("_id", updatedAnimal.getId().toString());
            response.put("name", updatedAnimal.getName());
            response.put("type", updatedAnimal.getType());
            response.put("breed", updatedAnimal.getBreed());
            response.put("age", updatedAnimal.getAge());
            response.put("gender", updatedAnimal.getGender());
            response.put("details", updatedAnimal.getDetails());
            response.put("photo_url", updatedAnimal.getPhoto_url());
            response.put("createdAt", updatedAnimal.getCreatedAt());
            
            System.out.println("✅ Animal updated: " + updatedAnimal.getName());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error updating animal: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("message", "Failed to update animal"));
        }
    }

    /**
     * DELETE /api/animals/{id} - Delete specific animal
     * Matches exactly the SimpleBackend.AnimalHandler.handleDeleteAnimal()
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAnimal(@RequestHeader("Authorization") String authHeader,
                                        @PathVariable String id) {
        try {
            System.out.println("🗑️ Delete animal request received for ID: " + id);
            
            // Authenticate farmer
            String farmerId = authenticateRequest(authHeader);
            if (farmerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "Access denied. No token provided."));
            }
            
            // 1. Find the animal first (replicating Node.js logic)
            Animal animal = animalService.findByFarmerAndId(farmerId, id);
            if (animal == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("message", "Animal not found in your farm"));
            }
            
            // 2. Delete all vaccine records for this animal (replicating Node.js logic)
            vaccineService.deleteByAnimalAndFarmer(id, farmerId);
            
            // 3. Delete all task records for this animal (replicating Node.js logic)
            taskService.deleteByAnimalAndFarmer(id, farmerId);
            
            // 4. Delete image from cloudinary if needed (replicating Node.js logic)
            if (animal.getPhoto_url() != null && animal.getPhoto_url().contains("cloudinary")) {
                try {
                    cloudinaryService.deleteImage(animal.getPhoto_url());
                } catch (Exception deleteError) {
                    System.out.println("Error deleting image from cloudinary: " + deleteError.getMessage());
                    // Continue with animal deletion even if image deletion fails
                }
            }
            
            // 5. Delete the animal from the database (replicating Node.js logic)
            boolean deleted = animalService.deleteByFarmerAndId(farmerId, id);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Failed to delete animal"));
            }
            
            // Response (replicating Node.js message)
            Map<String, String> response = new HashMap<>();
            response.put("message", "Animal removed successfully");
            
            System.out.println("✅ Animal deleted: " + animal.getName());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error deleting animal: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("message", "Failed to delete animal"));
        }
    }

    /**
     * Helper method to authenticate request using JWT token
     * Exactly like SimpleBackend.authenticateRequest()
     */
    private String authenticateRequest(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        
        // Validate JWT token
        if (JwtUtil.validateTokenStatic(token) == null) {
            System.err.println("❌ Invalid JWT token provided");
            return null;
        }
        
        // Check if token is expired
        if (JwtUtil.isTokenExpiredStatic(token)) {
            System.err.println("❌ JWT token has expired");
            return null;
        }
        
        String farmerId = JwtUtil.getUserIdFromToken(token);
        System.out.println("✅ JWT Authentication successful for farmer: " + farmerId);
        
        return farmerId;
    }
}