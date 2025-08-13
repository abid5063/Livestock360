import express from "express";
import Vaccine from "../models/Vaccine.js";
import Animal from "../models/Animal.js";
import protectRoute from "../middleware/auth.middleware.js";

const router = express.Router();

// Create a new vaccine record
router.post("/", protectRoute, async (req, res) => {
  try {
    const { vaccine_name, animal_id, vaccine_date, notes, next_due_date } = req.body;

    // Validate required fields
    if (!vaccine_name || !animal_id || !vaccine_date) {
      return res.status(400).json({ 
        message: "Please provide vaccine name, animal ID, and vaccine date" 
      });
    }

    // Verify animal exists and belongs to farmer
    const animal = await Animal.findOne({
      _id: animal_id,
      farmer: req.farmer._id
    });

    if (!animal) {
      return res.status(404).json({ message: "Animal not found in your farm" });
    }

    // Create new vaccine record
    const vaccine = new Vaccine({
      vaccine_name,
      animal: animal_id,
      animal_name: animal.name,
      vaccine_date: new Date(vaccine_date),
      notes: notes || "",
      next_due_date: next_due_date ? new Date(next_due_date) : null,
      farmer: req.farmer._id
    });

    await vaccine.save();

    // Populate animal details for response
    await vaccine.populate('animal', 'name type breed');

    res.status(201).json({
      _id: vaccine._id,
      vaccine_name: vaccine.vaccine_name,
      animal_name: vaccine.animal_name,
      vaccine_date: vaccine.vaccine_date,
      notes: vaccine.notes,
      next_due_date: vaccine.next_due_date,
      animal: vaccine.animal,
      farmer: {
        _id: req.farmer._id,
        name: req.farmer.name
      }
    });

  } catch (error) {
    console.error("Error creating vaccine record:", error);
    res.status(500).json({ message: "Failed to create vaccine record" });
  }
});

// Get all vaccine records for the authenticated farmer
router.get("/", protectRoute, async (req, res) => {
  try {
    const vaccines = await Vaccine.find({ farmer: req.farmer._id })
      .populate('animal', 'name type breed')
      .sort({ vaccine_date: -1 })
      .select("-__v -updatedAt");

    res.status(200).json(vaccines);
  } catch (error) {
    console.error("Error fetching vaccines:", error);
    res.status(500).json({ message: "Failed to fetch vaccines" });
  }
});

// Get vaccines for a specific animal
router.get("/animal/:animalId", protectRoute, async (req, res) => {
  try {
    // Verify animal belongs to farmer
    const animal = await Animal.findOne({
      _id: req.params.animalId,
      farmer: req.farmer._id
    });

    if (!animal) {
      return res.status(404).json({ message: "Animal not found in your farm" });
    }

    const vaccines = await Vaccine.find({ 
      animal: req.params.animalId,
      farmer: req.farmer._id 
    })
    .populate('animal', 'name type breed')
    .sort({ vaccine_date: -1 })
    .select("-__v -updatedAt");

    res.status(200).json(vaccines);
  } catch (error) {
    console.error("Error fetching animal vaccines:", error);
    res.status(500).json({ message: "Failed to fetch animal vaccines" });
  }
});

// Get single vaccine record (with ownership check)
router.get("/:id", protectRoute, async (req, res) => {
  try {
    const vaccine = await Vaccine.findOne({
      _id: req.params.id,
      farmer: req.farmer._id
    })
    .populate('animal', 'name type breed')
    .select("-__v -updatedAt");

    if (!vaccine) {
      return res.status(404).json({ message: "Vaccine record not found" });
    }

    res.status(200).json(vaccine);
  } catch (error) {
    console.error("Error fetching vaccine:", error);
    res.status(500).json({ message: "Failed to fetch vaccine record" });
  }
});

// Update vaccine record
router.put("/:id", protectRoute, async (req, res) => {
  try {
    const updates = req.body;
    
    // If animal_id is being updated, verify it belongs to farmer
    if (updates.animal_id) {
      const animal = await Animal.findOne({
        _id: updates.animal_id,
        farmer: req.farmer._id
      });

      if (!animal) {
        return res.status(404).json({ message: "Animal not found in your farm" });
      }

      updates.animal = updates.animal_id;
      updates.animal_name = animal.name;
      delete updates.animal_id;
    }

    // Convert date strings to Date objects
    if (updates.vaccine_date) {
      updates.vaccine_date = new Date(updates.vaccine_date);
    }
    if (updates.next_due_date) {
      updates.next_due_date = new Date(updates.next_due_date);
    }

    const vaccine = await Vaccine.findOneAndUpdate(
      { _id: req.params.id, farmer: req.farmer._id },
      updates,
      { new: true, runValidators: true }
    )
    .populate('animal', 'name type breed')
    .select("-__v -updatedAt");

    if (!vaccine) {
      return res.status(404).json({ message: "Vaccine record not found" });
    }

    res.status(200).json(vaccine);
  } catch (error) {
    console.error("Error updating vaccine:", error);
    res.status(500).json({ message: "Failed to update vaccine record" });
  }
});

// Delete vaccine record
router.delete("/:id", protectRoute, async (req, res) => {
  try {
    const vaccine = await Vaccine.findOne({
      _id: req.params.id,
      farmer: req.farmer._id
    });

    if (!vaccine) {
      return res.status(404).json({ message: "Vaccine record not found" });
    }

    await Vaccine.deleteOne({ _id: vaccine._id });

    res.status(200).json({ message: "Vaccine record deleted successfully" });
  } catch (error) {
    console.error("Error deleting vaccine:", error);
    res.status(500).json({ message: "Failed to delete vaccine record" });
  }
});

export default router;
