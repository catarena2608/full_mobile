package course.examples.nt118.network;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.WebSocket;
import course.examples.nt118.config.ApiConfig;
public class SocketClient {
    private static final String TAG = "SocketClient";
    private static Socket mSocket;

    private static final String SOCKET_SERVER_URL = "http://10.0.2.2:3000";

    // Server URL (Lưu ý: Dùng IP thật hoặc 10.0.2.2 nếu chạy máy ảo)
    // Nên lấy từ ApiConfig.getBaseUrl() nhưng bỏ phần "api/" đi nếu cần
    private static String getSocketServerUrl() {
        String baseUrl = ApiConfig.getBaseUrl();
        // Socket.IO thường chỉ cần URL gốc, không cần path API
        // Loại bỏ "/api/" nếu nó tồn tại ở cuối
        if (baseUrl.endsWith("/api/")) {
            return baseUrl.substring(0, baseUrl.length() - 5); // Cắt bỏ "/api/"
        } else if (baseUrl.endsWith("/api")) {
            return baseUrl.substring(0, baseUrl.length() - 4); // Cắt bỏ "/api"
        }
        return baseUrl; // Trả về nếu không có /api ở cuối
    }

    public static void connect(String jwtToken) {
        if (mSocket != null && mSocket.connected()) {
            return;
        }
        if (jwtToken == null || jwtToken.isEmpty()) {
            Log.e(TAG, "❌ Lỗi: Token bị rỗng, không thể kết nối Socket!");
            return;
        } else   Log.d(TAG, "token ok nhé " + jwtToken + " với token: " + jwtToken);

        // ... code options ...
        try {
            // 1. Cấu hình Options
            IO.Options options = new IO.Options();

            options.transports = new String[] { WebSocket.NAME };
            // [QUAN TRỌNG] Server Node.js của bạn yêu cầu: socket.handshake.auth.token
            // Nên ta phải gửi token vào Auth map
            Map<String, String> auth = Collections.singletonMap("token", jwtToken);
            options.auth = auth;

            // 2. Khởi tạo Socket
            mSocket = IO.socket(SOCKET_SERVER_URL, options);
            Log.d(TAG, "Đang kết nối tới " + SOCKET_SERVER_URL + " với token: " + jwtToken);

            // 3. Lắng nghe các sự kiện kết nối cơ bản để Debug
            mSocket.on(Socket.EVENT_CONNECT, args -> Log.i(TAG, "✅ Socket Connected! ID: " + mSocket.id()));
            mSocket.on(Socket.EVENT_CONNECT_ERROR, args -> Log.e(TAG, "❌ Connect Error: " + args[0]));
            mSocket.on(Socket.EVENT_DISCONNECT, args -> Log.w(TAG, "⚠️ Socket Disconnected"));

            mSocket.on("notify", args -> {
                // Socket.IO gửi dữ liệu về dưới dạng JSONObject (của org.json)
                JSONObject data = (JSONObject) args[0];
                Log.d(TAG, "📩 Nhận thông báo mới: " + data.toString());

                try {
                    String type = data.getString("type");
                    String actorID = data.getString("actorID");
                    // TODO: Gửi Broadcast hoặc cập nhật LiveData để hiển thị lên UI
                    // handleNewNotification(data);

                } catch (JSONException e) {
                    Log.e(TAG, "Lỗi parse JSON", e);
                }
            });

            // 4. Kết nối
            mSocket.connect();

        } catch (URISyntaxException e) {
            Log.e(TAG, "URI Error", e);
        }
    }

    public static void disconnect() {
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off(); // Gỡ bỏ tất cả listener để tránh leak memory
            mSocket = null;
        }
    }

    public static Socket getSocket() {
        return mSocket;
    }

    // Kiểm tra xem socket có đang sống không
    public static boolean isConnected() {
        return mSocket != null && mSocket.connected();
    }
}