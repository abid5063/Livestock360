import mongoose from "mongoose";

const appointmentSchema = new mongoose.Schema({
  farmerId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Farmer',
    required: [true, "Farmer ID is required"]
  },
  vetId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Vet',
    required: [true, "Vet ID is required"]
  },
  animalId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Animal',
    required: function() {
      // animalId is required only if animalName is not provided
      return !this.animalName;
    }
  },
  animalName: {
    type: String,
    trim: true,
    maxlength: [100, "Animal name cannot exceed 100 characters"],
    required: function() {
      // animalName is required only if animalId is not provided
      return !this.animalId;
    }
  },
  appointmentType: {
    type: String,
    enum: ['consultation', 'vaccination', 'checkup', 'emergency', 'surgery', 'follow-up'],
    default: 'consultation'
  },
  priority: {
    type: String,
    enum: ['low', 'normal', 'high', 'emergency'],
    default: 'normal'
  },
  status: {
    type: String,
    enum: ['pending', 'accepted', 'rejected', 'in-progress', 'completed', 'cancelled'],
    default: 'pending'
  },
  scheduledDate: {
    type: Date,
    required: [true, "Scheduled date is required"]
  },
  scheduledTime: {
    type: String,
    required: [true, "Scheduled time is required"],
    match: [/^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/, "Please enter time in HH:MM format"]
  },
  duration: {
    type: Number, // Duration in minutes
    default: 30,
    min: 15,
    max: 240
  },
  symptoms: {
    type: String,
    required: [true, "Symptoms description is required"],
    trim: true,
    maxlength: [1000, "Symptoms description cannot exceed 1000 characters"]
  },
  description: {
    type: String,
    trim: true,
    maxlength: [2000, "Description cannot exceed 2000 characters"]
  },
  images: [{
    url: String,
    description: String,
    uploadedAt: { type: Date, default: Date.now }
  }],
  location: {
    type: {
      type: String,
      enum: ['clinic', 'farm', 'online'],
      default: 'clinic'
    },
    address: String,
    coordinates: {
      latitude: Number,
      longitude: Number
    }
  },
  fee: {
    consultationFee: { type: Number, min: 0 },
    travelFee: { type: Number, min: 0, default: 0 },
    totalFee: { type: Number, min: 0 }
  },
  paymentStatus: {
    type: String,
    enum: ['pending', 'paid', 'failed', 'refunded'],
    default: 'pending'
  },
  paymentMethod: {
    type: String,
    enum: ['cash', 'card', 'upi', 'bank_transfer']
  },
  diagnosis: {
    type: String,
    trim: true,
    maxlength: [2000, "Diagnosis cannot exceed 2000 characters"]
  },
  treatment: {
    type: String,
    trim: true,
    maxlength: [2000, "Treatment cannot exceed 2000 characters"]
  },
  prescription: [{
    medicationName: String,
    dosage: String,
    frequency: String,
    duration: String,
    instructions: String
  }],
  followUpRequired: {
    type: Boolean,
    default: false
  },
  followUpDate: Date,
  followUpInstructions: String,
  vetNotes: {
    type: String,
    trim: true,
    maxlength: [3000, "Vet notes cannot exceed 3000 characters"]
  },
  farmerNotes: {
    type: String,
    trim: true,
    maxlength: [1000, "Farmer notes cannot exceed 1000 characters"]
  },
  rating: {
    type: Number,
    min: 1,
    max: 5
  },
  review: {
    type: String,
    trim: true,
    maxlength: [500, "Review cannot exceed 500 characters"]
  },
  isEmergency: {
    type: Boolean,
    default: false
  },
  cancelledBy: {
    type: String,
    enum: ['farmer', 'vet', 'system']
  },
  cancellationReason: String,
  rescheduledFrom: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Appointment'
  },
  completedAt: Date,
  acceptedAt: Date,
  rejectedAt: Date,
  cancelledAt: Date
}, {
  timestamps: true
});

// Indexes for efficient querying
appointmentSchema.index({ farmerId: 1, createdAt: -1 });
appointmentSchema.index({ vetId: 1, createdAt: -1 });
appointmentSchema.index({ animalId: 1 });
appointmentSchema.index({ scheduledDate: 1, scheduledTime: 1 });
appointmentSchema.index({ status: 1 });
appointmentSchema.index({ priority: 1, createdAt: -1 });
appointmentSchema.index({ isEmergency: 1, createdAt: -1 });

// Compound index for vet's schedule
appointmentSchema.index({ 
  vetId: 1, 
  scheduledDate: 1, 
  status: 1 
});

// Virtual for appointment date-time
appointmentSchema.virtual('appointmentDateTime').get(function() {
  if (!this.scheduledDate || !this.scheduledTime) return null;
  
  const date = new Date(this.scheduledDate);
  const [hours, minutes] = this.scheduledTime.split(':');
  date.setHours(parseInt(hours), parseInt(minutes), 0, 0);
  
  return date;
});

// Virtual for is past appointment
appointmentSchema.virtual('isPast').get(function() {
  const appointmentDateTime = this.appointmentDateTime;
  return appointmentDateTime && appointmentDateTime < new Date();
});

// Virtual for formatted date
appointmentSchema.virtual('formattedDate').get(function() {
  return this.scheduledDate ? this.scheduledDate.toDateString() : '';
});

// Virtual for formatted time
appointmentSchema.virtual('formattedTime').get(function() {
  if (!this.scheduledTime) return '';
  
  const [hours, minutes] = this.scheduledTime.split(':');
  const hour12 = hours % 12 || 12;
  const ampm = hours < 12 ? 'AM' : 'PM';
  
  return `${hour12}:${minutes} ${ampm}`;
});

// Pre-save middleware
appointmentSchema.pre('save', function(next) {
  // Calculate total fee
  if (this.fee && (this.fee.consultationFee || this.fee.travelFee)) {
    this.fee.totalFee = (this.fee.consultationFee || 0) + (this.fee.travelFee || 0);
  }
  
  // Set timestamps for status changes
  if (this.isModified('status')) {
    const now = new Date();
    switch (this.status) {
      case 'accepted':
        if (!this.acceptedAt) this.acceptedAt = now;
        break;
      case 'rejected':
        if (!this.rejectedAt) this.rejectedAt = now;
        break;
      case 'completed':
        if (!this.completedAt) this.completedAt = now;
        break;
      case 'cancelled':
        if (!this.cancelledAt) this.cancelledAt = now;
        break;
    }
  }
  
  // Set emergency flag based on priority
  if (this.priority === 'emergency') {
    this.isEmergency = true;
  }
  
  next();
});

// Instance method to check if appointment can be cancelled
appointmentSchema.methods.canBeCancelled = function() {
  const allowedStatuses = ['pending', 'accepted'];
  const appointmentDateTime = this.appointmentDateTime;
  const now = new Date();
  const timeDiff = appointmentDateTime - now;
  const hoursUntilAppointment = timeDiff / (1000 * 60 * 60);
  
  return allowedStatuses.includes(this.status) && hoursUntilAppointment > 2;
};

// Instance method to check if appointment can be rescheduled
appointmentSchema.methods.canBeRescheduled = function() {
  const allowedStatuses = ['pending', 'accepted'];
  const appointmentDateTime = this.appointmentDateTime;
  const now = new Date();
  const timeDiff = appointmentDateTime - now;
  const hoursUntilAppointment = timeDiff / (1000 * 60 * 60);
  
  return allowedStatuses.includes(this.status) && hoursUntilAppointment > 6;
};

// Static method to find overlapping appointments
appointmentSchema.statics.findOverlapping = function(vetId, date, time, duration = 30) {
  const appointmentDate = new Date(date);
  const [hours, minutes] = time.split(':');
  const startTime = new Date(appointmentDate);
  startTime.setHours(parseInt(hours), parseInt(minutes), 0, 0);
  
  const endTime = new Date(startTime);
  endTime.setMinutes(endTime.getMinutes() + duration);
  
  return this.find({
    vetId,
    scheduledDate: appointmentDate,
    status: { $in: ['pending', 'accepted', 'in-progress'] },
    $or: [
      {
        // Appointment starts during this time slot
        $expr: {
          $and: [
            { $gte: [{ $dateFromString: { dateString: { $concat: [{ $dateToString: { date: "$scheduledDate", format: "%Y-%m-%d" } }, "T", "$scheduledTime", ":00"] } } }, startTime] },
            { $lt: [{ $dateFromString: { dateString: { $concat: [{ $dateToString: { date: "$scheduledDate", format: "%Y-%m-%d" } }, "T", "$scheduledTime", ":00"] } } }, endTime] }
          ]
        }
      },
      {
        // This appointment starts during existing appointment
        $expr: {
          $and: [
            { $lte: [{ $dateFromString: { dateString: { $concat: [{ $dateToString: { date: "$scheduledDate", format: "%Y-%m-%d" } }, "T", "$scheduledTime", ":00"] } } }, startTime] },
            { $gt: [{ 
              $add: [
                { $dateFromString: { dateString: { $concat: [{ $dateToString: { date: "$scheduledDate", format: "%Y-%m-%d" } }, "T", "$scheduledTime", ":00"] } } },
                { $multiply: ["$duration", 60000] }
              ]
            }, startTime] }
          ]
        }
      }
    ]
  });
};

// Static method to get vet's schedule for a day
appointmentSchema.statics.getVetSchedule = function(vetId, date) {
  const startOfDay = new Date(date);
  startOfDay.setHours(0, 0, 0, 0);
  
  const endOfDay = new Date(date);
  endOfDay.setHours(23, 59, 59, 999);
  
  return this.find({
    vetId,
    scheduledDate: { $gte: startOfDay, $lte: endOfDay },
    status: { $in: ['pending', 'accepted', 'in-progress', 'completed'] }
  }).sort({ scheduledTime: 1 });
};

export const Appointment = mongoose.model("Appointment", appointmentSchema);
