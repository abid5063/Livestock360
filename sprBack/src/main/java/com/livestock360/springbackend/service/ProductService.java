package com.livestock360.springbackend.service;

import com.livestock360.springbackend.model.Farmer;
import com.livestock360.springbackend.model.Order;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for managing product selling functionality
 * Handles farmer product preferences, available sellers, and product validation
 */
@Service
public class ProductService {

    private final MongoDatabase database;
    private final FarmerService farmerService;

    @Autowired
    public ProductService(MongoDatabase database, FarmerService farmerService) {
        this.database = database;
        this.farmerService = farmerService;
    }

    private MongoCollection<Document> getFarmersCollection() {
        return database.getCollection("farmers");
    }

    /**
     * Enable or disable product selling for a farmer
     */
    public boolean updateFarmerSellingStatus(String farmerId, boolean isSeller, Map<String, Boolean> products) {
        try {
            ObjectId farmerObjectId = new ObjectId(farmerId);
            MongoCollection<Document> collection = getFarmersCollection();

            // Build the update document
            Document updateDoc = new Document()
                    .append("isSeller", isSeller);

            if (isSeller && products != null) {
                // Update individual product selling preferences
                updateDoc.append("sellsMilkCow", products.getOrDefault("MILK_COW", false))
                         .append("sellsMilkBuffalo", products.getOrDefault("MILK_BUFFALO", false))
                         .append("sellsMilkGoat", products.getOrDefault("MILK_GOAT", false))
                         .append("sellsButter", products.getOrDefault("BUTTER", false))
                         .append("sellsHenEggs", products.getOrDefault("HEN_EGGS", false))
                         .append("sellsDuckEggs", products.getOrDefault("DUCK_EGGS", false));
            } else if (!isSeller) {
                // If disabling selling, set all products to false
                updateDoc.append("sellsMilkCow", false)
                         .append("sellsMilkBuffalo", false)
                         .append("sellsMilkGoat", false)
                         .append("sellsButter", false)
                         .append("sellsHenEggs", false)
                         .append("sellsDuckEggs", false);
            }

            collection.updateOne(
                Filters.eq("_id", farmerObjectId),
                new Document("$set", updateDoc)
            );

            System.out.println("Updated farmer " + farmerId + " selling status to: " + isSeller);
            return true;

        } catch (Exception e) {
            System.out.println("Error updating farmer selling status: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all farmers who are currently selling products
     */
    public List<Farmer> getAvailableSellers() {
        try {
            MongoCollection<Document> collection = getFarmersCollection();
            List<Farmer> sellers = new ArrayList<>();

            // Find farmers where isSeller is true
            for (Document doc : collection.find(Filters.eq("isSeller", true))) {
                Farmer farmer = documentToFarmer(doc);
                if (farmer != null && farmer.sellsAnyProduct()) {
                    sellers.add(farmer);
                }
            }

            System.out.println("Found " + sellers.size() + " available sellers");
            return sellers;

        } catch (Exception e) {
            System.out.println("Error getting available sellers: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get available products for a specific farmer
     */
    public Map<String, Boolean> getFarmerProducts(String farmerId) {
        try {
            Farmer farmer = farmerService.findById(farmerId);
            if (farmer == null || !Boolean.TRUE.equals(farmer.getIsSeller())) {
                return new HashMap<>();
            }

            Map<String, Boolean> products = new HashMap<>();
            products.put("MILK_COW", Boolean.TRUE.equals(farmer.getSellsMilkCow()));
            products.put("MILK_BUFFALO", Boolean.TRUE.equals(farmer.getSellsMilkBuffalo()));
            products.put("MILK_GOAT", Boolean.TRUE.equals(farmer.getSellsMilkGoat()));
            products.put("BUTTER", Boolean.TRUE.equals(farmer.getSellsButter()));
            products.put("HEN_EGGS", Boolean.TRUE.equals(farmer.getSellsHenEggs()));
            products.put("DUCK_EGGS", Boolean.TRUE.equals(farmer.getSellsDuckEggs()));

            return products;

        } catch (Exception e) {
            System.out.println("Error getting farmer products: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get available products for a farmer with display names
     */
    public Map<String, Object> getFarmerProductsWithDetails(String farmerId) {
        try {
            Farmer farmer = farmerService.findById(farmerId);
            if (farmer == null || !Boolean.TRUE.equals(farmer.getIsSeller())) {
                return new HashMap<>();
            }

            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> products = new ArrayList<>();

            if (Boolean.TRUE.equals(farmer.getSellsMilkCow())) {
                products.add(createProductInfo("MILK_COW", "Cow Milk", "Fresh cow milk", "liter"));
            }
            if (Boolean.TRUE.equals(farmer.getSellsMilkBuffalo())) {
                products.add(createProductInfo("MILK_BUFFALO", "Buffalo Milk", "Fresh buffalo milk", "liter"));
            }
            if (Boolean.TRUE.equals(farmer.getSellsMilkGoat())) {
                products.add(createProductInfo("MILK_GOAT", "Goat Milk", "Fresh goat milk", "liter"));
            }
            if (Boolean.TRUE.equals(farmer.getSellsButter())) {
                products.add(createProductInfo("BUTTER", "Butter", "Fresh homemade butter", "kg"));
            }
            if (Boolean.TRUE.equals(farmer.getSellsHenEggs())) {
                products.add(createProductInfo("HEN_EGGS", "Hen Eggs", "Fresh hen eggs", "dozen"));
            }
            if (Boolean.TRUE.equals(farmer.getSellsDuckEggs())) {
                products.add(createProductInfo("DUCK_EGGS", "Duck Eggs", "Fresh duck eggs", "dozen"));
            }

            result.put("farmerId", farmerId);
            result.put("farmerName", farmer.getName());
            result.put("farmerLocation", farmer.getLocation());
            result.put("farmerPhone", farmer.getPhone());
            result.put("products", products);
            result.put("totalProducts", products.size());

            return result;

        } catch (Exception e) {
            System.out.println("Error getting farmer products with details: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Validate if farmer can sell the requested products
     */
    public boolean validateFarmerProducts(String farmerId, Map<String, Integer> requestedProducts) {
        try {
            Map<String, Boolean> farmerProducts = getFarmerProducts(farmerId);
            
            for (String productType : requestedProducts.keySet()) {
                if (!farmerProducts.getOrDefault(productType, false)) {
                    System.out.println("Farmer " + farmerId + " does not sell " + productType);
                    return false;
                }
                
                Integer quantity = requestedProducts.get(productType);
                if (quantity == null || quantity <= 0) {
                    System.out.println("Invalid quantity for product " + productType + ": " + quantity);
                    return false;
                }
            }
            
            return true;

        } catch (Exception e) {
            System.out.println("Error validating farmer products: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all sellers with their available products (for customer marketplace)
     */
    public List<Map<String, Object>> getAllSellersWithProducts() {
        try {
            List<Farmer> sellers = getAvailableSellers();
            List<Map<String, Object>> result = new ArrayList<>();

            for (Farmer farmer : sellers) {
                Map<String, Object> sellerData = getFarmerProductsWithDetails(farmer.getId().toString());
                if (!((List<?>) sellerData.get("products")).isEmpty()) {
                    result.add(sellerData);
                }
            }

            return result;

        } catch (Exception e) {
            System.out.println("Error getting all sellers with products: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get sellers filtered by product type
     */
    public List<Map<String, Object>> getSellersByProduct(String productType) {
        try {
            if (!Order.isValidProductType(productType)) {
                return new ArrayList<>();
            }

            String fieldName = getProductFieldName(productType);
            MongoCollection<Document> collection = getFarmersCollection();
            List<Map<String, Object>> result = new ArrayList<>();

            // Find farmers who sell the specific product
            Document query = new Document("isSeller", true).append(fieldName, true);
            
            for (Document doc : collection.find(query)) {
                Farmer farmer = documentToFarmer(doc);
                if (farmer != null) {
                    Map<String, Object> sellerData = getFarmerProductsWithDetails(farmer.getId().toString());
                    result.add(sellerData);
                }
            }

            return result;

        } catch (Exception e) {
            System.out.println("Error getting sellers by product: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Helper methods
    private Map<String, Object> createProductInfo(String type, String name, String description, String unit) {
        Map<String, Object> product = new HashMap<>();
        product.put("type", type);
        product.put("name", name);
        product.put("description", description);
        product.put("unit", unit);
        return product;
    }

    private String getProductFieldName(String productType) {
        switch (productType) {
            case "MILK_COW": return "sellsMilkCow";
            case "MILK_BUFFALO": return "sellsMilkBuffalo";
            case "MILK_GOAT": return "sellsMilkGoat";
            case "BUTTER": return "sellsButter";
            case "HEN_EGGS": return "sellsHenEggs";
            case "DUCK_EGGS": return "sellsDuckEggs";
            default: return null;
        }
    }

    private Farmer documentToFarmer(Document doc) {
        try {
            Farmer farmer = new Farmer();
            farmer.setId(doc.getObjectId("_id"));
            farmer.setName(doc.getString("name"));
            farmer.setEmail(doc.getString("email"));
            farmer.setPassword(doc.getString("password"));
            farmer.setSalt(doc.getString("salt"));
            farmer.setPhone(doc.getString("phone"));
            farmer.setLocation(doc.getString("location"));
            farmer.setAddress(doc.getString("address"));
            farmer.setProfilePicture(doc.getString("profilePicture"));
            farmer.setDateJoined(doc.getString("dateJoined"));
            
            // Handle token count with null safety
            Integer tokenCount = doc.getInteger("tokenCount");
            farmer.setTokenCount(tokenCount != null ? tokenCount : 0);
            
            // Handle product selling fields with null safety
            farmer.setIsSeller(doc.getBoolean("isSeller", false));
            farmer.setSellsMilkCow(doc.getBoolean("sellsMilkCow", false));
            farmer.setSellsMilkBuffalo(doc.getBoolean("sellsMilkBuffalo", false));
            farmer.setSellsMilkGoat(doc.getBoolean("sellsMilkGoat", false));
            farmer.setSellsButter(doc.getBoolean("sellsButter", false));
            farmer.setSellsHenEggs(doc.getBoolean("sellsHenEggs", false));
            farmer.setSellsDuckEggs(doc.getBoolean("sellsDuckEggs", false));
            
            return farmer;
        } catch (Exception e) {
            System.out.println("Error converting document to farmer: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get product statistics for admin dashboard
     */
    public Map<String, Object> getProductStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            MongoCollection<Document> collection = getFarmersCollection();
            
            long totalSellers = collection.countDocuments(Filters.eq("isSeller", true));
            long milkCowSellers = collection.countDocuments(Filters.eq("sellsMilkCow", true));
            long milkBuffaloSellers = collection.countDocuments(Filters.eq("sellsMilkBuffalo", true));
            long milkGoatSellers = collection.countDocuments(Filters.eq("sellsMilkGoat", true));
            long butterSellers = collection.countDocuments(Filters.eq("sellsButter", true));
            long henEggSellers = collection.countDocuments(Filters.eq("sellsHenEggs", true));
            long duckEggSellers = collection.countDocuments(Filters.eq("sellsDuckEggs", true));
            
            stats.put("totalSellers", totalSellers);
            stats.put("productBreakdown", Map.of(
                "milkCow", milkCowSellers,
                "milkBuffalo", milkBuffaloSellers,
                "milkGoat", milkGoatSellers,
                "butter", butterSellers,
                "henEggs", henEggSellers,
                "duckEggs", duckEggSellers
            ));
            
            return stats;
            
        } catch (Exception e) {
            System.out.println("Error getting product statistics: " + e.getMessage());
            return new HashMap<>();
        }
    }
}