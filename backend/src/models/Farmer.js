import mongoose from "mongoose";
import bcrypt from "bcryptjs";

const farmerSchema = new mongoose.Schema({
  name: {
    type: String,
    required: true
  },  
    email: {
    type: String,
    required: true,
    unique: true
  },


  password: {
    type: String,
    required: true,
    minlength: 6
  },
    profileImage: {
    type: String,  // This will store the path or URL to the image
    default: ""    // Default empty string if no image is provided
  },
  phoneNo: {
    type: String,
    default: "",
    required: false
  },
  location: {
    type: String,
    required: false
  },

  creationDate: {
    type: Date,
    default: Date.now
  }
});

// hash password before saving user to db
farmerSchema.pre("save", async function(next) {
  if(!this.isModified("password")) return next();
  const salt = await bcrypt.genSalt(10);
  this.password = await bcrypt.hash(this.password, salt);
  next();
});

// compare password func
farmerSchema.methods.comparePassword = async function (userPassword) {
  return await bcrypt.compare(userPassword, this.password);
};

const Farmer = mongoose.model("Farmer", farmerSchema);
export default Farmer;