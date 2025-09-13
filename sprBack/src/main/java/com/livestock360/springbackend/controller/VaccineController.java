package com.livestock360.springbackend.controller;

import com.livestock360.springbackend.model.Vaccine;
import com.livestock360.springbackend.model.Animal;
import com.livestock360.springbackend.service.VaccineService;
import com.livestock360.springbackend.service.AnimalService;
import com.livestock360.springbackend.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/vaccines")
@CrossOrigin(origins = "*")
public class VaccineController {

    @Autowired
    private VaccineService vaccineService;
    
    @Autowired
    private AnimalService animalService;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * POST /api/vaccines - Create a new vaccine record
     * Matches exactly the SimpleBackend.VaccineHandler.handleCreateVaccine()
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createVaccine(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            // Authenticate
            String token = authHeader.substring(7); // Remove "Bearer "
            Claims claims = JwtUtil.validateTokenStatic(token);
            if (claims == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "Access denied. Invalid or expired token.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            String farmerId = JwtUtil.getUserIdFromToken(token);
            
            // Extract request data
            String vaccineName = (String) request.get("vaccine_name");
            String animalId = (String) request.get("animal_id");
            String vaccineDateStr = (String) request.get("vaccine_date");
            String notes = (String) request.get("notes");
            String nextDueDateStr = (String) request.get("next_due_date");
            
            // Validate required fields
            if (vaccineName == null || animalId == null || vaccineDateStr == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "Please provide vaccine name, animal ID, and vaccine date");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            // Verify animal exists and belongs to farmer
            Animal animal = animalService.findByFarmerAndId(farmerId, animalId);
            if (animal == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "Animal not found in your farm");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            // Parse dates
            Date vaccineDate = parseDate(vaccineDateStr);
            Date nextDueDate = null;
            if (nextDueDateStr != null && !nextDueDateStr.trim().isEmpty()) {
                nextDueDate = parseDate(nextDueDateStr);
            }
            
            // Create vaccine
            Vaccine vaccine = new Vaccine();
            vaccine.setVaccineName(vaccineName);
            vaccine.setAnimal(new ObjectId(animalId));
            vaccine.setAnimalName(animal.getName());
            vaccine.setVaccineDate(vaccineDate);
            vaccine.setNotes(notes != null ? notes : "");
            vaccine.setNextDueDate(nextDueDate);
            vaccine.setFarmer(new ObjectId(farmerId));
            vaccine.setCreatedAt(new Date());
            vaccine.setUpdatedAt(new Date());
            
            // Save vaccine
            Vaccine savedVaccine = vaccineService.save(vaccine);
            if (savedVaccine == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "Failed to create vaccine record");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }
            
            // Response with animal details
            Map<String, Object> response = createVaccineResponse(savedVaccine, animal);
            
            System.out.println("✅ Vaccine record created: " + vaccineName + " for " + animal.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to create vaccine record");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/vaccines - Get all vaccine records for farmer
     * Matches exactly the SimpleBackend.VaccineHandler.handleGetVaccines()
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getVaccines(
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            // Authenticate
            String token = authHeader.substring(7); // Remove "Bearer "
            Claims claims = JwtUtil.validateTokenStatic(token);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String farmerId = JwtUtil.getUserIdFromToken(token);
            
            // Get vaccines
            List<Vaccine> vaccines = vaccineService.findByFarmerId(farmerId);
            
            // Convert to response format with animal details
            List<Map<String, Object>> response = new ArrayList<>();
            for (Vaccine vaccine : vaccines) {
                Animal animal = animalService.findByFarmerAndId(farmerId, vaccine.getAnimal().toString());
                response.add(createVaccineResponse(vaccine, animal));
            }
            
            System.out.println("✅ Retrieved " + vaccines.size() + " vaccine records for farmer: " + farmerId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/vaccines/animal/{animalId} - Get vaccines for specific animal
     * Matches exactly the SimpleBackend.VaccineHandler.handleGetAnimalVaccines()
     */
    @GetMapping("/animal/{animalId}")
    public ResponseEntity<List<Map<String, Object>>> getAnimalVaccines(
            @PathVariable String animalId,
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            // Authenticate
            String token = authHeader.substring(7); // Remove "Bearer "
            Claims claims = JwtUtil.validateTokenStatic(token);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String farmerId = JwtUtil.getUserIdFromToken(token);
            
            // Validate ObjectId format
            if (animalId.length() != 24 || !animalId.matches("[a-f0-9]{24}")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            // Verify animal belongs to farmer
            Animal animal = animalService.findByFarmerAndId(farmerId, animalId);
            if (animal == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            // Get vaccines for this animal
            List<Vaccine> vaccines = vaccineService.findByAnimalId(animalId, farmerId);
            
            // Convert to response format with animal details
            List<Map<String, Object>> response = new ArrayList<>();
            for (Vaccine vaccine : vaccines) {
                response.add(createVaccineResponse(vaccine, animal));
            }
            
            System.out.println("✅ Retrieved " + vaccines.size() + " vaccine records for animal: " + animalId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/vaccines/{id} - Get single vaccine record
     * Matches exactly the SimpleBackend.VaccineHandler.handleGetSingleVaccine()
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getSingleVaccine(
            @PathVariable String id,
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            // Authenticate
            String token = authHeader.substring(7); // Remove "Bearer "
            Claims claims = JwtUtil.validateTokenStatic(token);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String farmerId = JwtUtil.getUserIdFromToken(token);
            
            // Validate ObjectId format
            if (id.length() != 24 || !id.matches("[a-f0-9]{24}")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            // Get vaccine
            Vaccine vaccine = vaccineService.findById(id, farmerId);
            if (vaccine == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            // Get animal details
            Animal animal = animalService.findByFarmerAndId(farmerId, vaccine.getAnimal().toString());
            
            // Response with animal details
            Map<String, Object> response = createVaccineResponse(vaccine, animal);
            
            System.out.println("✅ Retrieved vaccine record: " + id);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/vaccines/{id} - Update vaccine record
     * Matches exactly the SimpleBackend.VaccineHandler.handleUpdateVaccine()
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateVaccine(
            @PathVariable String id,
            @RequestBody Map<String, Object> updates,
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            // Authenticate
            String token = authHeader.substring(7); // Remove "Bearer "
            Claims claims = JwtUtil.validateTokenStatic(token);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String farmerId = JwtUtil.getUserIdFromToken(token);
            
            // Get existing vaccine
            Vaccine vaccine = vaccineService.findById(id, farmerId);
            if (vaccine == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            // Handle animal_id update and validation
            String newAnimalId = (String) updates.get("animal_id");
            if (newAnimalId != null && !newAnimalId.isEmpty()) {
                Animal animal = animalService.findByFarmerAndId(farmerId, newAnimalId);
                if (animal == null) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("message", "Animal not found in your farm");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                }
                vaccine.setAnimal(new ObjectId(newAnimalId));
                vaccine.setAnimalName(animal.getName());
            }
            
            // Update fields
            if (updates.get("vaccine_name") != null) {
                vaccine.setVaccineName((String) updates.get("vaccine_name"));
            }
            if (updates.get("vaccine_date") != null) {
                vaccine.setVaccineDate(parseDate((String) updates.get("vaccine_date")));
            }
            if (updates.get("notes") != null) {
                vaccine.setNotes((String) updates.get("notes"));
            }
            if (updates.get("next_due_date") != null) {
                String nextDueDateStr = (String) updates.get("next_due_date");
                if (nextDueDateStr.isEmpty()) {
                    vaccine.setNextDueDate(null);
                } else {
                    vaccine.setNextDueDate(parseDate(nextDueDateStr));
                }
            }
            
            vaccine.setUpdatedAt(new Date());
            
            // Save updated vaccine
            Vaccine updatedVaccine = vaccineService.updateVaccine(vaccine);
            if (updatedVaccine == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            // Get animal details for response
            Animal animal = animalService.findByFarmerAndId(farmerId, updatedVaccine.getAnimal().toString());
            
            // Response with animal details
            Map<String, Object> response = createVaccineResponse(updatedVaccine, animal);
            
            System.out.println("✅ Vaccine record updated: " + id);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/vaccines/{id} - Delete vaccine record
     * Matches exactly the SimpleBackend.VaccineHandler.handleDeleteVaccine()
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteVaccine(
            @PathVariable String id,
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            // Authenticate
            String token = authHeader.substring(7); // Remove "Bearer "
            Claims claims = JwtUtil.validateTokenStatic(token);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String farmerId = JwtUtil.getUserIdFromToken(token);
            
            // Check if vaccine exists
            Vaccine vaccine = vaccineService.findById(id, farmerId);
            if (vaccine == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            // Delete vaccine
            boolean deleted = vaccineService.deleteById(id, farmerId);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            // Response
            Map<String, String> response = new HashMap<>();
            response.put("message", "Vaccine record deleted successfully");
            
            System.out.println("✅ Vaccine record deleted: " + id);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Helper method to create vaccine response with animal details
     * Matches exactly the SimpleBackend.VaccineHandler.createVaccineResponse()
     */
    private Map<String, Object> createVaccineResponse(Vaccine vaccine, Animal animal) {
        Map<String, Object> response = new HashMap<>();
        response.put("_id", vaccine.getId().toString());
        response.put("vaccine_name", vaccine.getVaccineName());
        response.put("animal_name", vaccine.getAnimalName());
        response.put("vaccine_date", vaccine.getVaccineDate());
        response.put("notes", vaccine.getNotes());
        response.put("next_due_date", vaccine.getNextDueDate());
        response.put("createdAt", vaccine.getCreatedAt());
        
        // Add animal details
        if (animal != null) {
            Map<String, Object> animalData = new HashMap<>();
            animalData.put("_id", animal.getId().toString());
            animalData.put("name", animal.getName());
            animalData.put("type", animal.getType());
            animalData.put("breed", animal.getBreed());
            response.put("animal", animalData);
        }
        
        return response;
    }

    /**
     * Helper method to parse date strings safely
     * Matches exactly the SimpleBackend parseDate method
     */
    private Date parseDate(String dateString) {
        try {
            // Try ISO format first (YYYY-MM-DD)
            if (dateString.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return dateFormat.parse(dateString);
            }
            // Try timestamp format
            return new Date(Long.parseLong(dateString));
        } catch (Exception e) {
            // Fallback to current date
            return new Date();
        }
    }
}