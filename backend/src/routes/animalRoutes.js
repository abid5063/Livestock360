// import express from "express";
// import cloudinary from "../lib/cloudinary.js";
// import Animal from "../models/Animal.js";
// import protectRoute from "../middleware/auth.middleware.js";
// const router = express.Router();
// router.post("/", protectRoute, async (req, res) => {
//   try {
//     const { title, caption, rating, image } = req.body;

//     if (!image || !title || !caption || !rating) {
//       return res.status(400).json({ message: "Please provide all fields" });
//     }

//     // upload the image to cloudinary
//     const uploadResponse = await cloudinary.uploader.upload(image);
//     const imageUrl = uploadResponse.secure_url;

//     // save to the database
//     const newBook = new Book({
//       title,
//       caption,
//       rating,
//       image: imageUrl,
//       user: req.user._id,
//     });

//     await newBook.save();

//     res.status(201).json(newBook);
// } catch (error) {
//   console.log("Error creating book", error);
//   res.status(500).json({ message: error.message });
// }
// });

// export default router;



import express from "express";
import cloudinary from "../lib/cloudinary.js";
import Animal from "../models/Animal.js";
import Vaccine from "../models/Vaccine.js";
import Task from "../models/Task.js";
import protectRoute from "../middleware/auth.middleware.js";
 
const router = express.Router();
 
// Create a new animal
router.post("/", protectRoute, async (req, res) => {
  try {
    const { name, type, breed, age, gender, details, image } = req.body;

    // Validate required fields
    if (!name || !type || !breed || !age || !gender) {
      return res.status(400).json({ 
        message: "Please provide name, type, breed, age and gender" 
      });
    }

    console.log(name);

    // Handle image upload if provided
    // let photo_url = "";
    // if (image) {
    //   const uploadResponse = await cloudinary.uploader.upload(image);
    //   photo_url = uploadResponse.secure_url;
    // }

        let photo_url = "";
    if (image) {
      console.log("typeof image:", typeof image);
      console.log("image starts with:", image.substring(0, 30));
      const uploadResponse = await cloudinary.uploader.upload(image, {
        folder: "animals",
        resource_type: "auto"
      });
      photo_url = uploadResponse.secure_url;
    }
    
    // Create new animal
    const animal = new Animal({
      name,
      type,
      breed,
      age: Number(age),
      gender,
      details: details || "",
      photo_url,
      farmer: req.farmer._id  // Using req.farmer from protectRoute
    });

    await animal.save();

    // Return response without sensitive/unecessary fields
    res.status(201).json({
      _id: animal._id,
      name: animal.name,
      type: animal.type,
      breed: animal.breed,
      age: animal.age,
      gender: animal.gender,
      photo_url: animal.photo_url,
      farmer: {
        _id: req.farmer._id,
        name: req.farmer.name
      }
    });

  } catch (error) {
  console.error("Error creating animal:", error);
  const msg = typeof error.message === "string" && error.message.includes("duplicate key")
    ? "An animal with this name already exists for your farm"
    : "Failed to create animal";
  res.status(500).json({ message: msg });
}
});

// Get all animals for the authenticated farmer
router.get("/", protectRoute, async (req, res) => {
  try {
    const animals = await Animal.find({ farmer: req.farmer._id })
      .sort({ createdAt: -1 })
      .select("-__v -updatedAt");

    res.status(200).json(animals);
  } catch (error) {
    console.error("Error fetching animals:", error);
    res.status(500).json({ message: "Failed to fetch animals" });
  }
});

// Get single animal (with ownership check)
router.get("/:id", protectRoute, async (req, res) => {
  try {
    const animal = await Animal.findOne({
      _id: req.params.id,
      farmer: req.farmer._id
    }).select("-__v -updatedAt");

    if (!animal) {
      return res.status(404).json({ message: "Animal not found in your farm" });
    }

    res.status(200).json(animal);
  } catch (error) {
    console.error("Error fetching animal:", error);
    res.status(500).json({ message: "Failed to fetch animal" });
  }
});

// Update animal
router.put("/:id", protectRoute, async (req, res) => {
  try {
    const updates = req.body;
    const animal = await Animal.findOneAndUpdate(
      { _id: req.params.id, farmer: req.farmer._id },
      updates,
      { new: true, runValidators: true }
    ).select("-__v -updatedAt");

    if (!animal) {
      return res.status(404).json({ message: "Animal not found in your farm" });
    }

    res.status(200).json(animal);
  } catch (error) {
    console.error("Error updating animal:", error);
    res.status(500).json({ 
      message: error.message.includes("duplicate key")
        ? "An animal with this name already exists"
        : "Failed to update animal"
    });
  }
});

// Delete animal
router.delete("/:id", protectRoute, async (req, res) => {
  try {
    // 1. Find the animal first
    const animal = await Animal.findOne({
      _id: req.params.id,
      farmer: req.farmer._id
    });

    if (!animal) {
      return res.status(404).json({ message: "Animal not found in your farm" });
    }

    // 2. Delete all vaccine records for this animal
    await Vaccine.deleteMany({ 
      animal: animal._id,
      farmer: req.farmer._id 
    });

    // 3. Delete all task records for this animal
    await Task.deleteMany({ 
      animal: animal._id,
      farmer: req.farmer._id 
    });

    // 4. Delete image from cloudinary if needed
    if (animal.photo_url && animal.photo_url.includes("cloudinary")) {
      try {
        const parts = animal.photo_url.split("/");
        const fileWithExt = parts[parts.length - 1];
        const folder = parts[parts.length - 2];
        const publicId = `${folder}/${fileWithExt.split(".")[0]}`;
        await cloudinary.uploader.destroy(publicId);
      } catch (deleteError) {
        console.log("Error deleting image from cloudinary", deleteError);
      }
    }

    // 5. Delete the animal from the database
    await Animal.deleteOne({ _id: animal._id });

    res.status(200).json({ message: "Animal removed successfully" });
  } catch (error) {
    console.error("Error deleting animal:", error);
    res.status(500).json({ message: "Failed to delete animal" });
  }
});

export default router;