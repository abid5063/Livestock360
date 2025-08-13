import express from "express";
import "dotenv/config";
import cors from "cors"; // Changed from require() to import
import authRoutes from "./routes/authRoutes.js";
import animalRoutes from "./routes/animalRoutes.js";
import vaccineRoutes from "./routes/vaccineRoutes.js";
import taskRoutes from "./routes/taskRoutes.js";
import vetRoutes from "./routes/vetRoutes.js";
import appointmentRoutes from "./routes/appointmentRoutes.js";
import messageRoutes from "./routes/messageRoutes.js";
import { connectDB } from "./lib/db.js";

const app = express();
const PORT = process.env.PORT || 3000;

// CORS configuration: Accept all origins (for development)
app.use(cors({
  origin: true,
  credentials: true, // If using cookies/sessions
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));

app.use(express.json());

app.use("/api/auth", authRoutes);
app.use("/api/animals", animalRoutes);
app.use("/api/vaccines", vaccineRoutes);
app.use("/api/tasks", taskRoutes);
app.use("/api/vets", vetRoutes);
app.use("/api/appointments", appointmentRoutes);
app.use("/api/messages", messageRoutes);

app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
  connectDB();
});