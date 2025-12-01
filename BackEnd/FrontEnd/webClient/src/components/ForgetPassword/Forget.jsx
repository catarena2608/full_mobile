import { useState } from "react";
import { Input, Button, message, Card, Typography } from "antd";
import { LockOutlined, MailOutlined, NumberOutlined } from "@ant-design/icons";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "./Forget.scss";

const { Title, Text } = Typography;

const Forget = () => {
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const navigate = useNavigate();

  const handleSendOTP = async () => {
    if (!email) return message.warning("Vui lòng nhập email");
    try {
      setLoading(true);
      await axios.post("http://localhost:3000/api/auth/forgot-password", { email });
      message.success("Đã gửi OTP qua email 🎉");
      setStep(2);
    } catch (err) {
      message.error(err.response?.data?.message || "Lỗi gửi OTP");
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOTP = async () => {
    if (otp.length !== 6) return message.warning("Vui lòng nhập đủ 6 số OTP");
    try {
      setLoading(true);
      await axios.post("http://localhost:3000/api/auth/verify-otp", { email, otp });
      message.success("OTP hợp lệ ✅");
      setStep(3);
    } catch (err) {
      message.error(err.response?.data?.message || "OTP không hợp lệ");
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async () => {
    if (password.length < 6) return message.warning("Mật khẩu ít nhất 6 ký tự");
    if (password !== confirm) return message.error("Mật khẩu xác nhận không khớp");
    try {
      setLoading(true);
      await axios.post("http://localhost:3000/api/auth/reset-password", { email, password });
      message.success("Đặt lại mật khẩu thành công 🎉");
      setTimeout(() => navigate("/login"), 1500);
    } catch (err) {
      message.error(err.response?.data?.message || "Lỗi đặt lại mật khẩu");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="forget-container">
      <Card className="forget-card" bordered={false}>
        <Title level={3} className="forget-title">Khôi phục mật khẩu</Title>

        {step === 1 && (
          <div className="forget-step fade-in">
            <Text className="forget-text">Nhập email để nhận mã OTP</Text>
            <Input
              size="large"
              prefix={<MailOutlined />}
              placeholder="Email của bạn"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            <Button
              type="primary"
              size="large"
              block
              loading={loading}
              className="mt-3"
              onClick={handleSendOTP}
            >
              Gửi mã OTP
            </Button>
          </div>
        )}

        {step === 2 && (
          <div className="forget-step fade-in">
            <Text className="forget-text">Nhập mã OTP gồm 6 chữ số</Text>
            <div className="otp-box">
              {Array.from({ length: 6 }).map((_, i) => (
                <Input
                  key={i}
                  maxLength={1}
                  value={otp[i] || ""}
                  onChange={(e) => {
                    const value = e.target.value.replace(/\D/, "");
                    setOtp((prev) => {
                      const arr = prev.split("");
                      arr[i] = value;
                      return arr.join("");
                    });
                  }}
                  className="otp-input"
                />
              ))}
            </div>
            <Button
              type="primary"
              size="large"
              block
              loading={loading}
              className="mt-3"
              onClick={handleVerifyOTP}
            >
              Xác nhận OTP
            </Button>
          </div>
        )}

        {step === 3 && (
          <div className="forget-step fade-in">
            <Text className="forget-text">Nhập mật khẩu mới của bạn</Text>
            <Input.Password
              size="large"
              prefix={<LockOutlined />}
              placeholder="Mật khẩu mới"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="mb-3"
            />
            <Input.Password
              size="large"
              prefix={<LockOutlined />}
              placeholder="Xác nhận mật khẩu"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
            />
            <Button
              type="primary"
              size="large"
              block
              loading={loading}
              className="mt-3"
              onClick={handleResetPassword}
            >
              Đặt lại mật khẩu
            </Button>
          </div>
        )}
      </Card>
    </div>
  );
};

export default Forget;
