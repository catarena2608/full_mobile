import { Navigate } from "react-router-dom";
import axios from "axios";
import { useEffect, useState } from "react";

function ProtectedRoute({ children }) {
  const [isAuth, setIsAuth] = useState(null);

  useEffect(() => {
    const checkAuth = async () => {
      try {
        // Gọi backend để xác minh cookie token (HTTP-only)
        const res = await axios.get("http://localhost:3000/api/auth/me", {
          withCredentials: true, // bắt buộc để gửi cookie
        });
        if (res.status === 200) setIsAuth(true);
      } catch (err) {
        setIsAuth(false);
      }
    };
    checkAuth();
  }, []);

  if (isAuth === null) return <div>Loading...</div>;
  return isAuth ? children : <Navigate to="/login" replace />;
}

export default ProtectedRoute;
