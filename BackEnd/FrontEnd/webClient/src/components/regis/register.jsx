import { useState } from "react";
import { Flex, Form, Input, Button, Typography, Card, message } from "antd";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "./register.scss";

const { Title, Text, Link } = Typography;

const Register = () => {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const onFinish = async (values) => {
    try {
      setLoading(true);

      const res = await axios.post(
        "http://localhost:3000/api/auth/register",
        values,
        {
          headers: { "Content-Type": "application/json" },
          withCredentials: true,
        }
      );

      message.success("Đăng ký thành công! Hãy đăng nhập nào 🚀");
      console.log("✅ Register Success:", res.data);

      navigate("/login"); // chuyển về trang login
    } catch (err) {
      console.error(err);
      const msg = err.response?.data?.message || "Đăng ký thất bại!";
      message.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Flex
      justify="center"
      align="center"
      style={{
        minHeight: "100vh",
        backgroundColor: "#f5f5f5",
        padding: "20px",
      }}
    >
      <Card
        style={{
          width: 420,
          borderRadius: 12,
          boxShadow: "0 4px 16px rgba(0,0,0,0.08)",
        }}
      >
        <Flex vertical gap="small">
          <div style={{ textAlign: "center", marginBottom: 24 }}>
            <Title level={3} style={{ marginBottom: 4 }}>
              Create an account ✨
            </Title>
            <Text type="secondary">Join us and get started today!</Text>
          </div>

          <Form
            name="register"
            layout="vertical"
            onFinish={onFinish}
            autoComplete="off"
          >
            <Form.Item
              label="Full Name"
              name="name"
              rules={[{ required: true, message: "Please enter your name" }]}
            >
              <Input placeholder="Nguyen Van A" size="large" />
            </Form.Item>

            <Form.Item
              label="Email"
              name="email"
              rules={[
                { required: true, message: "Please enter your email" },
                { type: "email", message: "Invalid email format" },
              ]}
            >
              <Input placeholder="yourname@email.com" size="large" />
            </Form.Item>

            <Form.Item
              label="Password"
              name="password"
              rules={[
                { required: true, message: "Please enter your password" },
                { min: 6, message: "Password must be at least 6 characters" },
              ]}
            >
              <Input.Password placeholder="••••••••" size="large" />
            </Form.Item>

            <Form.Item
              label="Avatar URL"
              name="avatar"
              rules={[{ required: true, message: "Please enter avatar URL" }]}
            >
              <Input placeholder="https://example.com/avatar.jpg" size="large" />
            </Form.Item>

            <Form.Item style={{ marginTop: 16 }}>
              <Button
                type="primary"
                htmlType="submit"
                size="large"
                block
                loading={loading}
              >
                Register
              </Button>
            </Form.Item>
          </Form>

          <Text style={{ textAlign: "center" }}>
            Already have an account?{" "}
            <Link onClick={() => navigate("/login")} type="secondary">
              Sign in
            </Link>
          </Text>
        </Flex>
      </Card>
    </Flex>
  );
};

export default Register;
