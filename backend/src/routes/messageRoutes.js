import express from "express";
import jwt from "jsonwebtoken";
import { Message } from "../models/Message.js";
import Farmer from "../models/Farmer.js";
import { Vet } from "../models/Vet.js";

const router = express.Router();

// Middleware to verify token
const verifyToken = (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ message: "Access denied. No token provided." });
  }

  try {
    const token = authHeader.substring(7);
    const decoded = jwt.verify(token, process.env.JWT_SECRET || "your_jwt_secret_key");
    req.user = decoded;
    next();
  } catch (error) {
    console.error("Token verification error:", error);
    res.status(401).json({ message: "Invalid token" });
  }
};

// Send a message
router.post("/", verifyToken, async (req, res) => {
  try {
    const { receiverId, receiverType, content, messageType = 'text', attachments, relatedAppointment, priority = 'normal' } = req.body;

    // Validation
    if (!receiverId || !receiverType || !content) {
      return res.status(400).json({ 
        message: "Receiver ID, receiver type, and content are required" 
      });
    }

    if (!['farmer', 'vet'].includes(receiverType)) {
      return res.status(400).json({ message: "Invalid receiver type" });
    }

    // Verify receiver exists
    let receiver;
    if (receiverType === 'farmer') {
      receiver = await Farmer.findById(receiverId);
    } else {
      receiver = await Vet.findById(receiverId);
    }

    if (!receiver) {
      return res.status(404).json({ message: "Receiver not found" });
    }

    // Get sender info
    let senderId, senderType;
    
    if (req.user.userType === 'farmer') {
      senderId = req.user.farmerId;
      senderType = 'farmer';
    } else if (req.user.userType === 'vet') {
      senderId = req.user.vetId;
      senderType = 'vet';
    } else {
      // Fallback for old farmer tokens without userType
      senderId = req.user.farmerId;
      senderType = 'farmer';
    }

    if (!senderId || !senderType) {
      return res.status(400).json({ message: "Invalid authentication token" });
    }

    // Generate conversation ID (consistent for both participants)
    const participantIds = [senderId, receiverId].sort();
    const conversationId = `${participantIds[0]}_${participantIds[1]}`;

    // Create message
    const message = new Message({
      senderId,
      senderType,
      receiverId,
      receiverType,
      conversationId,
      content: content.trim(),
      messageType,
      attachments: attachments || [],
      relatedAppointment,
      priority
    });

    await message.save();

    // Populate sender and receiver info for response
    let senderInfo, receiverInfo;
    if (senderType === 'farmer') {
      senderInfo = await Farmer.findById(senderId).select('name email');
    } else {
      senderInfo = await Vet.findById(senderId).select('name specialty email');
    }

    if (receiverType === 'farmer') {
      receiverInfo = await Farmer.findById(receiverId).select('name email');
    } else {
      receiverInfo = await Vet.findById(receiverId).select('name specialty email');
    }

    const responseMessage = {
      ...message.toObject(),
      senderInfo,
      receiverInfo
    };

    res.status(201).json({
      message: "Message sent successfully",
      data: responseMessage
    });

  } catch (error) {
    console.error("Send message error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Get conversation between two users
router.get("/conversation/:receiverId/:receiverType", verifyToken, async (req, res) => {
  try {
    const { receiverId, receiverType } = req.params;
    const { page = 1, limit = 50 } = req.query;

    console.log("Getting conversation:", { receiverId, receiverType, userToken: req.user });

    // Get sender info
    let senderId, senderType;
    
    if (req.user.userType === 'farmer') {
      senderId = req.user.farmerId;
      senderType = 'farmer';
    } else if (req.user.userType === 'vet') {
      senderId = req.user.vetId;
      senderType = 'vet';
    } else {
      // Fallback for old farmer tokens without userType
      senderId = req.user.farmerId;
      senderType = 'farmer';
    }

    if (!senderId || !senderType) {
      return res.status(400).json({ message: "Invalid authentication token" });
    }

    console.log("Conversation participants:", { senderId, senderType, receiverId, receiverType });

    // Get conversation messages
    const messages = await Message.getConversation(
      senderId, 
      senderType, 
      receiverId, 
      receiverType, 
      parseInt(page), 
      parseInt(limit)
    );

    // Mark messages as read (messages received by current user)
    await Message.markConversationAsRead(
      messages.length > 0 ? messages[0].conversationId : null,
      senderId,
      senderType
    );

    // Get participant info
    let participantInfo;
    if (receiverType === 'farmer') {
      participantInfo = await Farmer.findById(receiverId).select('name email phoneNo');
    } else {
      participantInfo = await Vet.findById(receiverId).select('name specialty email phoneNo');
    }

    res.json({
      messages: messages.reverse(), // Reverse to show oldest first
      participant: participantInfo,
      pagination: {
        current: parseInt(page),
        limit: parseInt(limit),
        hasMore: messages.length === parseInt(limit)
      }
    });

  } catch (error) {
    console.error("Get conversation error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Get user's conversations list
router.get("/conversations", verifyToken, async (req, res) => {
  try {
    // Get user info
    let userId, userType;
    
    if (req.user.userType === 'farmer') {
      userId = req.user.farmerId;
      userType = 'farmer';
    } else if (req.user.userType === 'vet') {
      userId = req.user.vetId;
      userType = 'vet';
    } else {
      // Fallback for old farmer tokens without userType
      userId = req.user.farmerId;
      userType = 'farmer';
    }

    if (!userId || !userType) {
      return res.status(400).json({ message: "Invalid authentication token" });
    }

    const conversations = await Message.getUserConversations(userId, userType);

    // Populate participant info for each conversation
    const conversationsWithParticipants = await Promise.all(
      conversations.map(async (conv) => {
        const lastMessage = conv.lastMessage;
        
        // Determine the other participant
        const isUserSender = lastMessage.senderId.toString() === userId;
        const otherUserId = isUserSender ? lastMessage.receiverId : lastMessage.senderId;
        const otherUserType = isUserSender ? lastMessage.receiverType : lastMessage.senderType;

        // Get participant info
        let participantInfo;
        if (otherUserType === 'farmer') {
          participantInfo = await Farmer.findById(otherUserId).select('name email phoneNo');
        } else {
          participantInfo = await Vet.findById(otherUserId).select('name specialty email phoneNo');
        }

        return {
          conversationId: conv._id,
          participant: participantInfo,
          lastMessage: {
            content: lastMessage.content,
            timestamp: lastMessage.createdAt,
            isFromUser: isUserSender,
            messageType: lastMessage.messageType
          },
          unreadCount: conv.unreadCount,
          totalMessages: conv.totalMessages
        };
      })
    );

    res.json({
      conversations: conversationsWithParticipants
    });

  } catch (error) {
    console.error("Get conversations error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Get messages for vet dashboard (recent unread messages)
router.get("/vet", verifyToken, async (req, res) => {
  try {
    if (req.user.userType !== 'vet') {
      return res.status(403).json({ message: "Access denied" });
    }

    const { limit = 10 } = req.query;
    
    // Get recent unread messages for the vet
    const messages = await Message.find({
      receiverId: req.user.vetId,
      receiverType: 'vet',
      isRead: false,
      isDeleted: false
    })
    .sort({ createdAt: -1 })
    .limit(parseInt(limit))
    .populate('senderId', 'name email phoneNo');

    // Transform messages for dashboard
    const dashboardMessages = messages.map(message => ({
      id: message._id,
      farmerName: message.senderId.name,
      content: message.content,
      timestamp: message.createdAt,
      read: message.isRead,
      priority: message.priority
    }));

    res.json(dashboardMessages);

  } catch (error) {
    console.error("Get vet messages error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Get messages for farmer dashboard (recent unread messages)
router.get("/farmer", verifyToken, async (req, res) => {
  try {
    if (req.user.userType !== 'farmer') {
      return res.status(403).json({ message: "Access denied" });
    }

    const { limit = 10 } = req.query;
    
    // Get recent unread messages for the farmer
    const messages = await Message.find({
      receiverId: req.user.farmerId,
      receiverType: 'farmer',
      isRead: false,
      isDeleted: false
    })
    .sort({ createdAt: -1 })
    .limit(parseInt(limit))
    .populate('senderId', 'name specialty email phoneNo');

    // Transform messages for dashboard
    const dashboardMessages = messages.map(message => ({
      id: message._id,
      vetName: message.senderId.name,
      vetSpecialty: message.senderId.specialty,
      content: message.content,
      timestamp: message.createdAt,
      read: message.isRead,
      priority: message.priority
    }));

    res.json(dashboardMessages);

  } catch (error) {
    console.error("Get farmer messages error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Mark message as read
router.put("/:messageId/read", verifyToken, async (req, res) => {
  try {
    const message = await Message.findById(req.params.messageId);
    
    if (!message) {
      return res.status(404).json({ message: "Message not found" });
    }

    const userId = req.user.userType === 'farmer' ? req.user.farmerId : req.user.vetId;
    
    // Check if user is the receiver of this message
    if (message.receiverId.toString() !== userId || message.receiverType !== req.user.userType) {
      return res.status(403).json({ message: "Access denied" });
    }

    await message.markAsRead();

    res.json({ message: "Message marked as read" });

  } catch (error) {
    console.error("Mark message as read error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Delete message
router.delete("/:messageId", verifyToken, async (req, res) => {
  try {
    const message = await Message.findById(req.params.messageId);
    
    if (!message) {
      return res.status(404).json({ message: "Message not found" });
    }

    const userId = req.user.userType === 'farmer' ? req.user.farmerId : req.user.vetId;
    
    // Check if user is the sender of this message
    if (message.senderId.toString() !== userId || message.senderType !== req.user.userType) {
      return res.status(403).json({ message: "Access denied. You can only delete your own messages." });
    }

    await message.softDelete(userId);

    res.json({ message: "Message deleted successfully" });

  } catch (error) {
    console.error("Delete message error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Get unread message count
router.get("/unread-count", verifyToken, async (req, res) => {
  try {
    const userId = req.user.userType === 'farmer' ? req.user.farmerId : req.user.vetId;
    const userType = req.user.userType;

    const unreadCount = await Message.getUnreadCount(userId, userType);

    res.json({ unreadCount });

  } catch (error) {
    console.error("Get unread count error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Search messages
router.get("/search", verifyToken, async (req, res) => {
  try {
    const { q, type, page = 1, limit = 20 } = req.query;
    
    if (!q) {
      return res.status(400).json({ message: "Search query is required" });
    }

    const userId = req.user.userType === 'farmer' ? req.user.farmerId : req.user.vetId;
    const userType = req.user.userType;

    const searchQuery = {
      $or: [
        { senderId: userId, senderType: userType },
        { receiverId: userId, receiverType: userType }
      ],
      content: { $regex: q, $options: 'i' },
      isDeleted: false
    };

    if (type) {
      searchQuery.messageType = type;
    }

    const messages = await Message.find(searchQuery)
      .sort({ createdAt: -1 })
      .limit(parseInt(limit))
      .skip((parseInt(page) - 1) * parseInt(limit))
      .populate('relatedAppointment', 'appointmentType scheduledDate');

    const total = await Message.countDocuments(searchQuery);

    res.json({
      messages,
      pagination: {
        current: parseInt(page),
        pages: Math.ceil(total / parseInt(limit)),
        total
      }
    });

  } catch (error) {
    console.error("Search messages error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

export default router;
