import mongoose from "mongoose";

const animalSchema = new mongoose.Schema({
  farmer: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Farmer',
    required: true
  },
  name: {
    type: String,
    required: true,
    trim: true,
    minlength: 2,
    maxlength: 50
  },
  type: {
    type: String,
    required: true,
    // enum: ['Cattle', 'Poultry', 'Swine', 'Sheep', 'Goat', 'Other'] // example types
  },
  breed: {
    type: String,
    required: true,
    trim: true
  },
  age: {
    type: Number,
    required: true,
    min: 0,
    max: 50 // reasonable max age for most farm animals
  },
  gender: {
    type: String,
    required: true,
    // enum: ['Male', 'Female', 'Other']
  },
  details: {
    type: String,
    default: "",
    maxlength: 500
  },
  photo_url: {
    type: String,
    default: ""
  },
  creation_date: {
    type: Date,
    default: Date.now
  }
}, {
  timestamps: true // adds createdAt and updatedAt automatically
});

// Compound index for farmer + name to ensure unique animal names per farmer
animalSchema.index({ farmer: 1, name: 1 }, { unique: true });

// Index for faster queries on farmer field
animalSchema.index({ farmer: 1 });

const Animal = mongoose.model('Animal', animalSchema);
export default Animal;