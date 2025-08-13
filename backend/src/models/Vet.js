import mongoose from "mongoose";

const vetSchema = new mongoose.Schema({
  name: {
    type: String,
    required: [true, "Name is required"],
    trim: true,
    minlength: [3, "Name must be at least 3 characters long"],
    maxlength: [100, "Name cannot exceed 100 characters"]
  },
  email: {
    type: String,
    required: [true, "Email is required"],
    trim: true,
    lowercase: true,
    match: [/^\w+([.-]?\w+)*@\w+([.-]?\w+)*(\.\w{2,3})+$/, "Please enter a valid email"]
  },
  password: {
    type: String,
    required: [true, "Password is required"],
    minlength: [6, "Password must be at least 6 characters long"]
  },
  specialty: {
    type: String,
    required: [true, "Specialty is required"],
    trim: true
  },
  licenseNumber: {
    type: String,
    required: [true, "License number is required"],
    trim: true,
    uppercase: true
  },
  phoneNo: {
    type: String,
    trim: true,
    match: [/^[\+]?[0-9\s\-\(\)]{10,15}$/, "Please enter a valid phone number"]
  },
  location: {
    type: String,
    trim: true,
    maxlength: [200, "Location cannot exceed 200 characters"]
  },
  isVerified: {
    type: Boolean,
    default: false
  },
  verificationDocuments: [{
    type: String, // URLs to uploaded verification documents
    trim: true
  }],
  rating: {
    type: Number,
    min: 0,
    max: 5,
    default: 0
  },
  totalReviews: {
    type: Number,
    default: 0
  },
  yearsOfExperience: {
    type: Number,
    min: 0,
    max: 50
  },
  education: [{
    degree: String,
    institution: String,
    year: Number
  }],
  certifications: [{
    name: String,
    issuingBody: String,
    dateIssued: Date,
    expiryDate: Date
  }],
  consultationFee: {
    type: Number,
    min: 0
  },
  emergencyFee: {
    type: Number,
    min: 0
  },
  availableHours: {
    monday: { start: String, end: String, available: { type: Boolean, default: true } },
    tuesday: { start: String, end: String, available: { type: Boolean, default: true } },
    wednesday: { start: String, end: String, available: { type: Boolean, default: true } },
    thursday: { start: String, end: String, available: { type: Boolean, default: true } },
    friday: { start: String, end: String, available: { type: Boolean, default: true } },
    saturday: { start: String, end: String, available: { type: Boolean, default: false } },
    sunday: { start: String, end: String, available: { type: Boolean, default: false } }
  },
  emergencyAvailable: {
    type: Boolean,
    default: false
  },
  profileImage: {
    type: String, // URL to profile image
    trim: true
  },
  bio: {
    type: String,
    maxlength: [500, "Bio cannot exceed 500 characters"],
    trim: true
  },
  languages: [{
    type: String,
    trim: true
  }],
  serviceAreas: [{
    type: String, // Geographic areas they serve
    trim: true
  }],
  clinicAddress: {
    street: String,
    city: String,
    state: String,
    zipCode: String,
    country: { type: String, default: "India" }
  },
  socialLinks: {
    website: String,
    linkedin: String,
    facebook: String
  },
  isActive: {
    type: Boolean,
    default: true
  },
  registrationDate: {
    type: Date,
    default: Date.now
  },
  lastLogin: {
    type: Date
  },
  totalAppointments: {
    type: Number,
    default: 0
  },
  completedAppointments: {
    type: Number,
    default: 0
  },
  cancelledAppointments: {
    type: Number,
    default: 0
  }
}, {
  timestamps: true
});

// Index for efficient searching
vetSchema.index({ email: 1 }, { unique: true });
vetSchema.index({ licenseNumber: 1 }, { unique: true });
vetSchema.index({ specialty: 1 });
vetSchema.index({ location: 1 });
vetSchema.index({ isVerified: 1, isActive: 1 });
vetSchema.index({ rating: -1 });

// Virtual for full address
vetSchema.virtual('fullAddress').get(function() {
  if (!this.clinicAddress) return this.location;
  
  const { street, city, state, zipCode } = this.clinicAddress;
  return [street, city, state, zipCode].filter(Boolean).join(', ');
});

// Virtual for completion rate
vetSchema.virtual('completionRate').get(function() {
  if (this.totalAppointments === 0) return 0;
  return Math.round((this.completedAppointments / this.totalAppointments) * 100);
});

// Method to calculate average rating
vetSchema.methods.updateRating = async function(newRating) {
  const currentTotal = this.rating * this.totalReviews;
  this.totalReviews += 1;
  this.rating = (currentTotal + newRating) / this.totalReviews;
  return this.save();
};

// Method to check if vet is available at specific time
vetSchema.methods.isAvailableAt = function(dayOfWeek, time) {
  const dayName = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday'][dayOfWeek];
  const daySchedule = this.availableHours[dayName];
  
  if (!daySchedule || !daySchedule.available) return false;
  
  if (!daySchedule.start || !daySchedule.end) return true; // No specific hours set
  
  const startTime = new Date(`1970-01-01T${daySchedule.start}:00`);
  const endTime = new Date(`1970-01-01T${daySchedule.end}:00`);
  const checkTime = new Date(`1970-01-01T${time}:00`);
  
  return checkTime >= startTime && checkTime <= endTime;
};

// Pre-save middleware to ensure email is lowercase
vetSchema.pre('save', function(next) {
  if (this.email) {
    this.email = this.email.toLowerCase();
  }
  if (this.licenseNumber) {
    this.licenseNumber = this.licenseNumber.toUpperCase();
  }
  next();
});

// Static method to find verified vets by specialty
vetSchema.statics.findBySpecialty = function(specialty) {
  return this.find({ 
    specialty, 
    isVerified: true, 
    isActive: true 
  }).sort({ rating: -1, totalReviews: -1 });
};

// Static method to search vets
vetSchema.statics.searchVets = function(searchTerm, location) {
  const query = {
    isVerified: true,
    isActive: true
  };
  
  if (searchTerm) {
    query.$or = [
      { name: { $regex: searchTerm, $options: 'i' } },
      { specialty: { $regex: searchTerm, $options: 'i' } },
      { bio: { $regex: searchTerm, $options: 'i' } }
    ];
  }
  
  if (location) {
    query.$or = query.$or || [];
    query.$or.push(
      { location: { $regex: location, $options: 'i' } },
      { 'clinicAddress.city': { $regex: location, $options: 'i' } },
      { 'clinicAddress.state': { $regex: location, $options: 'i' } },
      { serviceAreas: { $regex: location, $options: 'i' } }
    );
  }
  
  return this.find(query).sort({ rating: -1, totalReviews: -1 });
};

export const Vet = mongoose.model("Vet", vetSchema);
