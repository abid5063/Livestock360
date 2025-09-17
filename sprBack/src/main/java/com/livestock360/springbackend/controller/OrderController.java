package com.livestock360.springbackend.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.livestock360.springbackend.model.Order;
import com.livestock360.springbackend.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for order management in product selling system
 * Handles order creation, status updates, and retrieval
 */
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;
    private final SimpleDateFormat dateFormat;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    /**
     * Create a new order
     * POST /api/orders/create
     */
    @PostMapping("/create")
    public ResponseEntity<String> createOrder(@RequestBody Map<String, Object> requestData) {
        try {
            System.out.println("Creating order with data: " + requestData);

            // Extract required fields
            String customerId = (String) requestData.get("customerId");
            String farmerId = (String) requestData.get("farmerId");
            String deliveryAddress = (String) requestData.get("deliveryAddress");
            String customerNotes = (String) requestData.get("customerNotes");

            // Extract products map
            @SuppressWarnings("unchecked")
            Map<String, Object> productsObj = (Map<String, Object>) requestData.get("products");
            Map<String, Integer> products = new HashMap<>();
            
            if (productsObj != null) {
                for (Map.Entry<String, Object> entry : productsObj.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof Number) {
                        products.put(entry.getKey(), ((Number) value).intValue());
                    }
                }
            }

            // Validate required fields
            if (customerId == null || farmerId == null || deliveryAddress == null || products.isEmpty()) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Missing required fields: customerId, farmerId, deliveryAddress, and products");
                return ResponseEntity.badRequest().body(response.toString());
            }

            // Create the order
            Order order = orderService.createOrder(customerId, farmerId, products, deliveryAddress, customerNotes);
            
            if (order == null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Failed to create order. Please check if farmer sells requested products.");
                return ResponseEntity.badRequest().body(response.toString());
            }

            // Return success response
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Order created successfully");
            response.add("order", orderToJson(order));
            
            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error creating order: " + e.getMessage());
            e.printStackTrace();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Error creating order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    /**
     * Get orders for a specific farmer
     * GET /api/orders/farmer/{farmerId}
     */
    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<String> getFarmerOrders(@PathVariable String farmerId) {
        try {
            System.out.println("Getting orders for farmer: " + farmerId);

            List<Order> orders = orderService.getFarmerOrders(farmerId);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Orders retrieved successfully");
            response.addProperty("totalOrders", orders.size());

            JsonArray ordersArray = new JsonArray();
            for (Order order : orders) {
                ordersArray.add(orderToJson(order));
            }
            response.add("orders", ordersArray);

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error getting farmer orders: " + e.getMessage());
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Error retrieving orders");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    /**
     * Get orders for a specific customer
     * GET /api/orders/customer/{customerId}
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<String> getCustomerOrders(@PathVariable String customerId) {
        try {
            System.out.println("Getting orders for customer: " + customerId);

            List<Order> orders = orderService.getCustomerOrders(customerId);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Orders retrieved successfully");
            response.addProperty("totalOrders", orders.size());

            JsonArray ordersArray = new JsonArray();
            for (Order order : orders) {
                ordersArray.add(orderToJson(order));
            }
            response.add("orders", ordersArray);

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error getting customer orders: " + e.getMessage());
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Error retrieving orders");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    /**
     * Get order by ID
     * GET /api/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<String> getOrderById(@PathVariable String orderId) {
        try {
            System.out.println("Getting order by ID: " + orderId);

            Order order = orderService.findById(orderId);
            
            if (order == null) {
                JsonObject response = new JsonObject();
                response.addProperty("success", false);
                response.addProperty("message", "Order not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response.toString());
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Order retrieved successfully");
            response.add("order", orderToJson(order));

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error getting order: " + e.getMessage());
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Error retrieving order");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    /**
     * Confirm order (farmer accepts)
     * PUT /api/orders/{orderId}/confirm
     */
    @PutMapping("/{orderId}/confirm")
    public ResponseEntity<String> confirmOrder(@PathVariable String orderId, @RequestBody Map<String, String> requestData) {
        try {
            System.out.println("Confirming order: " + orderId);

            String farmerNotes = requestData.get("farmerNotes");
            boolean success = orderService.confirmOrder(orderId, farmerNotes);
            
            JsonObject response = new JsonObject();
            if (success) {
                response.addProperty("success", true);
                response.addProperty("message", "Order confirmed successfully");
                
                // Return updated order
                Order updatedOrder = orderService.findById(orderId);
                if (updatedOrder != null) {
                    response.add("order", orderToJson(updatedOrder));
                }
            } else {
                response.addProperty("success", false);
                response.addProperty("message", "Failed to confirm order. Order may not be in pending status.");
                return ResponseEntity.badRequest().body(response.toString());
            }

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error confirming order: " + e.getMessage());
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Error confirming order");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    /**
     * Mark order as delivered
     * PUT /api/orders/{orderId}/deliver
     */
    @PutMapping("/{orderId}/deliver")
    public ResponseEntity<String> deliverOrder(@PathVariable String orderId) {
        try {
            System.out.println("Delivering order: " + orderId);

            boolean success = orderService.deliverOrder(orderId);
            
            JsonObject response = new JsonObject();
            if (success) {
                response.addProperty("success", true);
                response.addProperty("message", "Order marked as delivered successfully");
                
                // Return updated order
                Order updatedOrder = orderService.findById(orderId);
                if (updatedOrder != null) {
                    response.add("order", orderToJson(updatedOrder));
                }
            } else {
                response.addProperty("success", false);
                response.addProperty("message", "Failed to deliver order. Order may not be in confirmed status.");
                return ResponseEntity.badRequest().body(response.toString());
            }

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error delivering order: " + e.getMessage());
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Error delivering order");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    /**
     * Mark order as received (customer confirms)
     * PUT /api/orders/{orderId}/receive
     */
    @PutMapping("/{orderId}/receive")
    public ResponseEntity<String> receiveOrder(@PathVariable String orderId) {
        try {
            System.out.println("Receiving order: " + orderId);

            boolean success = orderService.receiveOrder(orderId);
            
            JsonObject response = new JsonObject();
            if (success) {
                response.addProperty("success", true);
                response.addProperty("message", "Order marked as received successfully");
                
                // Return updated order
                Order updatedOrder = orderService.findById(orderId);
                if (updatedOrder != null) {
                    response.add("order", orderToJson(updatedOrder));
                }
            } else {
                response.addProperty("success", false);
                response.addProperty("message", "Failed to receive order. Order may not be in delivered status.");
                return ResponseEntity.badRequest().body(response.toString());
            }

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error receiving order: " + e.getMessage());
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Error receiving order");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    /**
     * Cancel order
     * PUT /api/orders/{orderId}/cancel
     */
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(@PathVariable String orderId, @RequestBody Map<String, String> requestData) {
        try {
            System.out.println("Cancelling order: " + orderId);

            String reason = requestData.get("reason");
            boolean success = orderService.cancelOrder(orderId, reason);
            
            JsonObject response = new JsonObject();
            if (success) {
                response.addProperty("success", true);
                response.addProperty("message", "Order cancelled successfully");
                
                // Return updated order
                Order updatedOrder = orderService.findById(orderId);
                if (updatedOrder != null) {
                    response.add("order", orderToJson(updatedOrder));
                }
            } else {
                response.addProperty("success", false);
                response.addProperty("message", "Failed to cancel order. Order may already be delivered or completed.");
                return ResponseEntity.badRequest().body(response.toString());
            }

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error cancelling order: " + e.getMessage());
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Error cancelling order");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    /**
     * Get order statistics (for admin)
     * GET /api/orders/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<String> getOrderStatistics() {
        try {
            System.out.println("Getting order statistics");

            Map<String, Object> stats = orderService.getOrderStatistics();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Statistics retrieved successfully");
            
            // Convert stats to JSON
            JsonObject statsJson = new JsonObject();
            for (Map.Entry<String, Object> entry : stats.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    statsJson.addProperty(entry.getKey(), (Number) entry.getValue());
                }
            }
            response.add("statistics", statsJson);

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            System.out.println("Error getting order statistics: " + e.getMessage());
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("message", "Error retrieving statistics");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.toString());
        }
    }

    // Helper method to convert Order to JSON
    private JsonObject orderToJson(Order order) {
        JsonObject orderJson = new JsonObject();
        orderJson.addProperty("id", order.getId().toString());
        orderJson.addProperty("customerId", order.getCustomerId().toString());
        orderJson.addProperty("farmerId", order.getFarmerId().toString());
        orderJson.addProperty("status", order.getStatus());
        orderJson.addProperty("totalAmount", order.getTotalAmount());
        orderJson.addProperty("deliveryAddress", order.getDeliveryAddress());
        orderJson.addProperty("customerNotes", order.getCustomerNotes());
        orderJson.addProperty("farmerNotes", order.getFarmerNotes());
        orderJson.addProperty("customerName", order.getCustomerName());
        orderJson.addProperty("customerPhone", order.getCustomerPhone());
        orderJson.addProperty("farmerName", order.getFarmerName());
        orderJson.addProperty("farmerPhone", order.getFarmerPhone());
        orderJson.addProperty("farmerLocation", order.getFarmerLocation());
        orderJson.addProperty("totalItems", order.getTotalItems());

        // Add dates
        if (order.getOrderDate() != null) {
            orderJson.addProperty("orderDate", dateFormat.format(order.getOrderDate()));
        }
        if (order.getConfirmedDate() != null) {
            orderJson.addProperty("confirmedDate", dateFormat.format(order.getConfirmedDate()));
        }
        if (order.getDeliveredDate() != null) {
            orderJson.addProperty("deliveredDate", dateFormat.format(order.getDeliveredDate()));
        }
        if (order.getReceivedDate() != null) {
            orderJson.addProperty("receivedDate", dateFormat.format(order.getReceivedDate()));
        }
        if (order.getCancelledDate() != null) {
            orderJson.addProperty("cancelledDate", dateFormat.format(order.getCancelledDate()));
        }

        // Add products
        JsonObject productsJson = new JsonObject();
        if (order.getProducts() != null) {
            for (Map.Entry<String, Integer> entry : order.getProducts().entrySet()) {
                productsJson.addProperty(entry.getKey(), entry.getValue());
            }
        }
        orderJson.add("products", productsJson);

        // Add product display names
        JsonArray productDetailsArray = new JsonArray();
        if (order.getProducts() != null) {
            for (Map.Entry<String, Integer> entry : order.getProducts().entrySet()) {
                JsonObject productDetail = new JsonObject();
                productDetail.addProperty("type", entry.getKey());
                productDetail.addProperty("name", Order.getProductDisplayName(entry.getKey()));
                productDetail.addProperty("quantity", entry.getValue());
                productDetailsArray.add(productDetail);
            }
        }
        orderJson.add("productDetails", productDetailsArray);

        return orderJson;
    }
}