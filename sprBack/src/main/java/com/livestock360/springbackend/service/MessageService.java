package com.livestock360.springbackend.service;

import com.livestock360.springbackend.model.Message;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MongoDatabase database;

    @Autowired
    public MessageService(MongoDatabase database) {
        this.database = database;
    }

    private MongoCollection<Document> getMessagesCollection() {
        return database.getCollection("messages");
    }

    // Save a message
    public Message save(Message message) {
        try {
            MongoCollection<Document> collection = getMessagesCollection();
            Document doc = messageToDocument(message);
            
            if (message.getId() == null) {
                // Insert new message
                collection.insertOne(doc);
                message.setId(doc.getObjectId("_id"));
            } else {
                // Update existing message
                collection.replaceOne(
                    Filters.eq("_id", message.getId()),
                    doc
                );
            }
            
            return message;
        } catch (Exception e) {
            System.out.println("Error saving message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Find message by ID
    public Message findById(String id) {
        try {
            MongoCollection<Document> collection = getMessagesCollection();
            Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
            
            if (doc != null) {
                return documentToMessage(doc);
            }
            return null;
        } catch (Exception e) {
            System.out.println("Error finding message by ID: " + e.getMessage());
            return null;
        }
    }

    // Get conversation messages
    public List<Message> getConversationMessages(String conversationId, int page, int limit) {
        try {
            MongoCollection<Document> collection = getMessagesCollection();
            List<Document> docs = collection.find(
                Filters.and(
                    Filters.eq("conversationId", conversationId),
                    Filters.eq("isDeleted", false)
                ))
                .sort(Sorts.descending("createdAt"))
                .limit(limit)
                .skip((page - 1) * limit)
                .into(new ArrayList<>());
            
            return docs.stream()
                .map(this::documentToMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Error getting conversation messages: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Get user's conversations - format exactly like SimpleBackend
    public List<Map<String, Object>> getUserConversations(String userId, String userType, 
            FarmerService farmerService, VetService vetService, CustomerService customerService) {
        try {
            MongoCollection<Document> collection = getMessagesCollection();
            
            // Aggregation pipeline to get latest message per conversation (exactly like SimpleBackend)
            List<Document> pipeline = Arrays.asList(
                new Document("$match", new Document("$or", Arrays.asList(
                    new Document("senderId", new ObjectId(userId)).append("senderType", userType),
                    new Document("receiverId", new ObjectId(userId)).append("receiverType", userType)
                )).append("isDeleted", false)),
                new Document("$sort", new Document("createdAt", -1)),
                new Document("$group", new Document("_id", "$conversationId")
                    .append("lastMessage", new Document("$first", "$$ROOT"))
                    .append("totalMessages", new Document("$sum", 1))
                    .append("unreadCount", new Document("$sum", 
                        new Document("$cond", Arrays.asList(
                            new Document("$and", Arrays.asList(
                                new Document("$eq", Arrays.asList("$receiverId", new ObjectId(userId))),
                                new Document("$eq", Arrays.asList("$receiverType", userType)),
                                new Document("$eq", Arrays.asList("$isRead", false))
                            )),
                            1,
                            0
                        ))
                    ))
                ),
                new Document("$sort", new Document("lastMessage.createdAt", -1))
            );
            
            List<Document> results = collection.aggregate(pipeline).into(new ArrayList<>());
            List<Map<String, Object>> responseConversations = new ArrayList<>();
            
            // Process conversations and get participant info (exactly like SimpleBackend)
            for (Document conv : results) {
                Document lastMessage = (Document) conv.get("lastMessage");
                
                // Determine the other participant
                boolean isUserSender = lastMessage.getObjectId("senderId").toString().equals(userId);
                String otherUserId = isUserSender ? 
                    lastMessage.getObjectId("receiverId").toString() : 
                    lastMessage.getObjectId("senderId").toString();
                String otherUserType = isUserSender ? 
                    lastMessage.getString("receiverType") : 
                    lastMessage.getString("senderType");
                
                // Skip if same user (safety check)
                if (otherUserId.equals(userId)) {
                    continue;
                }
                
                // Get participant info (exactly like SimpleBackend)
                Map<String, Object> participant = new HashMap<>();
                if ("farmer".equals(otherUserType)) {
                    var farmer = farmerService.findById(otherUserId);
                    if (farmer != null) {
                        participant.put("id", otherUserId);
                        participant.put("name", farmer.getName());
                        participant.put("email", farmer.getEmail());
                        participant.put("phoneNo", farmer.getPhone());
                        participant.put("type", otherUserType);
                    }
                } else if ("vet".equals(otherUserType)) {
                    var vet = vetService.findById(otherUserId);
                    if (vet != null) {
                        participant.put("id", otherUserId);
                        participant.put("name", vet.getName());
                        participant.put("email", vet.getEmail());
                        participant.put("phoneNo", vet.getPhoneNo());
                        participant.put("specialty", vet.getSpecialty());
                        participant.put("type", otherUserType);
                    }
                } else if ("customer".equals(otherUserType)) {
                    var customer = customerService.findById(otherUserId);
                    if (customer != null) {
                        participant.put("id", otherUserId);
                        participant.put("name", customer.getName());
                        participant.put("email", customer.getEmail());
                        participant.put("phoneNo", customer.getPhone());
                        participant.put("customerType", customer.getCustomerType());
                        participant.put("type", otherUserType);
                    }
                }
                
                // Only add conversation if participant info was found
                if (!participant.isEmpty()) {
                    Map<String, Object> lastMsg = new HashMap<>();
                    lastMsg.put("content", lastMessage.getString("content"));
                    lastMsg.put("timestamp", lastMessage.getDate("createdAt"));
                    lastMsg.put("isFromUser", isUserSender);
                    lastMsg.put("messageType", lastMessage.getString("messageType"));
                    
                    Map<String, Object> conversation = new HashMap<>();
                    conversation.put("conversationId", conv.getString("_id"));
                    conversation.put("participant", participant);
                    conversation.put("lastMessage", lastMsg);
                    conversation.put("unreadCount", conv.getInteger("unreadCount"));
                    conversation.put("totalMessages", conv.getInteger("totalMessages"));
                    
                    responseConversations.add(conversation);
                }
            }
            
            return responseConversations;
        } catch (Exception e) {
            System.out.println("Error getting user conversations: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Get messages for a user type (vet or farmer)
    public List<Message> getMessagesForUserType(String userId, String userType, int page, int limit) {
        try {
            MongoCollection<Document> collection = getMessagesCollection();
            List<Document> docs = collection.find(
                Filters.and(
                    Filters.or(
                        Filters.and(
                            Filters.eq("senderId", new ObjectId(userId)),
                            Filters.eq("senderType", userType)
                        ),
                        Filters.and(
                            Filters.eq("receiverId", new ObjectId(userId)),
                            Filters.eq("receiverType", userType)
                        )
                    ),
                    Filters.eq("isDeleted", false)
                ))
                .sort(Sorts.descending("createdAt"))
                .limit(limit)
                .skip((page - 1) * limit)
                .into(new ArrayList<>());
            
            return docs.stream()
                .map(this::documentToMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Error getting messages for user type: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Mark messages as read
    public void markMessagesAsRead(String conversationId, String userId, String userType) {
        try {
            MongoCollection<Document> collection = getMessagesCollection();
            collection.updateMany(
                Filters.and(
                    Filters.eq("conversationId", conversationId),
                    Filters.eq("receiverId", new ObjectId(userId)),
                    Filters.eq("receiverType", userType),
                    Filters.eq("isRead", false)
                ),
                new Document("$set", new Document("isRead", true).append("updatedAt", new Date()))
            );
        } catch (Exception e) {
            System.out.println("Error marking messages as read: " + e.getMessage());
        }
    }

    // Mark single message as read
    public boolean markMessageAsRead(String messageId, String userId, String userType) {
        try {
            MongoCollection<Document> collection = getMessagesCollection();
            long updated = collection.updateOne(
                Filters.and(
                    Filters.eq("_id", new ObjectId(messageId)),
                    Filters.eq("receiverId", new ObjectId(userId)),
                    Filters.eq("receiverType", userType)
                ),
                new Document("$set", new Document("isRead", true).append("updatedAt", new Date()))
            ).getModifiedCount();
            
            return updated > 0;
        } catch (Exception e) {
            System.out.println("Error marking message as read: " + e.getMessage());
            return false;
        }
    }

    // Delete message
    public boolean deleteMessage(String messageId, String userId, String userType) {
        try {
            MongoCollection<Document> collection = getMessagesCollection();
            
            // Can only delete if user is the sender
            long deleted = collection.updateOne(
                Filters.and(
                    Filters.eq("_id", new ObjectId(messageId)),
                    Filters.eq("senderId", new ObjectId(userId)),
                    Filters.eq("senderType", userType)
                ),
                new Document("$set", new Document("isDeleted", true).append("updatedAt", new Date()))
            ).getModifiedCount();
            
            return deleted > 0;
        } catch (Exception e) {
            System.out.println("Error deleting message: " + e.getMessage());
            return false;
        }
    }

    // Get unread count for user
    public long getUnreadCount(String userId, String userType) {
        try {
            MongoCollection<Document> collection = getMessagesCollection();
            return collection.countDocuments(
                Filters.and(
                    Filters.eq("receiverId", new ObjectId(userId)),
                    Filters.eq("receiverType", userType),
                    Filters.eq("isRead", false),
                    Filters.eq("isDeleted", false)
                )
            );
        } catch (Exception e) {
            System.out.println("Error getting unread count: " + e.getMessage());
            return 0;
        }
    }

    // Search messages
    public List<Message> searchMessages(String userId, String userType, String searchTerm, int page, int limit) {
        try {
            MongoCollection<Document> collection = getMessagesCollection();
            List<Document> docs = collection.find(
                Filters.and(
                    Filters.or(
                        Filters.and(
                            Filters.eq("senderId", new ObjectId(userId)),
                            Filters.eq("senderType", userType)
                        ),
                        Filters.and(
                            Filters.eq("receiverId", new ObjectId(userId)),
                            Filters.eq("receiverType", userType)
                        )
                    ),
                    Filters.regex("content", ".*" + searchTerm + ".*", "i"),
                    Filters.eq("isDeleted", false)
                ))
                .sort(Sorts.descending("createdAt"))
                .limit(limit)
                .skip((page - 1) * limit)
                .into(new ArrayList<>());
            
            return docs.stream()
                .map(this::documentToMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Error searching messages: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Generate conversation ID from participant IDs
    public String generateConversationId(String userId1, String userId2) {
        List<String> participantIds = Arrays.asList(userId1, userId2);
        Collections.sort(participantIds);
        return participantIds.get(0) + "_" + participantIds.get(1);
    }

    // Convert Document to Message
    private Message documentToMessage(Document doc) {
        try {
            Message message = new Message();
            message.setId(doc.getObjectId("_id"));
            message.setSenderId(doc.getObjectId("senderId"));
            message.setSenderType(doc.getString("senderType"));
            message.setReceiverId(doc.getObjectId("receiverId"));
            message.setReceiverType(doc.getString("receiverType"));
            message.setConversationId(doc.getString("conversationId"));
            message.setContent(doc.getString("content"));
            message.setMessageType(doc.getString("messageType"));
            message.setPriority(doc.getString("priority"));
            message.setIsRead(doc.getBoolean("isRead"));
            message.setIsDeleted(doc.getBoolean("isDeleted"));
            message.setCreatedAt(doc.getDate("createdAt"));
            message.setUpdatedAt(doc.getDate("updatedAt"));
            return message;
        } catch (Exception e) {
            System.out.println("Error converting document to message: " + e.getMessage());
            return null;
        }
    }

    // Convert Message to Document
    private Document messageToDocument(Message message) {
        Document doc = new Document();
        if (message.getId() != null) {
            doc.append("_id", message.getId());
        }
        doc.append("senderId", message.getSenderId())
           .append("senderType", message.getSenderType())
           .append("receiverId", message.getReceiverId())
           .append("receiverType", message.getReceiverType())
           .append("conversationId", message.getConversationId())
           .append("content", message.getContent())
           .append("messageType", message.getMessageType())
           .append("priority", message.getPriority())
           .append("isRead", message.getIsRead())
           .append("isDeleted", message.getIsDeleted())
           .append("createdAt", message.getCreatedAt())
           .append("updatedAt", message.getUpdatedAt());
        return doc;
    }
}