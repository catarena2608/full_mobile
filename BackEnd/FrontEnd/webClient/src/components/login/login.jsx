import { useEffect } from "react";
import {Flex,Form,Input,Button,Typography,Checkbox,Card,message,} from "antd";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { useDispatch } from "react-redux";
import { setUser, clearUser } from "../../../features/userData/userDataSlice";
import "./login.scss";

const { Title, Text, Link } = Typography;

const Login = () => {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const dispatch = useDispatch();

  // 🧠 Khi reload trang — kiểm tra cookie login
  useEffect(() => {
    const checkAuth = async () => {
      try {
        const res = await axios.get("http://localhost:3000/api/auth/me", {
          withCredentials: true, // gửi cookie HTTP-only
        });
        if (res.status === 200 && res.data) {
          dispatch(setUser(res.data.user)); // lưu user vào redux
          navigate("/"); // điều hướng sang layout chính
        }
      } catch (err) {
        dispatch(clearUser());
        // chưa login thì ở lại trang login
      }
    };
    checkAuth();
  }, [navigate, dispatch]);

  // 🧠 Khi submit form login
  const onFinish = async (values) => {
    try {
      const res = await axios.post(
        "http://localhost:3000/api/auth/login",
        values,
        {
          withCredentials: true,
          headers: { "Content-Type": "application/json" },
        }
      );

      message.success("Đăng nhập thành công!");
      console.log("✅ Success:", res.data);

      // lưu user vào redux
      dispatch(setUser(res.data.user));

      // chuyển sang layout chính
      navigate("/");
    } catch (err) {
      console.error(err);
      const msg = err.response?.data?.message || "Đăng nhập thất bại!";
      message.error(msg);
    }
  };

  const onFinishFailed = (errorInfo) => {
    console.log("❌ Failed:", errorInfo);
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
          width: 380,
          borderRadius: 12,
          boxShadow: "0 4px 16px rgba(0,0,0,0.08)",
        }}
      >
        <Flex vertical gap="small">
          <div style={{ textAlign: "center", marginBottom: 24 }}>
            <Title level={3} style={{ marginBottom: 4 }}>
              Welcome back 👋
            </Title>
            <Text type="secondary">Please sign in to continue</Text>
          </div>

          <Form
            form={form}
            name="login"
            layout="vertical"
            initialValues={{ remember: true }}
            onFinish={onFinish}
            onFinishFailed={onFinishFailed}
            autoComplete="off"
          >
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
              rules={[{ required: true, message: "Please enter your password" }]}
            >
              <Input.Password placeholder="••••••••" size="large" />
            </Form.Item>

            <Flex justify="space-between" align="center">
              <Form.Item name="remember" valuePropName="checked" noStyle>
                <Checkbox>Remember me</Checkbox>
              </Form.Item>
              <Link href="/forgetPassword" type="secondary">
                Forgot password?
              </Link>
            </Flex>

            <Form.Item style={{ marginTop: 16 }}>
              <Button type="primary" htmlType="submit" size="large" block>
                Sign In
              </Button>
            </Form.Item>
          </Form>

          <Text style={{ textAlign: "center" }}>
            Don’t have an account?{" "}
            <Link href="/register" type="secondary">
              Sign up
            </Link>
          </Text>
        </Flex>
      </Card>
    </Flex>
  );
};

export default Login;
