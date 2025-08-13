import express from "express";
import Task from "../models/Task.js";
import Animal from "../models/Animal.js";
import protectRoute from "../middleware/auth.middleware.js";

const router = express.Router();

// Create a new task
router.post("/", protectRoute, async (req, res) => {
  try {
    const { 
      title, 
      description, 
      dueDate, 
      dueTime, 
      estimatedCost, 
      priority, 
      category, 
      animal,
      notes 
    } = req.body;

    // Validate required fields
    if (!title || !dueDate || !dueTime) {
      return res.status(400).json({ 
        message: "Please provide title, due date, and due time" 
      });
    }

    // Validate animal exists and belongs to farmer if provided
    if (animal) {
      const animalExists = await Animal.findOne({
        _id: animal,
        farmer: req.farmer._id
      });
      
      if (!animalExists) {
        return res.status(400).json({ 
          message: "Animal not found in your farm" 
        });
      }
    }

    // Create new task
    const task = new Task({
      title,
      description: description || "",
      dueDate: new Date(dueDate),
      dueTime,
      estimatedCost: estimatedCost || 0,
      priority: priority || 'medium',
      category: category || 'other',
      animal: animal || null,
      notes: notes || "",
      farmer: req.farmer._id
    });

    await task.save();

    // Populate animal details if present
    await task.populate('animal', 'name type breed');

    res.status(201).json(task);

  } catch (error) {
    console.error("Error creating task:", error);
    res.status(500).json({ message: "Failed to create task" });
  }
});

// Get all tasks for the authenticated farmer
router.get("/", protectRoute, async (req, res) => {
  try {
    const { 
      status, 
      priority, 
      category, 
      dateFrom, 
      dateTo, 
      animal 
    } = req.query;

    // Build query
    const query = { farmer: req.farmer._id };

    if (status) query.status = status;
    if (priority) query.priority = priority;
    if (category) query.category = category;
    if (animal) query.animal = animal;

    // Date range filter
    if (dateFrom || dateTo) {
      query.dueDate = {};
      if (dateFrom) query.dueDate.$gte = new Date(dateFrom);
      if (dateTo) query.dueDate.$lte = new Date(dateTo);
    }

    const tasks = await Task.find(query)
      .populate('animal', 'name type breed')
      .sort({ dueDate: 1, dueTime: 1 })
      .select("-__v -updatedAt");

    res.status(200).json(tasks);
  } catch (error) {
    console.error("Error fetching tasks:", error);
    res.status(500).json({ message: "Failed to fetch tasks" });
  }
});

// Get single task (with ownership check)
router.get("/:id", protectRoute, async (req, res) => {
  try {
    const task = await Task.findOne({
      _id: req.params.id,
      farmer: req.farmer._id
    })
    .populate('animal', 'name type breed')
    .select("-__v -updatedAt");

    if (!task) {
      return res.status(404).json({ message: "Task not found" });
    }

    res.status(200).json(task);
  } catch (error) {
    console.error("Error fetching task:", error);
    res.status(500).json({ message: "Failed to fetch task" });
  }
});

// Update task
router.put("/:id", protectRoute, async (req, res) => {
  try {
    const updates = req.body;
    
    // Validate animal exists and belongs to farmer if being updated
    if (updates.animal) {
      const animalExists = await Animal.findOne({
        _id: updates.animal,
        farmer: req.farmer._id
      });
      
      if (!animalExists) {
        return res.status(400).json({ 
          message: "Animal not found in your farm" 
        });
      }
    }

    const task = await Task.findOneAndUpdate(
      { _id: req.params.id, farmer: req.farmer._id },
      updates,
      { new: true, runValidators: true }
    )
    .populate('animal', 'name type breed')
    .select("-__v -updatedAt");

    if (!task) {
      return res.status(404).json({ message: "Task not found" });
    }

    res.status(200).json(task);
  } catch (error) {
    console.error("Error updating task:", error);
    res.status(500).json({ message: "Failed to update task" });
  }
});

// Toggle task completion
router.patch("/:id/toggle", protectRoute, async (req, res) => {
  try {
    const task = await Task.findOne({
      _id: req.params.id,
      farmer: req.farmer._id
    });

    if (!task) {
      return res.status(404).json({ message: "Task not found" });
    }

    task.isCompleted = !task.isCompleted;
    await task.save();

    await task.populate('animal', 'name type breed');

    res.status(200).json(task);
  } catch (error) {
    console.error("Error toggling task completion:", error);
    res.status(500).json({ message: "Failed to toggle task completion" });
  }
});

// Delete task
router.delete("/:id", protectRoute, async (req, res) => {
  try {
    const task = await Task.findOne({
      _id: req.params.id,
      farmer: req.farmer._id
    });

    if (!task) {
      return res.status(404).json({ message: "Task not found" });
    }

    await Task.deleteOne({ _id: task._id });

    res.status(200).json({ message: "Task deleted successfully" });
  } catch (error) {
    console.error("Error deleting task:", error);
    res.status(500).json({ message: "Failed to delete task" });
  }
});

// Get task statistics
router.get("/stats/overview", protectRoute, async (req, res) => {
  try {
    const stats = await Task.aggregate([
      { $match: { farmer: req.farmer._id } },
      {
        $group: {
          _id: null,
          totalTasks: { $sum: 1 },
          completedTasks: { 
            $sum: { $cond: [{ $eq: ["$isCompleted", true] }, 1, 0] } 
          },
          pendingTasks: { 
            $sum: { $cond: [{ $eq: ["$status", "pending"] }, 1, 0] } 
          },
          totalEstimatedCost: { $sum: "$estimatedCost" },
          overdueTasks: {
            $sum: {
              $cond: [
                {
                  $and: [
                    { $lt: ["$dueDate", new Date()] },
                    { $ne: ["$isCompleted", true] }
                  ]
                },
                1,
                0
              ]
            }
          }
        }
      }
    ]);

    const result = stats[0] || {
      totalTasks: 0,
      completedTasks: 0,
      pendingTasks: 0,
      totalEstimatedCost: 0,
      overdueTasks: 0
    };

    res.status(200).json(result);
  } catch (error) {
    console.error("Error fetching task statistics:", error);
    res.status(500).json({ message: "Failed to fetch task statistics" });
  }
});

export default router;
