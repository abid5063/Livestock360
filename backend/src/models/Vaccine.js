import mongoose from "mongoose";

const vaccineSchema = new mongoose.Schema({
  farmer: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Farmer',
    required: true
  },
  animal: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Animal',
    required: true
  },
  vaccine_name: {
    type: String,
    required: true,
    trim: true,
    minlength: 2,
    maxlength: 100
  },
  animal_name: {
    type: String,
    required: true,
    trim: true
  },
  vaccine_date: {
    type: Date,
    required: true
  },
  notes: {
    type: String,
    default: "",
    maxlength: 500
  },
  next_due_date: {
    type: Date,
    default: null
  }
}, {
  timestamps: true
});

// Index for efficient queries
vaccineSchema.index({ farmer: 1, animal: 1 });
vaccineSchema.index({ farmer: 1, vaccine_date: -1 });

const Vaccine = mongoose.model('Vaccine', vaccineSchema);
export default Vaccine;
