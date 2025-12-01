require("dotenv").config();
const express = require("express");
const http = require("http");
const cors = require("cors");
const cookieParser = require("cookie-parser");

const checkAdmin = require("./utils/checkAdmin");

// 3 file service map (đều là object)
const noRequireMap = require("./routes/routes.noRequire"); // e.g. { "/stat/login": "http://localhost:7001" }
const tokenMap = require("./routes/routes.token");           // object
const actionMap = require("./routes/routes.action");         // object

// Gộp tất cả map thành 1 serviceMap
const serviceMap = Object.assign({}, noRequireMap, tokenMap, actionMap);

const app = express();

app.use(cookieParser());
app.use(
  cors({
    origin: ["http://localhost:5173"],
    credentials: true,
  })
);

/* -----------------------------------------
   🔐 MIDDLEWARE PHÂN QUYỀN THEO NHÓM ROUTE
------------------------------------------*/

// 1) Không cần token
Object.keys(noRequireMap).forEach((prefix) => {
  app.use(prefix, (req, res, next) => {
    console.log(`[NoRequire] ${req.originalUrl}`);
    next();
  });
});

// 2) Cần token nhưng không cần action
Object.keys(tokenMap).forEach((prefix) => {
  app.use(prefix, checkAdmin(false), (req, res, next) => {
    console.log(`[Token] ${req.originalUrl}`);
    next();
  });
});

// 3) Cần token + yêu cầu action
Object.keys(actionMap).forEach((prefix) => {
  app.use(prefix, checkAdmin(true), (req, res, next) => {
    console.log(`[ActionRequired] ${req.originalUrl}`);
    next();
  });
});

/* -----------------------------------------
   🔁 PROXY CHUNG
------------------------------------------*/
app.use("/", (req, res) => {
  let targetBase = null;

  for (const prefix in serviceMap) {
    if (req.originalUrl.startsWith(prefix)) {
      targetBase = serviceMap[prefix];
      break;
    }
  }

  if (!targetBase) {
    console.log(`[Gateway ❌] Không tìm thấy service cho ${req.originalUrl}`);
    return res.status(404).json({ message: "Không tìm thấy service phù hợp" });
  }

  const targetUrl = new URL(targetBase + req.originalUrl);

  console.log(`[Gateway 🚀] ${req.method} ${req.originalUrl} → ${targetUrl.href}`);

  const headers = { ...req.headers };
  delete headers.host;

  if (req.admin) {
  headers["x-admin-id"] = req.admin.id;      // <---- FIXED
  headers["x-admin-action"] = req.admin.action;
}

  const options = { method: req.method, headers };

  const proxyReq = http.request(targetUrl, options, (proxyRes) => {
    res.status(proxyRes.statusCode);
    Object.entries(proxyRes.headers).forEach(([k, v]) => res.setHeader(k, v));
    proxyRes.pipe(res);
  });

  proxyReq.on("error", (err) => {
    console.error(`[Gateway ❌] ${req.method} ${targetUrl.href} → ${err.message}`);
    if (!res.headersSent) {
      res.status(502).json({ message: "Lỗi kết nối tới service nội bộ" });
    }
  });

  req.pipe(proxyReq);
});

// ================== ROOT ==================
app.get("/", (req, res) => res.send("🌐 API Gateway đang hoạt động! 🚀"));

// ================== START ==================
const PORT = process.env.GATEWAY_PORT || 9000;
app.listen(PORT, () => {
  console.log(`🚪 Gateway chạy ở http://localhost:${PORT}`);
});
