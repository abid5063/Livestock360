import express from "express";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import { Vet } from "../models/Vet.js";

const router = express.Router();

// Register vet
router.post("/register", async (req, res) => {
  try {
    const { name, email, password, specialty, licenseNumber, phoneNo, location } = req.body;

    // Validation
    if (!name || !email || !password || !specialty || !licenseNumber) {
      return res.status(400).json({ message: "All required fields must be provided" });
    }

    if (password.length < 6) {
      return res.status(400).json({ message: "Password must be at least 6 characters" });
    }

    if (name.length < 3) {
      return res.status(400).json({ message: "Name must be at least 3 characters" });
    }

    // Check if vet already exists
    const existingVet = await Vet.findOne({ 
      $or: [
        { email: email.toLowerCase() }, 
        { licenseNumber }
      ]
    });

    if (existingVet) {
      if (existingVet.email === email.toLowerCase()) {
        return res.status(400).json({ message: "Email already registered" });
      }
      if (existingVet.licenseNumber === licenseNumber) {
        return res.status(400).json({ message: "License number already registered" });
      }
    }

    // Hash password
    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(password, salt);

    // Create new vet
    const vet = new Vet({
      name: name.trim(),
      email: email.toLowerCase().trim(),
      password: hashedPassword,
      specialty: specialty.trim(),
      licenseNumber: licenseNumber.trim(),
      phoneNo: phoneNo?.trim(),
      location: location?.trim(),
      isVerified: false, // Vets need verification
      registrationDate: new Date()
    });

    await vet.save();

    // Generate JWT token
    const token = jwt.sign(
      { 
        vetId: vet._id, 
        email: vet.email,
        userType: 'vet'
      },
      process.env.JWT_SECRET || "your_jwt_secret_key",
      { expiresIn: "30d" }
    );

    // Return vet data without password
    const vetData = {
      id: vet._id,
      name: vet.name,
      email: vet.email,
      specialty: vet.specialty,
      licenseNumber: vet.licenseNumber,
      phoneNo: vet.phoneNo,
      location: vet.location,
      isVerified: vet.isVerified,
      userType: 'vet'
    };

    res.status(201).json({
      message: "Vet registered successfully",
      token,
      vet: vetData
    });

  } catch (error) {
    console.error("Vet registration error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Login vet
router.post("/login", async (req, res) => {
  try {
    const { email, password } = req.body;

    // Validation
    if (!email || !password) {
      return res.status(400).json({ message: "Email and password are required" });
    }

    // Find vet by email
    const vet = await Vet.findOne({ email: email.toLowerCase().trim() });
    if (!vet) {
      return res.status(400).json({ message: "Invalid credentials" });
    }

    // Check password
    const isPasswordValid = await bcrypt.compare(password, vet.password);
    if (!isPasswordValid) {
      return res.status(400).json({ message: "Invalid credentials" });
    }

    // Update last login
    vet.lastLogin = new Date();
    await vet.save();

    // Generate JWT token
    const token = jwt.sign(
      { 
        vetId: vet._id, 
        email: vet.email,
        userType: 'vet'
      },
      process.env.JWT_SECRET || "your_jwt_secret_key",
      { expiresIn: "30d" }
    );

    // Return vet data without password
    const vetData = {
      id: vet._id,
      name: vet.name,
      email: vet.email,
      specialty: vet.specialty,
      licenseNumber: vet.licenseNumber,
      phoneNo: vet.phoneNo,
      location: vet.location,
      isVerified: vet.isVerified,
      userType: 'vet'
    };

    res.json({
      message: "Login successful",
      token,
      vet: vetData
    });

  } catch (error) {
    console.error("Vet login error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Get vet profile
router.get("/profile", async (req, res) => {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({ message: "Access denied. No token provided." });
    }

    const token = authHeader.substring(7);
    const decoded = jwt.verify(token, process.env.JWT_SECRET || "your_jwt_secret_key");
    
    const vet = await Vet.findById(decoded.vetId).select('-password');
    if (!vet) {
      return res.status(404).json({ message: "Vet not found" });
    }

    res.json(vet);

  } catch (error) {
    console.error("Get vet profile error:", error);
    if (error.name === 'JsonWebTokenError') {
      return res.status(401).json({ message: "Invalid token" });
    }
    res.status(500).json({ message: "Internal server error" });
  }
});

// Update vet profile
router.put("/profile", async (req, res) => {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({ message: "Access denied. No token provided." });
    }

    const token = authHeader.substring(7);
    const decoded = jwt.verify(token, process.env.JWT_SECRET || "your_jwt_secret_key");
    
    const { name, specialty, phoneNo, location } = req.body;
    
    const vet = await Vet.findByIdAndUpdate(
      decoded.vetId,
      { 
        $set: { 
          name: name?.trim(),
          specialty: specialty?.trim(),
          phoneNo: phoneNo?.trim(),
          location: location?.trim(),
          updatedAt: new Date()
        }
      },
      { new: true, runValidators: true }
    ).select('-password');

    if (!vet) {
      return res.status(404).json({ message: "Vet not found" });
    }

    res.json({
      message: "Profile updated successfully",
      vet
    });

  } catch (error) {
    console.error("Update vet profile error:", error);
    if (error.name === 'JsonWebTokenError') {
      return res.status(401).json({ message: "Invalid token" });
    }
    res.status(500).json({ message: "Internal server error" });
  }
});

// Search for vets
router.get("/search", async (req, res) => {
  try {
    const { specialty, location, search } = req.query;
    
    let query = {
      // Removed the isVerified and isActive restrictions for now to test
      // You can add them back later: isVerified: true, isActive: true
    };

    if (specialty) {
      query.specialty = specialty;
    }

    if (location) {
      query.$or = [
        { location: { $regex: location, $options: 'i' } },
        { 'clinicAddress.city': { $regex: location, $options: 'i' } },
        { 'clinicAddress.state': { $regex: location, $options: 'i' } }
      ];
    }

    if (search) {
      query.$or = query.$or || [];
      query.$or.push(
        { name: { $regex: search, $options: 'i' } },
        { specialty: { $regex: search, $options: 'i' } },
        { bio: { $regex: search, $options: 'i' } }
      );
    }

    const vets = await Vet.find(query)
      .select('name specialty location rating totalReviews consultationFee bio profileImage')
      .sort({ rating: -1, totalReviews: -1 })
      .limit(20);

    res.json({ vets });

  } catch (error) {
    console.error("Search vets error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Helper function to verify vet token
const verifyVetToken = async (req, res) => {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return { error: "Access denied. No token provided." };
    }

    const token = authHeader.substring(7);
    const decoded = jwt.verify(token, process.env.JWT_SECRET || "your_jwt_secret_key");
    
    if (decoded.userType !== 'vet') {
      return { error: "Access denied. Invalid user type." };
    }

    const vet = await Vet.findById(decoded.vetId).select('-password');
    if (!vet) {
      return { error: "Vet not found." };
    }

    return { vet, decoded };
  } catch (error) {
    return { error: "Invalid token." };
  }
};

// Edit vet profile (authenticated)
router.put("/edit/:id", async (req, res) => {
  try {
    const auth = await verifyVetToken(req, res);
    if (auth.error) {
      return res.status(401).json({ message: auth.error });
    }

    // Only allow the authenticated vet to update their own profile
    const updates = req.body;
    const vet = await Vet.findOneAndUpdate(
      { _id: req.params.id, _id: auth.vet._id },
      { 
        $set: { 
          ...updates,
          updatedAt: new Date()
        }
      },
      { new: true, runValidators: true }
    ).select("-password");

    if (!vet) {
      return res.status(404).json({ message: "Vet not found or unauthorized" });
    }

    res.status(200).json({
      message: "Vet profile updated successfully",
      vet: {
        id: vet._id,
        name: vet.name,
        email: vet.email,
        profileImage: vet.profileImage,
        phoneNo: vet.phoneNo,
        location: vet.location,
        specialty: vet.specialty,
        experience: vet.experience,
      }
    });
  } catch (error) {
    console.log("Error in edit vet route", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Delete vet profile (authenticated)
router.delete("/delete/:id", async (req, res) => {
  try {
    const auth = await verifyVetToken(req, res);
    if (auth.error) {
      return res.status(401).json({ message: auth.error });
    }

    // Only allow the authenticated vet to delete their own profile
    const vet = await Vet.findOneAndDelete({
      _id: req.params.id,
      _id: auth.vet._id
    });

    if (!vet) {
      return res.status(404).json({ message: "Vet not found or unauthorized" });
    }

    res.status(200).json({ message: "Vet profile deleted successfully" });
  } catch (error) {
    console.log("Error in delete vet route", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

export default router;
