require("dotenv").config();
const express = require("express");
const http = require("http");
const cors = require("cors");
const cookieParser = require("cookie-parser");

const checkAdmin = require("./utils/checkAdmin");

// Route maps
const noRequireMap = require("./routes/routes.noRequire");
const tokenMap = require("./routes/routes.token");
const actionMap = require("./routes/routes.action");

// Gộp map chung
const serviceMap = { ...noRequireMap, ...tokenMap, ...actionMap };

const app = express();

app.use(cookieParser());
app.use(
  cors({
    origin: ["http://localhost:5173"],
    credentials: true,
  })
);

// ------------- APPLY GROUP MIDDLEWARE ----------------
function applyMiddleware(map, middleware) {
  Object.keys(map).forEach(prefix => app.use(prefix, middleware));
}

applyMiddleware(noRequireMap, (req, _, next) => {
  console.log(`[NoRequire] ${req.originalUrl}`);
  next();
});

applyMiddleware(tokenMap, checkAdmin(false));
applyMiddleware(actionMap, checkAdmin(true));


// ----------------------- PROXY ------------------------
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

  // Clone header (xóa host)
  const headers = { ...req.headers };
  delete headers.host;

  // Inject admin info
  if (req.admin) {
    headers["x-admin-id"] = req.admin.id;
    headers["x-admin-action"] = req.admin.action;
  }

  const proxyReq = http.request(
    targetUrl,
    { method: req.method, headers },
    (proxyRes) => {
      res.writeHead(proxyRes.statusCode, proxyRes.headers);
      proxyRes.pipe(res);
    }
  );

  proxyReq.on("error", (err) => {
    console.error(`[Gateway ❌] ${req.method} ${targetUrl.href} → ${err.message}`);
    if (!res.headersSent)
      res.status(502).json({ message: "Lỗi kết nối tới service nội bộ" });
  });

  req.pipe(proxyReq);
});


// ------------------------ ROOT ------------------------
app.get("/", (req, res) => res.send("🌐 API Gateway đang hoạt động! 🚀"));

// ------------------------ START -----------------------
const PORT = process.env.GATEWAY_PORT || 9000;
app.listen(PORT, () => {
  console.log(`🚪 Gateway chạy ở http://localhost:${PORT}`);
});