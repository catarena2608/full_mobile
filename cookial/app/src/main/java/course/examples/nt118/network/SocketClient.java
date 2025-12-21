package course.examples.nt118.network;

import android.util.Log;
import org.json.JSONObject;
import org.json.JSONArray; // <--- Thêm import này
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
        return "http://136.110.31.88:6001";
    }

    public void connect(String jwtToken) {
        if (mSocket != null && mSocket.connected()) {
            Log.d(TAG, "Socket đã kết nối, bỏ qua.");
            // ⚠️ QUAN TRỌNG: Nếu đã kết nối rồi, có thể Activity vừa mở lên sẽ bị lỡ mất sự kiện init.
            // Ta có thể chủ động emit yêu cầu lấy lại list nếu cần (Tùy logic server).
            // mSocket.emit("get_init_notifications");
            return;
        }

        if (jwtToken == null || jwtToken.isEmpty()) {
            Log.e(TAG, "❌ Token trống! Không thể kết nối Socket.");
            return;
        }

        try {
            IO.Options options = new IO.Options();
            options.transports = new String[] { WebSocket.NAME };

            // Auth Payload
            Map<String, String> auth = new HashMap<>();
            auth.put("token", jwtToken);
            options.auth = auth;

            // Headers
            Map<String, List<String>> headers = new HashMap<>();
            String bearerToken = jwtToken.startsWith("Bearer ") ? jwtToken : "Bearer " + jwtToken;
            headers.put("Authorization", Collections.singletonList(bearerToken));
            options.extraHeaders = headers;

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
                Log.e(TAG, "❌ Lỗi kết nối Socket: " + ((Exception) args[0]).getMessage());
            }
        });

        mSocket.on(Socket.EVENT_DISCONNECT, args ->
                Log.w(TAG, "⚠️ Socket đã ngắt kết nối.")
        );

        /* =======================================================
           1. LẮNG NGHE DANH SÁCH THÔNG BÁO (Lúc mới connect)
           Server backend của bạn tự động emit cái này sau khi verify token
           ======================================================= */
        mSocket.on("init_notifications", args -> {
            try {
                // Backend trả về Array, nên ép kiểu sang JSONArray
                JSONArray data = (JSONArray) args[0];
                Log.d(TAG, "📥 Socket nhận danh sách init: " + data.length() + " items");

                // Bắn EventBus chứa JSONArray sang Activity
                EventBus.getDefault().post(new NotifyEvent(data));

            } catch (Exception e) {
                Log.e(TAG, "Lỗi parse init_notifications", e);
            }
        });

        /* =======================================================
           2. LẮNG NGHE THÔNG BÁO MỚI (Realtime)
           ======================================================= */
        mSocket.on("notify", args -> {
            try {
                // Backend trả về Object lẻ
                JSONObject data = (JSONObject) args[0];
                Log.d(TAG, "🔔 Socket nhận notify mới: " + data.toString());

                // Bắn EventBus chứa JSONObject sang Activity
                EventBus.getDefault().post(new NotifyEvent(data));

            } catch (Exception e) {
                Log.e(TAG, "Lỗi parse notify lẻ", e);
            }
        });
    }

    public void disconnect() {
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off();
            mSocket = null;
        }
    }

    public Socket getSocket() {
        return mSocket;
    }

    public boolean isConnected() {
        return mSocket != null && mSocket.connected();
    }
}

