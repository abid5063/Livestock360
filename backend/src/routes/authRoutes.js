import express from "express";
import Farmer from "../models/Farmer.js";
import jwt from "jsonwebtoken";
import protectRoute from "../middleware/auth.middleware.js"; // <-- Import your auth middleware

const router = express.Router();

const generateToken = (farmerId) => {
  return jwt.sign({ 
    farmerId, 
    userType: 'farmer' 
  }, process.env.JWT_SECRET, { expiresIn: "15d" });
}

router.post("/register", async (req, res) => {
  try {
    const { email, name, password } = req.body;

    if (!name || !email || !password) {
      return res.status(400).json({ message: "All fields are required" });
    }

    if (password.length < 6) {
      return res.status(400).json({ message: "Password should be at least 6 characters long" });
    }

    if (name.length < 3) {
      return res.status(400).json({ message: "name should be at least 3 characters long" });
    }

    const existingEmail = await Farmer.findOne({ email });
    if (existingEmail) {
      return res.status(400).json({ message: "Email already exists" });
    }

    const existingname = await Farmer.findOne({ name });
    if (existingname) {
      return res.status(400).json({ message: "name already exists" });
    }

    const profileImage = `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=random&color=fff&size=256`;

    const farmer = new Farmer({
      email,
      name,
      password,
      profileImage,
      location: "",
      phoneNo: "",
    });

    await farmer.save();
    const token = generateToken(farmer._id);

    res.status(201).json({
      token, 
      farmer: {
        _id: farmer._id,
        name: farmer.name, 
        email: farmer.email,
        profileImage: farmer.profileImage,
      },
    });

  } catch (error) {
    console.log("Error in register route", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

router.post("/login", async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) return res.status(400).json({ message: "All fields are required" });

    const farmer = await Farmer.findOne({ email });
    if (!farmer) return res.status(400).json({ message: "Invalid credentials" });

    const isPasswordCorrect = await farmer.comparePassword(password);
    if (!isPasswordCorrect) return res.status(400).json({ message: "Invalid credentials" });

    const token = generateToken(farmer._id);

    res.status(200).json({
      token,
      farmer: {
        id: farmer._id,
        name: farmer.name,
        email: farmer.email,
        profileImage: farmer.profileImage,
        phoneNo: farmer.phoneNo,
        location: farmer.location,
      },
    });
  } catch (error) {
    console.log("Error in login route", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// ...existing code...

// Edit farmer profile (authenticated)
router.put("/edit/:id", protectRoute, async (req, res) => {
  try {
    // Only allow the authenticated farmer to update their own profile
    const updates = req.body;
    const farmer = await Farmer.findOneAndUpdate(
      { _id: req.params.id, _id: req.farmer._id },
      updates,
      { new: true, runValidators: true }
    ).select("-password");

    if (!farmer) {
      return res.status(404).json({ message: "Farmer not found or unauthorized" });
    }

    res.status(200).json({
      message: "Farmer updated successfully",
      farmer: {
        id: farmer._id,
        name: farmer.name,
        email: farmer.email,
        profileImage: farmer.profileImage,
        phoneNo: farmer.phoneNo,
        location: farmer.location,
      }
    });
  } catch (error) {
    console.log("Error in edit farmer route", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Delete farmer profile (authenticated)
router.delete("/delete/:id", protectRoute, async (req, res) => {
  try {
    const farmer = await Farmer.findOneAndDelete({
      _id: req.params.id,
      _id: req.farmer._id
    });

    if (!farmer) {
      return res.status(404).json({ message: "Farmer not found or unauthorized" });
    }
    res.status(200).json({ message: "Farmer deleted successfully" });
  } catch (error) {
    console.log("Error in delete farmer route", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Search for farmers (for vets to find farmers to message)
router.get("/farmers/search", async (req, res) => {
  try {
    const { search } = req.query;
    
    let query = {};

    if (search) {
      query.$or = [
        { name: { $regex: search, $options: 'i' } },
        { location: { $regex: search, $options: 'i' } }
      ];
    }

    const farmers = await Farmer.find(query)
      .select('name location profileImage phoneNo')
      .limit(20);

    res.json({ farmers });

  } catch (error) {
    console.error("Search farmers error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// Get all farmers (for vets)
router.get("/farmers", async (req, res) => {
  try {
    const farmers = await Farmer.find({})
      .select('name location profileImage phoneNo')
      .limit(20);

    res.json({ farmers });

  } catch (error) {
    console.error("Get farmers error:", error);
    res.status(500).json({ message: "Internal server error" });
  }
});

// ...existing code...

export default router;