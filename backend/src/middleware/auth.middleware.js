import jwt from "jsonwebtoken";
import Farmer from "../models/Farmer.js";

const protectRoute = async (req, res, next) => {
  try {
    // get token
    const authHeader = req.header("Authorization");
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
      return res.status(401).json({ message: "No authentication token, access denied" });
    }
    const token = authHeader.replace("Bearer ", "");
    // verify token
    const decoded = jwt.verify(token, process.env.JWT_SECRET);

    // find user
    const farmer = await Farmer.findById(decoded.farmerId).select("-password");
    // console.log(farmer);
    if (!farmer) return res.status(401).json({ message: "Token is not valid" });

    req.farmer = farmer;
    next();
  } catch (error) {
    console.error("Authentication error:", error.message);
    res.status(401).json({ message: "Token is not valid" });
  }
};

export default protectRoute;