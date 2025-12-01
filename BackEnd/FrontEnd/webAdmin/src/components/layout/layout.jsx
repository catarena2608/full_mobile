import { Layout, Flex, Menu, Button, Space } from "antd";
import { MailOutlined, AppstoreOutlined, SettingOutlined } from "@ant-design/icons";
import "./layout.scss";
import { Outlet, Link } from "react-router-dom";
import axios from "axios";
import { useNavigate } from "react-router-dom";
const { Header, Footer, Sider, Content } = Layout;

const items = [
  {
    key: "sub1",
    label: "Navigation One",
    icon: <MailOutlined />,
    children: [
      {
        key: "g1",
        label: "Item 1",
        type: "group",
        children: [
          { key: "1", label: "Option 1" },
          { key: "2", label: "Option 2" },
        ],
      },
      {
        key: "g2",
        label: "Item 2",
        type: "group",
        children: [
          { key: "3", label: "Option 3" },
          { key: "4", label: "Option 4" },
        ],
      },
    ],
  },
  {
    key: "sub2",
    label: "Navigation Two",
    icon: <AppstoreOutlined />,
    children: [
      { key: "5", label: "Option 5" },
      { key: "6", label: "Option 6" },
      {
        key: "sub3",
        label: "Submenu",
        children: [
          { key: "7", label: "Option 7" },
          { key: "8", label: "Option 8" },
        ],
      },
    ],
  },
  {
    type: "divider",
  },
  {
    key: "sub4",
    label: "Navigation Three",
    icon: <SettingOutlined />,
    children: [
      { key: "9", label: "Option 9" },
      { key: "10", label: "Option 10" },
      { key: "11", label: "Option 11" },
      { key: "12", label: "Option 12" },
    ],
  },
  {
    key: "grp",
    label: "Group",
    type: "group",
    children: [
      { key: "13", label: "Option 13" },
      { key: "14", label: "Option 14" },
    ],
  },
];
const Layout1 = () => {
  const navigate = useNavigate();
  const handleLogout = async () => {
    try {
      await axios.post("http://localhost:3000/api/auth/logout", {}, { withCredentials: true });
      navigate("/login"); // chuyển thẳng về trang login
    } catch (err) {
      console.error(err);
    }
  };
  return (
    <Layout>
      {/* --- HEADER --- */}
      <Header>
        <Flex justify="space-between" align="center">
          <Flex align="center" gap="middle" onClick={()=>navigate("/")}>
            <img
              src="https://global-web-assets.cpcdn.com/assets/logo_circle-d106f02123de882fffdd2c06593eb2fd33f0ddf20418dd75ed72225bdb0e0ff7.png"
              alt="CookPad Logo"
              style={{ width: 40, height: 40 }}
            />
            <Link to="/" >CookPad</Link>
          </Flex>
          <Space>
            <Button onClick={()=>navigate("/uploadPost")}>Up Post</Button>
            <Button onClick={()=>navigate("/users")}>Users</Button>
            <Button onClick={()=>navigate("/myProfile")}>My Profile</Button>
            <Button onClick={handleLogout}>Logout</Button>
          </Space>
        </Flex>
      </Header>

      {/* --- BODY --- */}
      <Layout>
        <Sider theme="light" width={250}>
          <Menu
            defaultSelectedKeys={["1"]}
            defaultOpenKeys={["sub1"]}
            mode="inline"
            items={items}
          />
        </Sider>

        <Content style={{ padding: 24 }}><Outlet /></Content>
      </Layout>

      {/* --- FOOTER --- */}
      <Footer style={{ backgroundColor: "#fff", padding: "40px 24px" }}>
        <div className="footer-container">
          <div className="footer-section">
            <h4>Về Cookpad</h4>
            <p>
              Sứ mệnh của Cookpad là{" "}
              <b>làm cho việc vào bếp vui hơn mỗi ngày</b>, vì chúng tôi tin rằng
              nấu nướng là chìa khoá cho một cuộc sống hạnh phúc và khoẻ mạnh hơn.
            </p>
            <p>
              <a href="/vn/premium?via=footer_about_us">Đăng ký gói Premium</a>{" "}
              để truy cập các chức năng và quyền lợi đặc biệt!
            </p>
          </div>

          <div className="footer-section">
            <h4>Tải Ứng Dụng</h4>
            <div style={{ display: "flex", gap: "10px" }}>
              <a
                href="https://play.google.com/store/apps/details?id=com.mufumbo.android.recipe.search"
                target="_blank"
                rel="noopener noreferrer"
              >
                <img
                  src="https://static.cookpad.com/global/assets/images/vi/button_google_play_store.svg"
                  alt="Google Play"
                  width="135"
                  height="40"
                />
              </a>

              <a
                href="https://apps.apple.com/vn/app/id585332633?l=vi"
                target="_blank"
                rel="noopener noreferrer"
              >
                <img
                  src="https://static.cookpad.com/global/assets/images/vi/button_apple_app_store.svg"
                  alt="App Store"
                  width="120"
                  height="40"
                />
              </a>
            </div>
          </div>
        </div>

        <div
          style={{
            textAlign: "center",
            marginTop: 32,
            fontSize: 12,
            color: "#777",
          }}
        >
          © Cookpad Inc. All Rights Reserved
        </div>
      </Footer>
    </Layout>
  );
};

export default Layout1;
