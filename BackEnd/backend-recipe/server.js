const express = require("express");
const mongoose = require("mongoose");
const cors = require("cors");
const bodyParser = require("body-parser");
const cookieParser = require("cookie-parser");
require("dotenv").config();

const recipeRoutes = require("./routes/recipeRoutes");

const app = express();

// ---------------- MIDDLEWARE ----------------
app.use(
  cors({
    origin: "http://localhost:5173",
    credentials: true,
  })
);
app.use(bodyParser.json());
app.use(cookieParser());

// ---------------- ROUTES ----------------
app.use("/api/recipe", recipeRoutes);

// ---------------- MONGO CONNECT ----------------
mongoose
  .connect(process.env.MONGO_URI, { dbName: "recipedb" })
  .then(() => {
    console.log("✅ Recipe service connected to MongoDB");

    const PORT = process.env.PORT || 5001;
    app.listen(PORT, () =>
      console.log(`🚀 Recipe service running on port ${PORT}`)
    );
  })
  .catch((err) => console.error("❌ MongoDB error:", err));
