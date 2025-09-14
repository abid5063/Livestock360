import jwt from "jsonwebtoken";
import Farmer from "../models/Farmer.js";
import Customer from "../models/Customer.js";
import Vet from "../models/Vet.js";

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

    // find user based on user type
    if (decoded.userType === 'farmer' && decoded.farmerId) {
      const farmer = await Farmer.findById(decoded.farmerId).select("-password");
      if (!farmer) return res.status(401).json({ message: "Token is not valid" });
      req.farmer = farmer;
      req.userType = 'farmer';
    } else if (decoded.userType === 'customer' && decoded.customerId) {
      const customer = await Customer.findById(decoded.customerId).select("-password");
      if (!customer) return res.status(401).json({ message: "Token is not valid" });
      req.customer = customer;
      req.userType = 'customer';
    } else if (decoded.userType === 'vet' && decoded.vetId) {
      const vet = await Vet.findById(decoded.vetId).select("-password");
      if (!vet) return res.status(401).json({ message: "Token is not valid" });
      req.vet = vet;
      req.userType = 'vet';
    } else {
      return res.status(401).json({ message: "Invalid user type in token" });
    }

    next();
  } catch (error) {
    console.error("Authentication error:", error.message);
    res.status(401).json({ message: "Token is not valid" });
  }
};

export default protectRoute;