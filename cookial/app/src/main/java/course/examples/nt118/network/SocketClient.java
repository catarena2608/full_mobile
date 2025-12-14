package course.examples.nt118.network;

import android.util.Log;
import org.json.JSONObject;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.WebSocket;
import org.greenrobot.eventbus.EventBus;
import course.examples.nt118.model.NotifyEvent;

public class SocketClient {
    private static final String TAG = "SocketClient";
    private static SocketClient instance;
    private Socket mSocket;

    // Singleton
    public static synchronized SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    private SocketClient() { }

    private String getSocketUrl() {
        // Trả về IP/Domain của Server Socket
        // Lưu ý: Nếu server chạy local hoặc port khác, hãy sửa lại cho đúng
        return "http://136.110.31.88:6001";
    }

    public void connect(String jwtToken) {
        if (mSocket != null && mSocket.connected()) {
            Log.d(TAG, "Socket đã kết nối, bỏ qua.");
            return;
        }

        if (jwtToken == null || jwtToken.isEmpty()) {
            Log.e(TAG, "❌ Token trống! Không thể kết nối Socket.");
            return;
        }

        try {
            IO.Options options = new IO.Options();

            // 1. Bắt buộc dùng WebSocket để tránh lỗi 400/Session ID unknown trên Load Balancer
            options.transports = new String[] { WebSocket.NAME };

            // ==================================================================
            // 🔴 FIX LỖI 401: Gửi Token bằng cả 2 cách để chắc chắn Server nhận được
            // ==================================================================

            // CÁCH 1: Gửi qua Auth Payload (Chuẩn Socket.IO v3/v4)
            // Server nhận tại: socket.handshake.auth.token
            Map<String, String> auth = new HashMap<>();
            auth.put("token", jwtToken);
            options.auth = auth;

            // CÁCH 2: Gửi qua HTTP Headers (Chuẩn REST API / Middleware Express)
            // Server nhận tại: socket.handshake.headers.authorization
            Map<String, List<String>> headers = new HashMap<>();
            // Tự động thêm tiền tố "Bearer " nếu token chưa có
            String bearerToken = jwtToken.startsWith("Bearer ") ? jwtToken : "Bearer " + jwtToken;
            headers.put("Authorization", Collections.singletonList(bearerToken));
            options.extraHeaders = headers;

            // ==================================================================

            // Cấu hình Reconnect
            options.reconnection = true;
            options.reconnectionAttempts = 5;
            options.reconnectionDelay = 2000;

            String url = getSocketUrl();
            Log.d(TAG, "🚀 Đang kết nối Socket tới: " + url);
            mSocket = IO.socket(url, options);

            initSystemListeners();

            mSocket.connect();

        } catch (URISyntaxException e) {
            Log.e(TAG, "❌ Lỗi cú pháp URL Socket", e);
        }
    }

    private void initSystemListeners() {
        if (mSocket == null) return;

        mSocket.on(Socket.EVENT_CONNECT, args ->
                Log.i(TAG, "✅ Socket đã kết nối thành công! ID: " + mSocket.id())
        );

        mSocket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            if (args.length > 0 && args[0] instanceof Exception) {
                Exception e = (Exception) args[0];
                Log.e(TAG, "❌ Lỗi kết nối Socket: " + e.getMessage());
                // Nếu vẫn bị 401, hãy kiểm tra lại Token có hết hạn không
                if (e.getMessage().contains("401")) {
                    Log.e(TAG, "👉 Token có thể đã hết hạn hoặc Server từ chối xác thực.");
                }
            }
        });

        mSocket.on(Socket.EVENT_DISCONNECT, args ->
                Log.w(TAG, "⚠️ Socket đã ngắt kết nối.")
        );

        mSocket.on("notify", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                Log.d(TAG, "📩 Socket nhận tin: " + data.toString());

                // 🔥 BẮN EVENT RA TOÀN APP
                // post() có thể gọi từ background thread, các Activity sẽ nhận được
                EventBus.getDefault().post(new NotifyEvent(data));

            } catch (Exception e) {
                Log.e(TAG, "Lỗi parse data notify", e);
            }
        });
    }

    public void disconnect() {
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off();
            mSocket = null;
            Log.d(TAG, "🛑 Đã đóng kết nối Socket.");
        }
    }

    public Socket getSocket() {
        return mSocket;
    }

    public boolean isConnected() {
        return mSocket != null && mSocket.connected();
    }
}