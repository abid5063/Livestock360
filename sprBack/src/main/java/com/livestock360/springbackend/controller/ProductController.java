package com.livestock360.springbackend.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.livestock360.springbackend.model.Farmer;
import com.livestock360.springbackend.service.ProductService;
import com.livestock360.springbackend.service.FarmerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for product selling system
 * Handles farmer product preferences and marketplace operations
 */
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;
    private final FarmerService farmerService;

    @Autowired
    public ProductController(ProductService productService, FarmerService farmerService) {
        this.productService = productService;
        this.farmerService = farmerService;
    }

    /**
     * Enable/disable farmer selling and update product preferences
     * PUT /api/products/farmer/enable
     */
    @PutMapping("/farmer/enable")
    public ResponseEntity<String> updateFarmerSellingStatus(@RequestBody Map<String, Object> requestData) {
        try {
            System.out.println("Updating farmer selling status: " + requestData);

            String farmerId = (String) requestData.get("farmerId");
            Object enableSellingObj = requestData.get("enableSelling");
            
            if (farmerId == null || enableSellingObj == null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Missing required fields: farmerId and enableSelling");
                return ResponseEntity.badRequest().body(response.toString());
            }

            boolean enableSelling = (Boolean) enableSellingObj;

            // Extract product preferences if enabling selling
            Map<String, Boolean> productPreferences = null;
            if (enableSelling && requestData.containsKey("products")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> productsObj = (Map<String, Object>) requestData.get("products");
                productPreferences = new java.util.HashMap<>();
                
                for (Map.Entry<String, Object> entry : productsObj.entrySet()) {
                    if (entry.getValue() instanceof Boolean) {
                        productPreferences.put(entry.getKey(), (Boolean) entry.getValue());
                    }
                }
            }

            // Update farmer selling status
            boolean success = productService.updateFarmerSellingStatus(farmerId, enableSelling, productPreferences);
            
            if (!success) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Farmer not found or failed to update");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response.toString());
            }

            // Get updated farmer for response
            Farmer updatedFarmer = farmerService.findById(farmerId);

            // Return success response
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", enableSelling ? "Selling enabled successfully" : "Selling disabled successfully");
            if (updatedFarmer != null) {
                response.add("farmer", farmerToJson(updatedFarmer));
            }
            
            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error updating farmer selling status: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Error updating selling status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    /**
     * Get all farmers who are selling products
     * GET /api/products/available-sellers
     */
    @GetMapping("/available-sellers")
    public ResponseEntity<String> getAvailableSellers(
            @RequestParam(value = "product", required = false) String productType,
            @RequestParam(value = "location", required = false) String location) {
        try {
            System.out.println("Getting available sellers - product: " + productType + ", location: " + location);

            List<Farmer> sellers = productService.getAvailableSellers();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Available sellers retrieved successfully");
            response.addProperty("totalSellers", sellers.size());

            JsonArray sellersArray = new JsonArray();
            for (Farmer seller : sellers) {
                sellersArray.add(farmerToJson(seller));
            }
            response.add("sellers", sellersArray);

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error getting available sellers: " + e.getMessage());
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Error retrieving sellers");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    /**
     * Get specific farmer's product details
     * GET /api/products/farmer/{farmerId}/products
     */
    @GetMapping("/farmer/{farmerId}/products")
    public ResponseEntity<String> getFarmerProducts(@PathVariable String farmerId) {
        try {
            System.out.println("Getting products for farmer: " + farmerId);

            // Get farmer first
            Farmer farmer = farmerService.findById(farmerId);
            if (farmer == null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Farmer not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response.toString());
            }

            // Note: Could also get product details with productService.getFarmerProducts(farmerId) if needed

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Farmer products retrieved successfully");
            response.add("farmer", farmerToJson(farmer));

            // Add available products list
            JsonArray availableProducts = new JsonArray();
            if (Boolean.TRUE.equals(farmer.getSellsMilkCow())) {
                JsonObject product = new JsonObject();
                product.addProperty("type", "MILK_COW");
                product.addProperty("name", "Cow Milk");
                product.addProperty("available", true);
                availableProducts.add(product);
            }
            if (Boolean.TRUE.equals(farmer.getSellsMilkBuffalo())) {
                JsonObject product = new JsonObject();
                product.addProperty("type", "MILK_BUFFALO");
                product.addProperty("name", "Buffalo Milk");
                product.addProperty("available", true);
                availableProducts.add(product);
            }
            if (Boolean.TRUE.equals(farmer.getSellsMilkGoat())) {
                JsonObject product = new JsonObject();
                product.addProperty("type", "MILK_GOAT");
                product.addProperty("name", "Goat Milk");
                product.addProperty("available", true);
                availableProducts.add(product);
            }
            if (Boolean.TRUE.equals(farmer.getSellsButter())) {
                JsonObject product = new JsonObject();
                product.addProperty("type", "BUTTER");
                product.addProperty("name", "Fresh Butter");
                product.addProperty("available", true);
                availableProducts.add(product);
            }
            if (Boolean.TRUE.equals(farmer.getSellsHenEggs())) {
                JsonObject product = new JsonObject();
                product.addProperty("type", "EGGS_HEN");
                product.addProperty("name", "Hen Eggs");
                product.addProperty("available", true);
                availableProducts.add(product);
            }
            if (Boolean.TRUE.equals(farmer.getSellsDuckEggs())) {
                JsonObject product = new JsonObject();
                product.addProperty("type", "EGGS_DUCK");
                product.addProperty("name", "Duck Eggs");
                product.addProperty("available", true);
                availableProducts.add(product);
            }
            
            response.add("availableProducts", availableProducts);
            response.addProperty("totalProducts", availableProducts.size());

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error getting farmer products: " + e.getMessage());
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Error retrieving farmer products");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    /**
     * Get all product types and their display names
     * GET /api/products/types
     */
    @GetMapping("/types")
    public ResponseEntity<String> getProductTypes() {
        try {
            System.out.println("Getting all product types");

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Product types retrieved successfully");

            JsonArray productTypes = new JsonArray();
            
            // Add all available product types
            String[] types = {"MILK_COW", "MILK_BUFFALO", "MILK_GOAT", "BUTTER", "EGGS_HEN", "EGGS_DUCK"};
            String[] names = {"Cow Milk", "Buffalo Milk", "Goat Milk", "Fresh Butter", "Hen Eggs", "Duck Eggs"};
            
            for (int i = 0; i < types.length; i++) {
                JsonObject productType = new JsonObject();
                productType.addProperty("type", types[i]);
                productType.addProperty("name", names[i]);
                productType.addProperty("category", types[i].startsWith("MILK") ? "Dairy" : 
                                                  types[i].equals("BUTTER") ? "Dairy" : "Poultry");
                productTypes.add(productType);
            }
            
            response.add("productTypes", productTypes);
            response.addProperty("totalTypes", productTypes.size());

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error getting product types: " + e.getMessage());
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Error retrieving product types");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    /**
     * Get marketplace statistics
     * GET /api/products/marketplace/stats
     */
    @GetMapping("/marketplace/stats")
    public ResponseEntity<String> getMarketplaceStatistics() {
        try {
            System.out.println("Getting marketplace statistics");

            Map<String, Object> stats = productService.getProductStatistics();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Marketplace statistics retrieved successfully");
            
            // Convert stats to JSON
            JsonObject statsJson = new JsonObject();
            for (Map.Entry<String, Object> entry : stats.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    statsJson.addProperty(entry.getKey(), (Number) entry.getValue());
                } else if (entry.getValue() instanceof String) {
                    statsJson.addProperty(entry.getKey(), (String) entry.getValue());
                }
            }
            response.add("statistics", statsJson);

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error getting marketplace statistics: " + e.getMessage());
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Error retrieving marketplace statistics");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    /**
     * Validate if farmer sells specific products
     * POST /api/products/farmer/validate
     */
    @PostMapping("/farmer/validate")
    public ResponseEntity<String> validateFarmerProducts(@RequestBody Map<String, Object> requestData) {
        try {
            System.out.println("Validating farmer products: " + requestData);

            String farmerId = (String) requestData.get("farmerId");
            @SuppressWarnings("unchecked")
            Map<String, Object> productsObj = (Map<String, Object>) requestData.get("products");
            
            if (farmerId == null || productsObj == null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Missing required fields: farmerId and products");
                return ResponseEntity.badRequest().body(response.toString());
            }

            // Convert products map
            Map<String, Integer> products = new java.util.HashMap<>();
            for (Map.Entry<String, Object> entry : productsObj.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    products.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                }
            }

            boolean isValid = productService.validateFarmerProducts(farmerId, products);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("isValid", isValid);
            response.addProperty("message", isValid ? "Farmer sells all requested products" : 
                                                    "Farmer does not sell some requested products");

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error validating farmer products: " + e.getMessage());
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Error validating products");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    // Helper method to convert Farmer to JSON with selling info
    private JsonObject farmerToJson(Farmer farmer) {
        JsonObject farmerJson = new JsonObject();
        farmerJson.addProperty("id", farmer.getId().toString());
        farmerJson.addProperty("name", farmer.getName());
        farmerJson.addProperty("phone", farmer.getPhone());
        farmerJson.addProperty("location", farmer.getLocation());
        farmerJson.addProperty("isSeller", farmer.getIsSeller() != null ? farmer.getIsSeller() : false);
        farmerJson.addProperty("totalProducts", farmer.getProductCount());

        // Add selling status for each product
        JsonObject sellingStatus = new JsonObject();
        sellingStatus.addProperty("sellsMilkCow", Boolean.TRUE.equals(farmer.getSellsMilkCow()));
        sellingStatus.addProperty("sellsMilkBuffalo", Boolean.TRUE.equals(farmer.getSellsMilkBuffalo()));
        sellingStatus.addProperty("sellsMilkGoat", Boolean.TRUE.equals(farmer.getSellsMilkGoat()));
        sellingStatus.addProperty("sellsButter", Boolean.TRUE.equals(farmer.getSellsButter()));
        sellingStatus.addProperty("sellsHenEggs", Boolean.TRUE.equals(farmer.getSellsHenEggs()));
        sellingStatus.addProperty("sellsDuckEggs", Boolean.TRUE.equals(farmer.getSellsDuckEggs()));
        farmerJson.add("sellingStatus", sellingStatus);

        // Add available products with display names
        JsonArray availableProducts = new JsonArray();
        if (Boolean.TRUE.equals(farmer.getSellsMilkCow())) {
            JsonObject product = new JsonObject();
            product.addProperty("type", "MILK_COW");
            product.addProperty("name", "Cow Milk");
            availableProducts.add(product);
        }
        if (Boolean.TRUE.equals(farmer.getSellsMilkBuffalo())) {
            JsonObject product = new JsonObject();
            product.addProperty("type", "MILK_BUFFALO");
            product.addProperty("name", "Buffalo Milk");
            availableProducts.add(product);
        }
        if (Boolean.TRUE.equals(farmer.getSellsMilkGoat())) {
            JsonObject product = new JsonObject();
            product.addProperty("type", "MILK_GOAT");
            product.addProperty("name", "Goat Milk");
            availableProducts.add(product);
        }
        if (Boolean.TRUE.equals(farmer.getSellsButter())) {
            JsonObject product = new JsonObject();
            product.addProperty("type", "BUTTER");
            product.addProperty("name", "Fresh Butter");
            availableProducts.add(product);
        }
        if (Boolean.TRUE.equals(farmer.getSellsHenEggs())) {
            JsonObject product = new JsonObject();
            product.addProperty("type", "EGGS_HEN");
            product.addProperty("name", "Hen Eggs");
            availableProducts.add(product);
        }
        if (Boolean.TRUE.equals(farmer.getSellsDuckEggs())) {
            JsonObject product = new JsonObject();
            product.addProperty("type", "EGGS_DUCK");
            product.addProperty("name", "Duck Eggs");
            availableProducts.add(product);
        }
        farmerJson.add("availableProducts", availableProducts);

        return farmerJson;
    }
}