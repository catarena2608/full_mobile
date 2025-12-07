// routes/routes.config.js
module.exports = {
  "/api/auth": "http://backend-auth:3001",  // Auth service
  "/api/users": "http://backend-user:3002", // User service
  "/api/follow": "http://backend-user:3002",
  "/api/post": "http://backend-post:4001", // Post service
  "/api/save": "http://backend-post:4001",
  "/api/like": "http://backend-post:4001",
  "/api/comment": "http://backend-post:4001",
  "/api/recipe": "http://backend-recipe:5001", // Recipe service
};
