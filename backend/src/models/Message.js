import mongoose from "mongoose";

const messageSchema = new mongoose.Schema({
  senderId: {
    type: mongoose.Schema.Types.ObjectId,
    required: [true, "Sender ID is required"]
  },
  senderType: {
    type: String,
    enum: ['farmer', 'vet'],
    required: [true, "Sender type is required"]
  },
  receiverId: {
    type: mongoose.Schema.Types.ObjectId,
    required: [true, "Receiver ID is required"]
  },
  receiverType: {
    type: String,
    enum: ['farmer', 'vet'],
    required: [true, "Receiver type is required"]
  },
  conversationId: {
    type: String,
    required: [true, "Conversation ID is required"],
    index: true
  },
  content: {
    type: String,
    required: [true, "Message content is required"],
    trim: true,
    maxlength: [2000, "Message content cannot exceed 2000 characters"]
  },
  messageType: {
    type: String,
    enum: ['text', 'image', 'file', 'appointment_update', 'system'],
    default: 'text'
  },
  attachments: [{
    type: {
      type: String,
      enum: ['image', 'document', 'video']
    },
    url: String,
    filename: String,
    size: Number,
    mimeType: String
  }],
  relatedAppointment: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Appointment'
  },
  isRead: {
    type: Boolean,
    default: false
  },
  readAt: Date,
  isEdited: {
    type: Boolean,
    default: false
  },
  editedAt: Date,
  originalContent: String, // Store original content if edited
  isDeleted: {
    type: Boolean,
    default: false
  },
  deletedAt: Date,
  deletedBy: {
    type: mongoose.Schema.Types.ObjectId
  },
  priority: {
    type: String,
    enum: ['low', 'normal', 'high', 'urgent'],
    default: 'normal'
  },
  metadata: {
    deliveryStatus: {
      type: String,
      enum: ['sent', 'delivered', 'failed'],
      default: 'sent'
    },
    deviceInfo: String,
    ipAddress: String
  }
}, {
  timestamps: true
});

// Indexes for efficient querying
messageSchema.index({ conversationId: 1, createdAt: -1 });
messageSchema.index({ senderId: 1, senderType: 1 });
messageSchema.index({ receiverId: 1, receiverType: 1 });
messageSchema.index({ isRead: 1, receiverId: 1 });
messageSchema.index({ createdAt: -1 });

// Compound index for conversation participants
messageSchema.index({ 
  senderId: 1, 
  receiverId: 1, 
  createdAt: -1 
});

// Virtual for formatted timestamp
messageSchema.virtual('formattedTimestamp').get(function() {
  return this.createdAt.toLocaleString();
});

// Virtual for time ago
messageSchema.virtual('timeAgo').get(function() {
  const now = new Date();
  const diff = now - this.createdAt;
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);
  
  if (minutes < 1) return 'Just now';
  if (minutes < 60) return `${minutes}m ago`;
  if (hours < 24) return `${hours}h ago`;
  if (days < 7) return `${days}d ago`;
  return this.createdAt.toLocaleDateString();
});

// Pre-save middleware
messageSchema.pre('save', function(next) {
  // Generate conversation ID if not exists
  if (!this.conversationId) {
    // Create a consistent conversation ID regardless of who sends first
    const ids = [
      `${this.senderType}_${this.senderId}`,
      `${this.receiverType}_${this.receiverId}`
    ].sort();
    this.conversationId = ids.join('_');
  }
  
  // Set read timestamp
  if (this.isModified('isRead') && this.isRead && !this.readAt) {
    this.readAt = new Date();
  }
  
  // Handle message editing
  if (this.isModified('content') && !this.isNew) {
    if (!this.isEdited) {
      this.originalContent = this.content;
      this.isEdited = true;
      this.editedAt = new Date();
    }
  }
  
  next();
});

// Instance method to mark as read
messageSchema.methods.markAsRead = async function() {
  if (!this.isRead) {
    this.isRead = true;
    this.readAt = new Date();
    return this.save();
  }
  return this;
};

// Instance method to soft delete
messageSchema.methods.softDelete = async function(deletedBy) {
  this.isDeleted = true;
  this.deletedAt = new Date();
  this.deletedBy = deletedBy;
  return this.save();
};

// Static method to get conversation
messageSchema.statics.getConversation = function(userId1, userType1, userId2, userType2, page = 1, limit = 50) {
  // Generate conversation ID - use sorted user IDs for consistency
  const participantIds = [userId1, userId2].sort();
  const conversationId = `${participantIds[0]}_${participantIds[1]}`;
  
  return this.find({
    conversationId,
    isDeleted: false
  })
  .sort({ createdAt: -1 })
  .limit(limit)
  .skip((page - 1) * limit)
  .populate('relatedAppointment', 'appointmentType scheduledDate status');
};

// Static method to get user's conversations list
messageSchema.statics.getUserConversations = function(userId, userType) {
  return this.aggregate([
    {
      $match: {
        $or: [
          { senderId: new mongoose.Types.ObjectId(userId), senderType: userType },
          { receiverId: new mongoose.Types.ObjectId(userId), receiverType: userType }
        ],
        isDeleted: false
      }
    },
    {
      $sort: { createdAt: -1 }
    },
    {
      $group: {
        _id: "$conversationId",
        lastMessage: { $first: "$$ROOT" },
        unreadCount: {
          $sum: {
            $cond: [
              {
                $and: [
                  { $eq: ["$receiverId", new mongoose.Types.ObjectId(userId)] },
                  { $eq: ["$receiverType", userType] },
                  { $eq: ["$isRead", false] }
                ]
              },
              1,
              0
            ]
          }
        },
        totalMessages: { $sum: 1 }
      }
    },
    {
      $sort: { "lastMessage.createdAt": -1 }
    }
  ]);
};

// Static method to mark conversation as read
messageSchema.statics.markConversationAsRead = function(conversationId, userId, userType) {
  return this.updateMany(
    {
      conversationId,
      receiverId: userId,
      receiverType: userType,
      isRead: false
    },
    {
      $set: {
        isRead: true,
        readAt: new Date()
      }
    }
  );
};

// Static method to get unread message count
messageSchema.statics.getUnreadCount = function(userId, userType) {
  return this.countDocuments({
    receiverId: userId,
    receiverType: userType,
    isRead: false,
    isDeleted: false
  });
};

export const Message = mongoose.model("Message", messageSchema);
