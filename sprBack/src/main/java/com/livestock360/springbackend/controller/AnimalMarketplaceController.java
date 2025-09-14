package com.livestock360.springbackend.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.livestock360.springbackend.model.Animal;
import com.livestock360.springbackend.service.AnimalService;
import com.livestock360.springbackend.service.FarmerService;
import com.livestock360.springbackend.model.Farmer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for marketplace animal operations accessible by customers
 */
@RestController
@RequestMapping("/api/marketplace")
@CrossOrigin(origins = "*")
public class AnimalMarketplaceController {

    private final AnimalService animalService;
    private final FarmerService farmerService;
    private final MongoDatabase database;
    private final SimpleDateFormat dateFormat;

    @Autowired
    public AnimalMarketplaceController(AnimalService animalService, FarmerService farmerService, MongoDatabase database) {
        this.animalService = animalService;
        this.farmerService = farmerService;
        this.database = database;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    /**
     * Get all animals available in the marketplace (public listing)
     * This endpoint is accessible by customers to browse available animals
     */
    @GetMapping("/animals")
    public ResponseEntity<String> getAllMarketplaceAnimals(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String breed,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        
        try {
            System.out.println("Marketplace animals request - Type: " + type + ", Breed: " + breed);

            // Get all animals from database directly since AnimalService doesn't have findAll()
            MongoCollection<Document> collection = database.getCollection("animals");
            List<Animal> animals = new ArrayList<>();
            
            // Build query based on filters
            Document query = new Document();
            if (type != null && !type.trim().isEmpty()) {
                query.append("type", type);
            }
            if (breed != null && !breed.trim().isEmpty()) {
                query.append("breed", breed);
            }
            
            // Execute query and convert to Animal objects
            for (Document doc : collection.find(query)) {
                Animal animal = convertDocumentToAnimal(doc);
                if (animal != null) {
                    animals.add(animal);
                }
            }

            // Apply pagination
            int start = Math.max(0, (page - 1) * limit);
            int end = Math.min(animals.size(), start + limit);
            List<Animal> paginatedAnimals = animals.subList(start, end);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Animals retrieved successfully");
            response.addProperty("totalAnimals", animals.size());
            response.addProperty("currentPage", page);
            response.addProperty("totalPages", (int) Math.ceil((double) animals.size() / limit));

            JsonArray animalsArray = new JsonArray();
            for (Animal animal : paginatedAnimals) {
                JsonObject animalObj = new JsonObject();
                animalObj.addProperty("id", animal.getId().toString());
                animalObj.addProperty("name", animal.getName());
                animalObj.addProperty("type", animal.getType());
                animalObj.addProperty("breed", animal.getBreed());
                animalObj.addProperty("age", animal.getAge());
                animalObj.addProperty("gender", animal.getGender());
                animalObj.addProperty("details", animal.getDetails());
                animalObj.addProperty("photo_url", animal.getPhoto_url());
                animalObj.addProperty("farmerId", animal.getFarmer().toString());
                animalObj.addProperty("createdAt", dateFormat.format(animal.getCreatedAt()));
                
                // Get farmer details
                try {
                    Farmer farmer = farmerService.findById(animal.getFarmer().toString());
                    if (farmer != null) {
                        animalObj.addProperty("farmerName", farmer.getName());
                        animalObj.addProperty("farmerPhone", farmer.getPhone());
                        animalObj.addProperty("farmerLocation", farmer.getLocation());
                    }
                } catch (Exception e) {
                    System.out.println("Error getting farmer details: " + e.getMessage());
                }
                
                animalsArray.add(animalObj);
            }
            
            response.add("animals", animalsArray);
            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error retrieving marketplace animals: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Error retrieving animals");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    /**
     * Convert MongoDB Document to Animal object
     */
    private Animal convertDocumentToAnimal(Document doc) {
        try {
            Animal animal = new Animal();
            animal.setId(doc.getObjectId("_id"));
            animal.setName(doc.getString("name"));
            animal.setType(doc.getString("type"));
            animal.setBreed(doc.getString("breed"));
            animal.setAge(doc.getInteger("age"));
            animal.setGender(doc.getString("gender"));
            animal.setDetails(doc.getString("details"));
            animal.setPhoto_url(doc.getString("photo_url"));
            
            // Handle farmer field - it could be ObjectId or String
            Object farmerObj = doc.get("farmer");
            if (farmerObj instanceof ObjectId) {
                animal.setFarmer((ObjectId) farmerObj);
            } else if (farmerObj instanceof String) {
                animal.setFarmer(new ObjectId((String) farmerObj));
            }
            
            animal.setCreatedAt(doc.getDate("createdAt"));
            
            return animal;
        } catch (Exception e) {
            System.out.println("Error converting document to animal: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get a specific animal by ID for marketplace viewing
     */
    @GetMapping("/animals/{animalId}")
    public ResponseEntity<String> getMarketplaceAnimalById(@PathVariable String animalId) {
        try {
            System.out.println("Getting marketplace animal by ID: " + animalId);

            Animal animal = animalService.findById(animalId);
            
            if (animal == null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Animal not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response.toString());
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Animal retrieved successfully");

            JsonObject animalObj = new JsonObject();
            animalObj.addProperty("id", animal.getId().toString());
            animalObj.addProperty("name", animal.getName());
            animalObj.addProperty("type", animal.getType());
            animalObj.addProperty("breed", animal.getBreed());
            animalObj.addProperty("age", animal.getAge());
            animalObj.addProperty("gender", animal.getGender());
            animalObj.addProperty("details", animal.getDetails());
            animalObj.addProperty("photo_url", animal.getPhoto_url());
            animalObj.addProperty("farmerId", animal.getFarmer().toString());
            animalObj.addProperty("createdAt", dateFormat.format(animal.getCreatedAt()));
            
            // Get farmer details
            try {
                Farmer farmer = farmerService.findById(animal.getFarmer().toString());
                if (farmer != null) {
                    animalObj.addProperty("farmerName", farmer.getName());
                    animalObj.addProperty("farmerPhone", farmer.getPhone());
                    animalObj.addProperty("farmerLocation", farmer.getLocation());
                    animalObj.addProperty("farmerEmail", farmer.getEmail());
                }
            } catch (Exception e) {
                System.out.println("Error getting farmer details: " + e.getMessage());
            }
            
            response.add("animal", animalObj);
            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error retrieving marketplace animal: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Error retrieving animal details");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }


}