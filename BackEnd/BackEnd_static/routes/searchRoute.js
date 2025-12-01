const express = require("express");
const router = express.Router();

const TopSearch = require("../models/topSearchModel");

router.get("/", async (req, res) => {
  try {
    const {
      type,
      day,
      month,
      year,
      countHigh,
      countLow,
      limit = 20
    } = req.query;

    let filter = {};
    let sort = { count: -1 };

    /* ----------------------------------
     * 1️⃣ Lọc theo type (mảng string)
     * ---------------------------------- */
    if (type) {
      const typeArr = type.split(",");
      filter.type = { $in: typeArr };
    }

    /* ----------------------------------
     * 2️⃣ Lọc theo ngày / tháng / năm
     * ---------------------------------- */
    if (day || month || year) {
      filter.createdAt = {};

      if (year) {
        filter.createdAt.$gte = new Date(year, month ? month - 1 : 0, day || 1);
        filter.createdAt.$lte = new Date(
          year,
          month ? month - 1 : 11,
          day || 31,
          23, 59, 59
        );
      }
    }

    /* ----------------------------------
     * 3️⃣ Lọc theo count
     * ---------------------------------- */
    if (countLow || countHigh) {
      filter.count = {};

      if (countLow) filter.count.$gte = Number(countLow);
      if (countHigh) filter.count.$lte = Number(countHigh);
    }

    /* ----------------------------------
     * Nếu filter trống → trả topSearch
     * ---------------------------------- */
    const isFilterEmpty = Object.keys(filter).length === 0;

    if (isFilterEmpty) {
      const result = await TopSearch.find()
        .sort({ count: -1 })
        .limit(Number(limit))
        .lean();

      return res.json({
        success: true,
        mode: "top_default",
        data: result
      });
    }

    /* ----------------------------------
     * Nếu có filter → query TopSearch
     * ---------------------------------- */
    const result = await TopSearch.find(filter)
      .sort(sort)
      .limit(Number(limit))
      .lean();

    res.json({
      success: true,
      mode: "filtered",
      appliedFilter: filter,
      data: result
    });

  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

module.exports = router;
