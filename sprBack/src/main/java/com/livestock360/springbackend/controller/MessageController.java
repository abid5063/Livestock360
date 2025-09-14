package com.livestock360.springbackend.controller;

import com.livestock360.springbackend.model.Message;
import com.livestock360.springbackend.service.MessageService;
import com.livestock360.springbackend.service.FarmerService;
import com.livestock360.springbackend.service.VetService;
import com.livestock360.springbackend.service.CustomerService;
import com.livestock360.springbackend.utils.JwtUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.Date;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageController {

    private final MessageService messageService;
    private final FarmerService farmerService;
    private final VetService vetService;
    private final CustomerService customerService;
    private final Gson gson = new Gson();

    @Autowired
    public MessageController(MessageService messageService, FarmerService farmerService, VetService vetService, CustomerService customerService) {
        this.messageService = messageService;
        this.farmerService = farmerService;
        this.vetService = vetService;
        this.customerService = customerService;
    }

    // POST /api/messages - Send a message
    @PostMapping
    public ResponseEntity<String> sendMessage(@RequestBody String messageData, @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            String userType = JwtUtil.getUserTypeFromToken(token);
            
            if (userId == null || userType == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(gson.toJson(error));
            }

            JsonObject requestData = gson.fromJson(messageData, JsonObject.class);
            
            // Validate required fields
            if (!requestData.has("receiverId") || !requestData.has("receiverType") || !requestData.has("content")) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Missing required fields: receiverId, receiverType, content");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(gson.toJson(error));
            }

            String receiverId = requestData.get("receiverId").getAsString();
            String receiverType = requestData.get("receiverType").getAsString();
            String content = requestData.get("content").getAsString();
            
            // Validate receiver exists
            if ("farmer".equals(receiverType)) {
                if (farmerService.findById(receiverId) == null) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Receiver not found");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(error));
                }
            } else if ("vet".equals(receiverType)) {
                if (vetService.findById(receiverId) == null) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Receiver not found");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(error));
                }
            } else if ("customer".equals(receiverType)) {
                if (customerService.findById(receiverId) == null) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Receiver not found");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(error));
                }
            } else {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Invalid receiver type");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(gson.toJson(error));
            }

            // Create message (exactly like SimpleBackend)
            Message message = new Message();
            message.setSenderId(new ObjectId(userId));
            message.setSenderType(userType);
            message.setReceiverId(new ObjectId(receiverId));
            message.setReceiverType(receiverType);
            message.setContent(content.trim());
            
            // Set optional fields with defaults (exactly like SimpleBackend)
            String messageType = requestData.has("messageType") ? 
                requestData.get("messageType").getAsString() : "text";
            String priority = requestData.has("priority") ? 
                requestData.get("priority").getAsString() : "normal";
            
            message.setMessageType(messageType);
            message.setPriority(priority);
            
            // Set default values (exactly like SimpleBackend)
            message.setIsRead(false);
            message.setIsDeleted(false);
            message.setCreatedAt(new Date());
            message.setUpdatedAt(new Date());
            
            // Generate conversation ID
            String conversationId = messageService.generateConversationId(userId, receiverId);
            message.setConversationId(conversationId);
            
            Message savedMessage = messageService.save(message);
            
            if (savedMessage != null) {
                // Get sender and receiver info for response (exactly like SimpleBackend)
                Map<String, Object> senderData = new HashMap<>();
                Map<String, Object> receiverData = new HashMap<>();
                
                if ("farmer".equals(userType)) {
                    var farmer = farmerService.findById(userId);
                    if (farmer != null) {
                        senderData.put("name", farmer.getName());
                        senderData.put("email", farmer.getEmail());
                    }
                } else if ("vet".equals(userType)) {
                    var vet = vetService.findById(userId);
                    if (vet != null) {
                        senderData.put("name", vet.getName());
                        senderData.put("email", vet.getEmail());
                        senderData.put("specialty", vet.getSpecialty());
                    }
                } else if ("customer".equals(userType)) {
                    var customer = customerService.findById(userId);
                    if (customer != null) {
                        senderData.put("name", customer.getName());
                        senderData.put("email", customer.getEmail());
                        senderData.put("customerType", customer.getCustomerType());
                    }
                }
                
                if ("farmer".equals(receiverType)) {
                    var farmer = farmerService.findById(receiverId);
                    if (farmer != null) {
                        receiverData.put("name", farmer.getName());
                        receiverData.put("email", farmer.getEmail());
                    }
                } else if ("vet".equals(receiverType)) {
                    var vet = vetService.findById(receiverId);
                    if (vet != null) {
                        receiverData.put("name", vet.getName());
                        receiverData.put("email", vet.getEmail());
                        receiverData.put("specialty", vet.getSpecialty());
                    }
                } else if ("customer".equals(receiverType)) {
                    var customer = customerService.findById(receiverId);
                    if (customer != null) {
                        receiverData.put("name", customer.getName());
                        receiverData.put("email", customer.getEmail());
                        receiverData.put("customerType", customer.getCustomerType());
                    }
                }
                
                // Build full message response data (exactly like SimpleBackend)
                Map<String, Object> responseMessage = new HashMap<>();
                responseMessage.put("_id", savedMessage.getId().toString());
                responseMessage.put("senderId", userId);
                responseMessage.put("senderType", userType);
                responseMessage.put("receiverId", receiverId);
                responseMessage.put("receiverType", receiverType);
                responseMessage.put("conversationId", conversationId);
                responseMessage.put("content", content.trim());
                responseMessage.put("messageType", savedMessage.getMessageType());
                responseMessage.put("priority", savedMessage.getPriority());
                responseMessage.put("isRead", false);
                responseMessage.put("createdAt", savedMessage.getCreatedAt());
                responseMessage.put("senderInfo", senderData);
                responseMessage.put("receiverInfo", receiverData);
                
                // Response format exactly like SimpleBackend
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Message sent successfully");
                response.put("data", responseMessage);
                
                return ResponseEntity.status(HttpStatus.CREATED).body(gson.toJson(response));
            } else {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Failed to send message");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
            }
            
        } catch (Exception e) {
            System.out.println("Error in sendMessage: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
        }
    }

    // GET /api/messages/conversation/{receiverId}/{receiverType} - Get conversation messages
    @GetMapping("/conversation/{receiverId}/{receiverType}")
    public ResponseEntity<String> getConversationMessages(
            @PathVariable String receiverId,
            @PathVariable String receiverType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            String userType = JwtUtil.getUserTypeFromToken(token);
            
            if (userId == null || userType == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(gson.toJson(error));
            }

            // Validate that the receiver exists (exactly like SimpleBackend)
            if ("farmer".equals(receiverType)) {
                if (farmerService.findById(receiverId) == null) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Receiver not found or invalid receiver type");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(error));
                }
            } else if ("vet".equals(receiverType)) {
                if (vetService.findById(receiverId) == null) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Receiver not found or invalid receiver type");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(error));
                }
            } else if ("customer".equals(receiverType)) {
                if (customerService.findById(receiverId) == null) {
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Receiver not found or invalid receiver type");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(error));
                }
            } else {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Receiver not found or invalid receiver type");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(error));
            }

            String conversationId = messageService.generateConversationId(userId, receiverId);
            List<Message> messages = messageService.getConversationMessages(conversationId, page, limit);
            
            // Mark messages as read for the current user (exactly like SimpleBackend)
            messageService.markMessagesAsRead(conversationId, userId, userType);
            
            // Convert messages to response format (exactly like SimpleBackend)
            List<Map<String, Object>> responseMessages = new ArrayList<>();
            for (Message msg : messages) {
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("_id", msg.getId().toString());
                messageData.put("senderId", msg.getSenderId().toString());
                messageData.put("senderType", msg.getSenderType());
                messageData.put("receiverId", msg.getReceiverId().toString());
                messageData.put("receiverType", msg.getReceiverType());
                messageData.put("content", msg.getContent());
                messageData.put("messageType", msg.getMessageType());
                messageData.put("isRead", msg.getIsRead());
                messageData.put("createdAt", msg.getCreatedAt());
                responseMessages.add(messageData);
            }
            
            // Prepare participant info (exactly like SimpleBackend)
            Map<String, Object> participant = new HashMap<>();
            if ("farmer".equals(receiverType)) {
                var farmer = farmerService.findById(receiverId);
                if (farmer != null) {
                    participant.put("name", farmer.getName());
                    participant.put("email", farmer.getEmail());
                    participant.put("phoneNo", farmer.getPhone());
                }
            } else if ("vet".equals(receiverType)) {
                var vet = vetService.findById(receiverId);
                if (vet != null) {
                    participant.put("name", vet.getName());
                    participant.put("email", vet.getEmail());
                    participant.put("phoneNo", vet.getPhoneNo());
                    participant.put("specialty", vet.getSpecialty());
                }
            } else if ("customer".equals(receiverType)) {
                var customer = customerService.findById(receiverId);
                if (customer != null) {
                    participant.put("name", customer.getName());
                    participant.put("email", customer.getEmail());
                    participant.put("phoneNo", customer.getPhone());
                    participant.put("customerType", customer.getCustomerType());
                }
            }
            
            // Response format exactly like SimpleBackend
            Map<String, Object> response = new HashMap<>();
            response.put("messages", responseMessages);
            response.put("participant", participant);
            
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("current", page);
            pagination.put("limit", limit);
            pagination.put("hasMore", messages.size() == limit);
            response.put("pagination", pagination);
            
            return ResponseEntity.ok(gson.toJson(response));
            
        } catch (Exception e) {
            System.out.println("Error in getConversationMessages: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
        }
    }

    // GET /api/messages/conversations - Get user's conversations
    @GetMapping("/conversations")
    public ResponseEntity<String> getUserConversations(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            String userType = JwtUtil.getUserTypeFromToken(token);
            
            if (userId == null || userType == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(gson.toJson(error));
            }

            List<Map<String, Object>> conversations = messageService.getUserConversations(userId, userType, farmerService, vetService, customerService);
            
            // Response format exactly like SimpleBackend (no "success" property, just direct data)
            Map<String, Object> response = new HashMap<>();
            response.put("conversations", conversations);
            
            return ResponseEntity.ok(gson.toJson(response));
            
        } catch (Exception e) {
            System.out.println("Error in getUserConversations: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
        }
    }

    // GET /api/messages/vet - Get messages for vet
    @GetMapping("/vet")
    public ResponseEntity<String> getVetMessages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            String userType = JwtUtil.getUserTypeFromToken(token);
            
            if (userId == null || !"vet".equals(userType)) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Unauthorized - Vet access required");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(gson.toJson(error));
            }

            List<Message> messages = messageService.getMessagesForUserType(userId, userType, page, limit);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("page", page);
            response.addProperty("limit", limit);
            response.addProperty("totalMessages", messages.size());
            response.add("messages", gson.toJsonTree(messages));
            
            return ResponseEntity.ok(gson.toJson(response));
            
        } catch (Exception e) {
            System.out.println("Error in getVetMessages: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
        }
    }

    // GET /api/messages/farmer - Get messages for farmer
    @GetMapping("/farmer")
    public ResponseEntity<String> getFarmerMessages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            String userType = JwtUtil.getUserTypeFromToken(token);
            
            if (userId == null || !"farmer".equals(userType)) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Unauthorized - Farmer access required");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(gson.toJson(error));
            }

            List<Message> messages = messageService.getMessagesForUserType(userId, userType, page, limit);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("page", page);
            response.addProperty("limit", limit);
            response.addProperty("totalMessages", messages.size());
            response.add("messages", gson.toJsonTree(messages));
            
            return ResponseEntity.ok(gson.toJson(response));
            
        } catch (Exception e) {
            System.out.println("Error in getFarmerMessages: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
        }
    }

    // PUT /api/messages/{messageId}/read - Mark message as read
    @PutMapping("/{messageId}/read")
    public ResponseEntity<String> markMessageAsRead(
            @PathVariable String messageId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            String userType = JwtUtil.getUserTypeFromToken(token);
            
            if (userId == null || userType == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(gson.toJson(error));
            }

            boolean success = messageService.markMessageAsRead(messageId, userId, userType);
            
            if (success) {
                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("message", "Message marked as read");
                return ResponseEntity.ok(gson.toJson(response));
            } else {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Message not found or not authorized");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(error));
            }
            
        } catch (Exception e) {
            System.out.println("Error in markMessageAsRead: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
        }
    }

    // DELETE /api/messages/{messageId} - Delete message
    @DeleteMapping("/{messageId}")
    public ResponseEntity<String> deleteMessage(
            @PathVariable String messageId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            String userType = JwtUtil.getUserTypeFromToken(token);
            
            if (userId == null || userType == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(gson.toJson(error));
            }

            boolean success = messageService.deleteMessage(messageId, userId, userType);
            
            if (success) {
                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("message", "Message deleted successfully");
                return ResponseEntity.ok(gson.toJson(response));
            } else {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Message not found or not authorized to delete");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(error));
            }
            
        } catch (Exception e) {
            System.out.println("Error in deleteMessage: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
        }
    }

    // GET /api/messages/unread-count - Get unread message count
    @GetMapping("/unread-count")
    public ResponseEntity<String> getUnreadCount(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            String userType = JwtUtil.getUserTypeFromToken(token);
            
            if (userId == null || userType == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(gson.toJson(error));
            }

            long unreadCount = messageService.getUnreadCount(userId, userType);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("unreadCount", unreadCount);
            
            return ResponseEntity.ok(gson.toJson(response));
            
        } catch (Exception e) {
            System.out.println("Error in getUnreadCount: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
        }
    }

    // GET /api/messages/search - Search messages
    @GetMapping("/search")
    public ResponseEntity<String> searchMessages(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = JwtUtil.getUserIdFromToken(token);
            String userType = JwtUtil.getUserTypeFromToken(token);
            
            if (userId == null || userType == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(gson.toJson(error));
            }

            if (q == null || q.trim().isEmpty()) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Search term is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(gson.toJson(error));
            }

            List<Message> messages = messageService.searchMessages(userId, userType, q.trim(), page, limit);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("searchTerm", q.trim());
            response.addProperty("page", page);
            response.addProperty("limit", limit);
            response.addProperty("totalResults", messages.size());
            response.add("messages", gson.toJsonTree(messages));
            
            return ResponseEntity.ok(gson.toJson(response));
            
        } catch (Exception e) {
            System.out.println("Error in searchMessages: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(error));
        }
    }
}