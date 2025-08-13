import express from "express";
import jwt from "jsonwebtoken";
import { Appointment } from "../models/Appointment.js";
import Farmer from "../models/Farmer.js";
import { Vet } from "../models/Vet.js";
import Animal from "../models/Animal.js";

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
    res.status(401).json({ message: "Invalid token" });
  }
};

// Create new appointment (Farmer or Vet)
router.post("/", verifyToken, async (req, res) => {
  try {
    // Allow both farmers and vets to create appointments
    if (req.user.userType !== 'farmer' && req.user.userType !== 'vet') {
      return res.status(403).json({ message: "Only farmers and vets can create appointments" });
    }

    // Handle different data structures based on who's creating the appointment
    let appointmentData;
    
    if (req.user.userType === 'farmer') {
      // Original farmer flow
      const {
        vetId,
        animalId,
        appointmentType,
        priority,
        scheduledDate,
        scheduledTime,
        duration,
        symptoms,
        description,
        location,
        images
      } = req.body;

      // Validation for farmer-created appointments
      if (!vetId || !animalId || !scheduledDate || !scheduledTime || !symptoms) {
        return res.status(400).json({ 
          message: "Vet ID, Animal ID, scheduled date, time, and symptoms are required" 
        });
      }

      // Verify vet exists and is verified
      const vet = await Vet.findById(vetId);
      if (!vet ) {
        return res.status(400).json({ message: "Vet not found or not available" });
      }


      // Verify animal belongs to the farmer
      // const animal = await Animal.findOne({ _id: animalId, farmerId: req.user.farmerId });
      // if (!animal) {
      //   return res.status(400).json({ message: "Animal not found or doesn't belong to you" });
      // }

      appointmentData = {
        farmerId: req.user.farmerId,
        vetId,
        animalId,
        appointmentType: appointmentType || 'consultation',
        priority: priority || 'normal',
        scheduledDate: new Date(scheduledDate),
        scheduledTime,
        duration: duration || 30,
        symptoms: symptoms.trim(),
        description: description?.trim(),
        location: location || { type: 'clinic' },
        images: images || [],
        fee: {
          consultationFee: vet.consultationFee || 0,
          travelFee: location?.type === 'farm' ? (vet.travelFee || 0) : 0
        }
      };

    } else if (req.user.userType === 'vet') {
      // New vet flow - different data structure from mobile app
      const {
        vet: vetIdFromBody,
        farmer: farmerId,
        animalName,
        date,
        time,
        reason,
        notes,
        status
      } = req.body;

      // Validation for vet-created appointments
      if (!farmerId || !animalName || !date || !time || !reason) {
        return res.status(400).json({ 
          message: "Farmer ID, animal name, date, time, and reason are required" 
        });
      }

      // Verify farmer exists
      const farmer = await Farmer.findById(farmerId);
      if (!farmer) {
        return res.status(400).json({ message: "Farmer not found" });
      }

      // Use vet ID from token, not from body (security)
      const vetId = req.user.vetId;

      appointmentData = {
        farmerId,
        vetId,
        animalName: animalName.trim(), // Store as text instead of reference
        appointmentType: 'consultation',
        priority: 'normal',
        scheduledDate: new Date(date),
        scheduledTime: time,
        duration: 30,
        symptoms: reason.trim(),
        description: notes?.trim() || '',
        status: status || 'pending',
        location: { type: 'clinic' },
        images: [],
        fee: {
          consultationFee: 0,
          travelFee: 0
        }
      };
    }

    // Check for overlapping appointments
    const overlappingAppointments = await Appointment.findOverlapping(
      appointmentData.vetId, 
      appointmentData.scheduledDate, 
      appointmentData.scheduledTime, 
      appointmentData.duration
    );

    if (overlappingAppointments.length > 0) {
      return res.status(400).json({ 
        message: "Vet is not available at the selected time. Please choose a different time slot." 
      });
    }

    // Create appointment
    const appointment = new Appointment(appointmentData);

    await appointment.save();

    // Populate related data for response - handle both flows
    const populateOptions = [
      { path: 'vetId', select: 'name specialty phoneNo location' },
      { path: 'farmerId', select: 'name email phoneNo' }
    ];

    // Only populate animalId if it exists (farmer-created appointments)
    if (appointment.animalId) {
      populateOptions.push({ path: 'animalId', select: 'name species breed' });
    }

    await appointment.populate(populateOptions);

    res.status(201).json({
      message: "Appointment created successfully",
      appointment
    });

  } catch (error) {
    console.error("Create appointment error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Get farmer's appointments
router.get("/farmer", verifyToken, async (req, res) => {
  try {
    if (req.user.userType !== 'farmer') {
      return res.status(403).json({ message: "Access denied" });
    }

    const { status, page = 1, limit = 10 } = req.query;
    const query = { farmerId: req.user.farmerId };
    
    if (status) {
      query.status = status;
    }

    const appointments = await Appointment.find(query)
      .populate('vetId', 'name specialty phoneNo location rating')
      .populate('animalId', 'name species breed')
      .sort({ createdAt: -1 })
      .limit(limit * 1)
      .skip((page - 1) * limit);

    const total = await Appointment.countDocuments(query);

    res.json({
      appointments,
      pagination: {
        current: page,
        pages: Math.ceil(total / limit),
        total
      }
    });

  } catch (error) {
    console.error("Get farmer appointments error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Get vet's appointments
router.get("/vet", verifyToken, async (req, res) => {
  try {
    if (req.user.userType !== 'vet') {
      return res.status(403).json({ message: "Access denied" });
    }

    const { status, date, page = 1, limit = 10 } = req.query;
    const query = { vetId: req.user.vetId };
    
    if (status) {
      query.status = status;
    }
    
    if (date) {
      const selectedDate = new Date(date);
      const startOfDay = new Date(selectedDate);
      startOfDay.setHours(0, 0, 0, 0);
      const endOfDay = new Date(selectedDate);
      endOfDay.setHours(23, 59, 59, 999);
      
      query.scheduledDate = { $gte: startOfDay, $lte: endOfDay };
    }

    const appointments = await Appointment.find(query)
      .populate('farmerId', 'name email phoneNo location')
      .populate('animalId', 'name species breed age weight')
      .sort({ scheduledDate: 1, scheduledTime: 1 })
      .limit(limit * 1)
      .skip((page - 1) * limit);

    const total = await Appointment.countDocuments(query);

    // Add additional data for vet dashboard - handle both animalId and animalName
    const appointmentData = appointments.map(appointment => {
      const appointmentObj = appointment.toObject();
      return {
        ...appointmentObj,
        farmerName: appointment.farmerId.name,
        // Use animalId data if available, otherwise use animalName
        animalType: appointment.animalId ? appointment.animalId.species : 'Unknown',
        animalName: appointment.animalId ? appointment.animalId.name : appointment.animalName,
        date: appointment.scheduledDate,
        id: appointment._id
      };
    });

    res.json({
      appointments: appointmentData,
      pagination: {
        current: page,
        pages: Math.ceil(total / limit),
        total
      }
    });

  } catch (error) {
    console.error("Get vet appointments error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Get current vet's appointments (for mobile app)
router.get("/vet", verifyToken, async (req, res) => {
  try {
    if (req.user.userType !== 'vet') {
      return res.status(403).json({ message: "Access denied" });
    }

    // Use the vetId from the token
    const vetId = req.user.vetId;

    const { status, date } = req.query;
    const query = { vetId };
    
    if (status) {
      query.status = status;
    }
    
    if (date) {
      const selectedDate = new Date(date);
      const startOfDay = new Date(selectedDate);
      startOfDay.setHours(0, 0, 0, 0);
      const endOfDay = new Date(selectedDate);
      endOfDay.setHours(23, 59, 59, 999);
      
      query.scheduledDate = { $gte: startOfDay, $lte: endOfDay };
    }

    const appointments = await Appointment.find(query)
      .populate('farmerId', 'name email phoneNo location')
      .populate('animalId', 'name species breed age weight')
      .sort({ scheduledDate: 1, scheduledTime: 1 });

    // Format appointments for mobile app
    const formattedAppointments = appointments.map(appointment => {
      const appointmentObj = appointment.toObject();
      return {
        ...appointmentObj,
        farmer: {
          _id: appointment.farmerId._id,
          name: appointment.farmerId.name,
          email: appointment.farmerId.email,
          phoneNo: appointment.farmerId.phoneNo,
          location: appointment.farmerId.location
        },
        animal: appointment.animalId ? {
          _id: appointment.animalId._id,
          name: appointment.animalId.name,
          species: appointment.animalId.species,
          breed: appointment.animalId.breed,
          age: appointment.animalId.age,
          weight: appointment.animalId.weight
        } : {
          name: appointment.animalName || 'Unknown'
        },
        date: appointment.scheduledDate,
        time: appointment.scheduledTime,
        reason: appointment.symptoms
      };
    });

    res.json(formattedAppointments);

  } catch (error) {
    console.error("Get current vet appointments error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Get vet's appointments by vet ID (for mobile app) - Legacy route
router.get("/vet/:vetId", verifyToken, async (req, res) => {
  try {
    if (req.user.userType !== 'vet') {
      return res.status(403).json({ message: "Access denied" });
    }

    // Use the vetId from the token for security, ignore the URL parameter
    const vetId = req.user.vetId;

    const { status, date } = req.query;
    const query = { vetId };
    
    if (status) {
      query.status = status;
    }
    
    if (date) {
      const selectedDate = new Date(date);
      const startOfDay = new Date(selectedDate);
      startOfDay.setHours(0, 0, 0, 0);
      const endOfDay = new Date(selectedDate);
      endOfDay.setHours(23, 59, 59, 999);
      
      query.scheduledDate = { $gte: startOfDay, $lte: endOfDay };
    }

    const appointments = await Appointment.find(query)
      .populate('farmerId', 'name email phoneNo location')
      .populate('animalId', 'name species breed age weight')
      .sort({ scheduledDate: 1, scheduledTime: 1 });

    // Format appointments for mobile app
    const formattedAppointments = appointments.map(appointment => {
      const appointmentObj = appointment.toObject();
      return {
        ...appointmentObj,
        farmer: {
          _id: appointment.farmerId._id,
          name: appointment.farmerId.name,
          email: appointment.farmerId.email,
          phoneNo: appointment.farmerId.phoneNo,
          location: appointment.farmerId.location
        },
        animal: appointment.animalId ? {
          _id: appointment.animalId._id,
          name: appointment.animalId.name,
          species: appointment.animalId.species,
          breed: appointment.animalId.breed,
          age: appointment.animalId.age,
          weight: appointment.animalId.weight
        } : {
          name: appointment.animalName || 'Unknown'
        },
        date: appointment.scheduledDate,
        time: appointment.scheduledTime,
        reason: appointment.symptoms
      };
    });

    res.json(formattedAppointments);

  } catch (error) {
    console.error("Get vet appointments by ID error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Get single appointment
router.get("/:id", verifyToken, async (req, res) => {
  try {
    const appointment = await Appointment.findById(req.params.id)
      .populate('farmerId', 'name email phoneNo location')
      .populate('vetId', 'name specialty phoneNo location rating')
      .populate('animalId', 'name species breed age weight');

    if (!appointment) {
      return res.status(404).json({ message: "Appointment not found" });
    }

    // Check if user has access to this appointment
    const hasAccess = 
      (req.user.userType === 'farmer' && appointment.farmerId._id.toString() === req.user.farmerId) ||
      (req.user.userType === 'vet' && appointment.vetId._id.toString() === req.user.vetId);

    if (!hasAccess) {
      return res.status(403).json({ message: "Access denied" });
    }

    res.json(appointment);

  } catch (error) {
    console.error("Get appointment error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Update appointment status (Vet)
router.put("/:id", verifyToken, async (req, res) => {
  try {
    if (req.user.userType !== 'vet') {
      return res.status(403).json({ message: "Only vets can update appointment status" });
    }

    const { status, diagnosis, treatment, prescription, vetNotes, followUpRequired, followUpDate } = req.body;
    
    const appointment = await Appointment.findOne({ 
      _id: req.params.id, 
      vetId: req.user.vetId 
    });

    if (!appointment) {
      return res.status(404).json({ message: "Appointment not found" });
    }

    // Update fields
    if (status) appointment.status = status;
    if (diagnosis) appointment.diagnosis = diagnosis;
    if (treatment) appointment.treatment = treatment;
    if (prescription) appointment.prescription = prescription;
    if (vetNotes) appointment.vetNotes = vetNotes;
    if (followUpRequired !== undefined) appointment.followUpRequired = followUpRequired;
    if (followUpDate) appointment.followUpDate = new Date(followUpDate);

    await appointment.save();

    // Update vet's appointment statistics
    if (status === 'completed') {
      await Vet.findByIdAndUpdate(req.user.vetId, {
        $inc: { completedAppointments: 1 }
      });
    } else if (status === 'cancelled') {
      await Vet.findByIdAndUpdate(req.user.vetId, {
        $inc: { cancelledAppointments: 1 }
      });
    }

    res.json({
      message: "Appointment updated successfully",
      appointment
    });

  } catch (error) {
    console.error("Update appointment error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Cancel appointment
router.delete("/:id", verifyToken, async (req, res) => {
  try {
    const { reason } = req.body;
    
    const appointment = await Appointment.findById(req.params.id);
    if (!appointment) {
      return res.status(404).json({ message: "Appointment not found" });
    }

    // Check if user has permission to cancel
    const hasPermission = 
      (req.user.userType === 'farmer' && appointment.farmerId.toString() === req.user.farmerId) ||
      (req.user.userType === 'vet' && appointment.vetId.toString() === req.user.vetId);

    if (!hasPermission) {
      return res.status(403).json({ message: "Access denied" });
    }

    // Check if appointment can be cancelled
    if (!appointment.canBeCancelled()&& 0) {
      return res.status(400).json({ 
        message: "Appointment cannot be cancelled (too close to appointment time or already completed)" 
      });
    }

    // Update appointment
    appointment.status = 'cancelled';
    appointment.cancelledBy = req.user.userType;
    appointment.cancellationReason = reason;
    appointment.cancelledAt = new Date();

    await appointment.save();

    // Update statistics
    if (req.user.userType === 'vet') {
      await Vet.findByIdAndUpdate(req.user.vetId, {
        $inc: { cancelledAppointments: 1 }
      });
    }

    res.json({
      message: "Appointment cancelled successfully",
      appointment
    });

  } catch (error) {
    console.error("Cancel appointment error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Get available time slots for a vet on a specific date
router.get("/availability/:vetId/:date", verifyToken, async (req, res) => {
  try {
    const { vetId, date } = req.params;
    
    const vet = await Vet.findById(vetId);
    if (!vet || !vet.isActive || !vet.isVerified) {
      return res.status(404).json({ message: "Vet not found or not available" });
    }

    const selectedDate = new Date(date);
    const dayOfWeek = selectedDate.getDay();
    const dayName = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday'][dayOfWeek];
    
    const daySchedule = vet.availableHours[dayName];
    if (!daySchedule || !daySchedule.available) {
      return res.json({ availableSlots: [] });
    }

    // Get existing appointments for the day
    const existingAppointments = await Appointment.getVetSchedule(vetId, selectedDate);
    const bookedSlots = existingAppointments
      .filter(apt => ['pending', 'accepted', 'in-progress'].includes(apt.status))
      .map(apt => apt.scheduledTime);

    // Generate available time slots (assuming 30-minute slots)
    const startHour = daySchedule.start ? parseInt(daySchedule.start.split(':')[0]) : 9;
    const endHour = daySchedule.end ? parseInt(daySchedule.end.split(':')[0]) : 17;
    
    const availableSlots = [];
    for (let hour = startHour; hour < endHour; hour++) {
      for (let minute = 0; minute < 60; minute += 30) {
        const timeSlot = `${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`;
        if (!bookedSlots.includes(timeSlot)) {
          availableSlots.push(timeSlot);
        }
      }
    }

    res.json({ availableSlots });

  } catch (error) {
    console.error("Get availability error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Delete appointment permanently
router.delete("/remove/:id", verifyToken, async (req, res) => {
  try {
    const appointment = await Appointment.findById(req.params.id);
    if (!appointment) {
      return res.status(404).json({ message: "Appointment not found" });
    }

    // Check if user has permission to delete
    const hasPermission = 
      (req.user.userType === 'farmer' && appointment.farmerId.toString() === req.user.farmerId) ||
      (req.user.userType === 'vet' && appointment.vetId.toString() === req.user.vetId);

    if (!hasPermission) {
      return res.status(403).json({ message: "Access denied" });
    }

    // Delete the appointment permanently
    await Appointment.findByIdAndDelete(req.params.id);

    res.json({
      message: "Appointment deleted successfully"
    });

  } catch (error) {
    console.error("Delete appointment error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

export default router;
