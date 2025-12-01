const mongoose = require("mongoose");
const { v4: uuidv4 } = require("uuid");

const searchSchema = new mongoose.Schema({
  _id: { type: String, default: uuidv4 },

  keyword:{type:String, required:true},
  type:{type:[String],required:true},
  target:{type:[String], default:[]},
  
}, { timestamps: true });

searchSchema.set("id", false);

module.exports = mongoose.model("Search", searchSchema,"search");
