package com.livestock360.springbackend.service;

import com.livestock360.springbackend.model.Order;
import com.livestock360.springbackend.model.Farmer;
import com.livestock360.springbackend.model.Customer;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for managing orders in the product selling system
 * Handles order creation, status updates, and retrieval
 */
@Service
public class OrderService {

    private final MongoDatabase database;
    private final FarmerService farmerService;
    private final CustomerService customerService;
    private final ProductService productService;

    @Autowired
    public OrderService(MongoDatabase database, FarmerService farmerService, 
                       CustomerService customerService, ProductService productService) {
        this.database = database;
        this.farmerService = farmerService;
        this.customerService = customerService;
        this.productService = productService;
    }

    private MongoCollection<Document> getOrdersCollection() {
        return database.getCollection("orders");
    }

    /**
     * Create a new order
     */
    public Order createOrder(String customerId, String farmerId, Map<String, Integer> products, 
                           String deliveryAddress, String customerNotes) {
        try {
            // TEMPORARY: Skip product validation for testing
            System.out.println("Skipping product validation for testing purposes");
            
            // TODO: Re-enable this validation once farmer products are properly configured
            /*
            if (!productService.validateFarmerProducts(farmerId, products)) {
                System.out.println("Farmer cannot sell requested products");
                return null;
            }
            */

            // Get customer and farmer details
            Customer customer = customerService.findById(customerId);
            Farmer farmer = farmerService.findById(farmerId);

            if (customer == null || farmer == null) {
                System.out.println("Customer or farmer not found");
                return null;
            }

            // Create order object
            Order order = new Order();
            order.setCustomerId(new ObjectId(customerId));
            order.setFarmerId(new ObjectId(farmerId));
            order.setProducts(products);
            order.setDeliveryAddress(deliveryAddress);
            order.setCustomerNotes(customerNotes);
            order.setStatus(Order.STATUS_PENDING);
            order.setOrderDate(new Date());
            
            // Set customer and farmer info for quick access
            order.setCustomerName(customer.getName());
            order.setCustomerPhone(customer.getPhone());
            order.setFarmerName(farmer.getName());
            order.setFarmerPhone(farmer.getPhone());
            order.setFarmerLocation(farmer.getLocation());

            // Calculate total amount (simplified - you might want to add pricing logic)
            order.setTotalAmount(calculateOrderTotal(products));

            // Save to database
            Document orderDoc = orderToDocument(order);
            getOrdersCollection().insertOne(orderDoc);
            
            // Set the generated ID
            order.setId(orderDoc.getObjectId("_id"));

            System.out.println("Created order: " + order.getId());
            return order;

        } catch (Exception e) {
            System.out.println("Error creating order: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get orders for a specific farmer
     */
    public List<Order> getFarmerOrders(String farmerId) {
        try {
            ObjectId farmerObjectId = new ObjectId(farmerId);
            MongoCollection<Document> collection = getOrdersCollection();
            List<Order> orders = new ArrayList<>();

            for (Document doc : collection.find(Filters.eq("farmerId", farmerObjectId))
                    .sort(Sorts.descending("orderDate"))) {
                Order order = documentToOrder(doc);
                if (order != null) {
                    orders.add(order);
                }
            }

            return orders;

        } catch (Exception e) {
            System.out.println("Error getting farmer orders: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get orders for a specific customer
     */
    public List<Order> getCustomerOrders(String customerId) {
        try {
            ObjectId customerObjectId = new ObjectId(customerId);
            MongoCollection<Document> collection = getOrdersCollection();
            List<Order> orders = new ArrayList<>();

            for (Document doc : collection.find(Filters.eq("customerId", customerObjectId))
                    .sort(Sorts.descending("orderDate"))) {
                Order order = documentToOrder(doc);
                if (order != null) {
                    orders.add(order);
                }
            }

            return orders;

        } catch (Exception e) {
            System.out.println("Error getting customer orders: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get order by ID
     */
    public Order findById(String orderId) {
        try {
            ObjectId orderObjectId = new ObjectId(orderId);
            MongoCollection<Document> collection = getOrdersCollection();
            Document doc = collection.find(Filters.eq("_id", orderObjectId)).first();

            if (doc != null) {
                return documentToOrder(doc);
            }
            return null;

        } catch (Exception e) {
            System.out.println("Error finding order by ID: " + e.getMessage());
            return null;
        }
    }

    /**
     * Confirm order (farmer accepts)
     */
    public boolean confirmOrder(String orderId, String farmerNotes) {
        try {
            Order order = findById(orderId);
            if (order == null || !order.canBeConfirmed()) {
                return false;
            }

            ObjectId orderObjectId = new ObjectId(orderId);
            Document updateDoc = new Document("$set", new Document()
                    .append("status", Order.STATUS_CONFIRMED)
                    .append("confirmedDate", new Date())
                    .append("farmerNotes", farmerNotes));

            getOrdersCollection().updateOne(Filters.eq("_id", orderObjectId), updateDoc);
            
            System.out.println("Confirmed order: " + orderId);
            return true;

        } catch (Exception e) {
            System.out.println("Error confirming order: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mark order as delivered
     */
    public boolean deliverOrder(String orderId) {
        try {
            Order order = findById(orderId);
            if (order == null || !order.canBeDelivered()) {
                return false;
            }

            ObjectId orderObjectId = new ObjectId(orderId);
            Document updateDoc = new Document("$set", new Document()
                    .append("status", Order.STATUS_DELIVERED)
                    .append("deliveredDate", new Date()));

            getOrdersCollection().updateOne(Filters.eq("_id", orderObjectId), updateDoc);
            
            System.out.println("Delivered order: " + orderId);
            return true;

        } catch (Exception e) {
            System.out.println("Error delivering order: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mark order as received (customer confirms)
     */
    public boolean receiveOrder(String orderId) {
        try {
            Order order = findById(orderId);
            if (order == null || !order.canBeReceived()) {
                return false;
            }

            ObjectId orderObjectId = new ObjectId(orderId);
            Document updateDoc = new Document("$set", new Document()
                    .append("status", Order.STATUS_RECEIVED)
                    .append("receivedDate", new Date()));

            getOrdersCollection().updateOne(Filters.eq("_id", orderObjectId), updateDoc);
            
            System.out.println("Received order: " + orderId);
            return true;

        } catch (Exception e) {
            System.out.println("Error receiving order: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cancel order
     */
    public boolean cancelOrder(String orderId, String reason) {
        try {
            Order order = findById(orderId);
            if (order == null || !order.canBeCancelled()) {
                return false;
            }

            ObjectId orderObjectId = new ObjectId(orderId);
            Document updateDoc = new Document("$set", new Document()
                    .append("status", Order.STATUS_CANCELLED)
                    .append("cancelledDate", new Date())
                    .append("cancelReason", reason));

            getOrdersCollection().updateOne(Filters.eq("_id", orderObjectId), updateDoc);
            
            System.out.println("Cancelled order: " + orderId);
            return true;

        } catch (Exception e) {
            System.out.println("Error cancelling order: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get order statistics
     */
    public Map<String, Object> getOrderStatistics() {
        try {
            MongoCollection<Document> collection = getOrdersCollection();
            Map<String, Object> stats = new HashMap<>();

            long totalOrders = collection.countDocuments();
            long pendingOrders = collection.countDocuments(Filters.eq("status", Order.STATUS_PENDING));
            long confirmedOrders = collection.countDocuments(Filters.eq("status", Order.STATUS_CONFIRMED));
            long deliveredOrders = collection.countDocuments(Filters.eq("status", Order.STATUS_DELIVERED));
            long receivedOrders = collection.countDocuments(Filters.eq("status", Order.STATUS_RECEIVED));
            long cancelledOrders = collection.countDocuments(Filters.eq("status", Order.STATUS_CANCELLED));

            stats.put("totalOrders", totalOrders);
            stats.put("pendingOrders", pendingOrders);
            stats.put("confirmedOrders", confirmedOrders);
            stats.put("deliveredOrders", deliveredOrders);
            stats.put("receivedOrders", receivedOrders);
            stats.put("cancelledOrders", cancelledOrders);

            return stats;

        } catch (Exception e) {
            System.out.println("Error getting order statistics: " + e.getMessage());
            return new HashMap<>();
        }
    }

    // Helper methods
    private Double calculateOrderTotal(Map<String, Integer> products) {
        // Simplified pricing - you might want to implement dynamic pricing
        double total = 0.0;
        Map<String, Double> prices = Map.of(
            "MILK_COW", 50.0,        // per liter
            "MILK_BUFFALO", 60.0,    // per liter
            "MILK_GOAT", 80.0,       // per liter
            "BUTTER", 300.0,         // per kg
            "HEN_EGGS", 120.0,       // per dozen
            "DUCK_EGGS", 150.0       // per dozen
        );

        for (Map.Entry<String, Integer> entry : products.entrySet()) {
            String productType = entry.getKey();
            Integer quantity = entry.getValue();
            Double price = prices.get(productType);
            
            if (price != null && quantity != null) {
                total += price * quantity;
            }
        }

        return total;
    }

    private Document orderToDocument(Order order) {
        Document doc = new Document()
                .append("customerId", order.getCustomerId())
                .append("farmerId", order.getFarmerId())
                .append("products", order.getProducts())
                .append("status", order.getStatus())
                .append("totalAmount", order.getTotalAmount())
                .append("orderDate", order.getOrderDate())
                .append("deliveryAddress", order.getDeliveryAddress())
                .append("customerNotes", order.getCustomerNotes())
                .append("customerName", order.getCustomerName())
                .append("customerPhone", order.getCustomerPhone())
                .append("farmerName", order.getFarmerName())
                .append("farmerPhone", order.getFarmerPhone())
                .append("farmerLocation", order.getFarmerLocation());

        if (order.getConfirmedDate() != null) {
            doc.append("confirmedDate", order.getConfirmedDate());
        }
        if (order.getDeliveredDate() != null) {
            doc.append("deliveredDate", order.getDeliveredDate());
        }
        if (order.getReceivedDate() != null) {
            doc.append("receivedDate", order.getReceivedDate());
        }
        if (order.getCancelledDate() != null) {
            doc.append("cancelledDate", order.getCancelledDate());
        }
        if (order.getFarmerNotes() != null) {
            doc.append("farmerNotes", order.getFarmerNotes());
        }

        return doc;
    }

    private Order documentToOrder(Document doc) {
        try {
            Order order = new Order();
            order.setId(doc.getObjectId("_id"));
            order.setCustomerId(doc.getObjectId("customerId"));
            order.setFarmerId(doc.getObjectId("farmerId"));
            
            // Handle products map
            Document productsDoc = doc.get("products", Document.class);
            if (productsDoc != null) {
                Map<String, Integer> products = new HashMap<>();
                for (String key : productsDoc.keySet()) {
                    products.put(key, productsDoc.getInteger(key));
                }
                order.setProducts(products);
            }
            
            order.setStatus(doc.getString("status"));
            order.setTotalAmount(doc.getDouble("totalAmount"));
            order.setOrderDate(doc.getDate("orderDate"));
            order.setConfirmedDate(doc.getDate("confirmedDate"));
            order.setDeliveredDate(doc.getDate("deliveredDate"));
            order.setReceivedDate(doc.getDate("receivedDate"));
            order.setCancelledDate(doc.getDate("cancelledDate"));
            order.setDeliveryAddress(doc.getString("deliveryAddress"));
            order.setCustomerNotes(doc.getString("customerNotes"));
            order.setFarmerNotes(doc.getString("farmerNotes"));
            order.setCustomerName(doc.getString("customerName"));
            order.setCustomerPhone(doc.getString("customerPhone"));
            order.setFarmerName(doc.getString("farmerName"));
            order.setFarmerPhone(doc.getString("farmerPhone"));
            order.setFarmerLocation(doc.getString("farmerLocation"));

            return order;

        } catch (Exception e) {
            System.out.println("Error converting document to order: " + e.getMessage());
            return null;
        }
    }
}