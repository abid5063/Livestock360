package com.livestock360.springbackend.model;

import org.bson.types.ObjectId;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

/**
 * Order model for product selling system
 * Handles orders between customers and farmers for dairy and egg products
 */
public class Order {
    private ObjectId _id;
    private ObjectId customerId;
    private ObjectId farmerId;
    private Map<String, Integer> products; // Product type -> quantity ordered
    private String status; // PENDING, CONFIRMED, DELIVERED, RECEIVED, CANCELLED
    private Double totalAmount;
    private Date orderDate;
    private Date confirmedDate;
    private Date deliveredDate;
    private Date receivedDate;
    private Date cancelledDate;
    private String customerNotes;
    private String farmerNotes;
    private String deliveryAddress;
    private String customerPhone;
    private String paymentMethod;
    private Boolean isPaid;
    private String customerName;
    private String farmerName;
    private String farmerPhone;
    private String farmerLocation;
    
    // Order status constants
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_RECEIVED = "RECEIVED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    
    // Product type constants
    public static final String PRODUCT_MILK_COW = "MILK_COW";
    public static final String PRODUCT_MILK_BUFFALO = "MILK_BUFFALO";
    public static final String PRODUCT_MILK_GOAT = "MILK_GOAT";
    public static final String PRODUCT_BUTTER = "BUTTER";
    public static final String PRODUCT_HEN_EGGS = "HEN_EGGS";
    public static final String PRODUCT_DUCK_EGGS = "DUCK_EGGS";

    // Constructors
    public Order() {
        this.products = new HashMap<>();
        this.orderDate = new Date();
        this.status = STATUS_PENDING;
        this.isPaid = false;
        this.totalAmount = 0.0;
    }

    public Order(ObjectId customerId, ObjectId farmerId, Map<String, Integer> products, 
                String deliveryAddress, String customerNotes) {
        this();
        this.customerId = customerId;
        this.farmerId = farmerId;
        this.products = products != null ? products : new HashMap<>();
        this.deliveryAddress = deliveryAddress;
        this.customerNotes = customerNotes;
    }

    // Getters and Setters
    public ObjectId getId() {
        return _id;
    }

    public void setId(ObjectId _id) {
        this._id = _id;
    }

    public ObjectId getCustomerId() {
        return customerId;
    }

    public void setCustomerId(ObjectId customerId) {
        this.customerId = customerId;
    }

    public ObjectId getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(ObjectId farmerId) {
        this.farmerId = farmerId;
    }

    public Map<String, Integer> getProducts() {
        return products;
    }

    public void setProducts(Map<String, Integer> products) {
        this.products = products != null ? products : new HashMap<>();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public Date getConfirmedDate() {
        return confirmedDate;
    }

    public void setConfirmedDate(Date confirmedDate) {
        this.confirmedDate = confirmedDate;
    }

    public Date getDeliveredDate() {
        return deliveredDate;
    }

    public void setDeliveredDate(Date deliveredDate) {
        this.deliveredDate = deliveredDate;
    }

    public Date getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(Date receivedDate) {
        this.receivedDate = receivedDate;
    }

    public Date getCancelledDate() {
        return cancelledDate;
    }

    public void setCancelledDate(Date cancelledDate) {
        this.cancelledDate = cancelledDate;
    }

    public String getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }

    public String getFarmerNotes() {
        return farmerNotes;
    }

    public void setFarmerNotes(String farmerNotes) {
        this.farmerNotes = farmerNotes;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Boolean getIsPaid() {
        return isPaid;
    }

    public void setIsPaid(Boolean isPaid) {
        this.isPaid = isPaid;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }

    public String getFarmerPhone() {
        return farmerPhone;
    }

    public void setFarmerPhone(String farmerPhone) {
        this.farmerPhone = farmerPhone;
    }

    public String getFarmerLocation() {
        return farmerLocation;
    }

    public void setFarmerLocation(String farmerLocation) {
        this.farmerLocation = farmerLocation;
    }

    // Helper methods
    public void addProduct(String productType, Integer quantity) {
        if (productType != null && quantity != null && quantity > 0) {
            this.products.put(productType, quantity);
        }
    }

    public void removeProduct(String productType) {
        this.products.remove(productType);
    }

    public Integer getProductQuantity(String productType) {
        return this.products.getOrDefault(productType, 0);
    }

    public int getTotalItems() {
        return this.products.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean hasProduct(String productType) {
        return this.products.containsKey(productType) && this.products.get(productType) > 0;
    }

    public boolean canBeConfirmed() {
        return STATUS_PENDING.equals(this.status);
    }

    public boolean canBeDelivered() {
        return STATUS_CONFIRMED.equals(this.status);
    }

    public boolean canBeReceived() {
        return STATUS_DELIVERED.equals(this.status);
    }

    public boolean canBeCancelled() {
        return STATUS_PENDING.equals(this.status) || STATUS_CONFIRMED.equals(this.status);
    }

    public void confirmOrder() {
        if (canBeConfirmed()) {
            this.status = STATUS_CONFIRMED;
            this.confirmedDate = new Date();
        }
    }

    public void deliverOrder() {
        if (canBeDelivered()) {
            this.status = STATUS_DELIVERED;
            this.deliveredDate = new Date();
        }
    }

    public void receiveOrder() {
        if (canBeReceived()) {
            this.status = STATUS_RECEIVED;
            this.receivedDate = new Date();
        }
    }

    public void cancelOrder() {
        if (canBeCancelled()) {
            this.status = STATUS_CANCELLED;
            this.cancelledDate = new Date();
        }
    }

    // Product name helper methods
    public static String getProductDisplayName(String productType) {
        switch (productType) {
            case PRODUCT_MILK_COW:
                return "Cow Milk";
            case PRODUCT_MILK_BUFFALO:
                return "Buffalo Milk";
            case PRODUCT_MILK_GOAT:
                return "Goat Milk";
            case PRODUCT_BUTTER:
                return "Butter";
            case PRODUCT_HEN_EGGS:
                return "Hen Eggs";
            case PRODUCT_DUCK_EGGS:
                return "Duck Eggs";
            default:
                return productType;
        }
    }

    public static boolean isValidProductType(String productType) {
        return PRODUCT_MILK_COW.equals(productType) ||
               PRODUCT_MILK_BUFFALO.equals(productType) ||
               PRODUCT_MILK_GOAT.equals(productType) ||
               PRODUCT_BUTTER.equals(productType) ||
               PRODUCT_HEN_EGGS.equals(productType) ||
               PRODUCT_DUCK_EGGS.equals(productType);
    }

    @Override
    public String toString() {
        return "Order{" +
                "_id=" + _id +
                ", customerId=" + customerId +
                ", farmerId=" + farmerId +
                ", products=" + products +
                ", status='" + status + '\'' +
                ", totalAmount=" + totalAmount +
                ", orderDate=" + orderDate +
                ", totalItems=" + getTotalItems() +
                '}';
    }
}