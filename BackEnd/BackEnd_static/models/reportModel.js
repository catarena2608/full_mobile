const mongoose = require("mongoose");
const { v4: uuidv4 } = require("uuid");

const reportSchema = new mongoose.Schema({
  _id: { type: String, default: uuidv4 },
  author:{type:String,required:true},
  reportedUser:{type:String,required:true},
  type: {type: String, required:true},
  target: { type: String, required: true },
  content: { type: String, default: "" },

}, { timestamps: true });

reportSchema.set("id", false);

module.exports = mongoose.model("ReportSearch", reportSchema, "reports");
