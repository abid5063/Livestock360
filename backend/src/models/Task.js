import mongoose from "mongoose";

const taskSchema = new mongoose.Schema({
  title: {
    type: String,
    required: true,
    trim: true,
    maxlength: 100
  },
  description: {
    type: String,
    trim: true,
    maxlength: 500
  },
  dueDate: {
    type: Date,
    required: true
  },
  dueTime: {
    type: String,
    required: true,
    validate: {
      validator: function(v) {
        // Validate time format (HH:MM)
        return /^([01]?[0-9]|2[0-3]):[0-5][0-9]$/.test(v);
      },
      message: 'Time must be in HH:MM format'
    }
  },
  estimatedCost: {
    type: Number,
    min: 0,
    default: 0
  },
  priority: {
    type: String,
    enum: ['low', 'medium', 'high'],
    default: 'medium'
  },
  status: {
    type: String,
    enum: ['pending', 'in-progress', 'completed', 'cancelled'],
    default: 'pending'
  },
  category: {
    type: String,
    enum: ['feeding', 'vaccination', 'health-check', 'breeding', 'maintenance', 'other'],
    default: 'other'
  },
  farmer: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Farmer',
    required: true
  },
  animal: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Animal',
    required: false // Optional - task might not be animal-specific
  },
  isCompleted: {
    type: Boolean,
    default: false
  },
  completedAt: {
    type: Date
  },
  notes: {
    type: String,
    trim: true,
    maxlength: 300
  }
}, {
  timestamps: true
});

// Index for efficient queries
taskSchema.index({ farmer: 1, dueDate: 1 });
taskSchema.index({ farmer: 1, status: 1 });
taskSchema.index({ farmer: 1, priority: 1 });

// Virtual for combining date and time
taskSchema.virtual('dueDatetime').get(function() {
  const [hours, minutes] = this.dueTime.split(':');
  const datetime = new Date(this.dueDate);
  datetime.setHours(parseInt(hours), parseInt(minutes), 0, 0);
  return datetime;
});

// Pre-save middleware to update completedAt when task is completed
taskSchema.pre('save', function(next) {
  if (this.isModified('isCompleted') && this.isCompleted) {
    this.completedAt = new Date();
    this.status = 'completed';
  } else if (this.isModified('isCompleted') && !this.isCompleted) {
    this.completedAt = undefined;
    if (this.status === 'completed') {
      this.status = 'pending';
    }
  }
  next();
});

const Task = mongoose.model('Task', taskSchema);

export default Task;
