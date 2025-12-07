package course.examples.nt118;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.json.JSONObject;

import course.examples.nt118.adapter.NotificationAdapter;
import course.examples.nt118.databinding.ActivityNotificationBinding;
import course.examples.nt118.model.Notify;
import course.examples.nt118.network.SocketClient;
import course.examples.nt118.utils.TokenManager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";
    private ActivityNotificationBinding binding;
    private NotificationAdapter adapter; // Dùng 1 adapter duy nhất
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupViews();
        setupRecyclerView();

        // KHÔNG GỌI API NỮA
        // Chỉ kích hoạt lắng nghe Socket
        initSocketListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Gỡ listener khi thoát để tránh memory leak
        Socket socket = SocketClient.getInstance().getSocket();
        if (socket != null) {
            socket.off("notify", onNewNotification);
        }
    }

    private void setupViews() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Ẩn các thành phần không dùng đến do không có API lịch sử
        binding.layoutEarlierNotifications.setVisibility(View.GONE);
        binding.btnSeeEarlier.setVisibility(View.GONE);

        // Sửa label "Hôm nay" thành "Thông báo mới" hoặc ẩn đi tùy bạn
        binding.tvTodayLabel.setText("Thông báo trực tiếp");
        // Mặc định ẩn label đi, có thông báo mới hiện
        binding.tvTodayLabel.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        // Chỉ setup 1 RecyclerView (rvNotificationsToday) để hứng data socket
        adapter = new NotificationAdapter(this, this::onNotificationClick);
        binding.rvNotificationsToday.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotificationsToday.setAdapter(adapter);
    }

    // ================== SOCKET IO LOGIC ==================

    private void initSocketListener() {
        // 1. Kiểm tra kết nối, nếu chưa thì connect bằng Token từ Cookie
        if (!SocketClient.getInstance().isConnected()) {
            // Logic lấy token trực tiếp từ Cookie mà ta đã bàn ở câu trước
            String token = TokenManager.getTokenFromCookie(this);
            if (!token.isEmpty()) {
                SocketClient.getInstance().connect(token);
            } else {
                Log.e(TAG, "Không tìm thấy Token trong Cookie!");
                return;
            }
        }

        // 2. Đăng ký sự kiện
        Socket socket = SocketClient.getInstance().getSocket();
        if (socket != null) {
            // Xóa listener cũ để tránh trùng lặp
            socket.off("notify", onNewNotification);

            // Đăng ký mới
            socket.on("notify", onNewNotification);
            Log.d(TAG, "✅ Đang lắng nghe sự kiện 'notify'...");
        }
    }

    /**
     * Xử lý khi Server bắn sự kiện 'notify'
     */
    private final Emitter.Listener onNewNotification = args -> {
        runOnUiThread(() -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    Log.d(TAG, "📩 Nhận socket: " + data.toString());

                    // Parse JSON sang Object
                    Notify newNoti = gson.fromJson(data.toString(), Notify.class);

                    if (newNoti != null) {
                        // Thêm vào đầu danh sách
                        if (adapter != null) {
                            adapter.addNotificationToTop(newNoti);

                            // Scroll lên đầu
                            binding.rvNotificationsToday.smoothScrollToPosition(0);

                            // Hiện label nếu đây là thông báo đầu tiên
                            binding.tvTodayLabel.setVisibility(View.VISIBLE);
                        }
                    }

                } catch (JsonSyntaxException e) {
                    Log.e(TAG, "Lỗi format JSON từ Socket", e);
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi xử lý Socket", e);
                }
            }
        });
    };

    // ================== UTILS ==================

    private void onNotificationClick(Notify noti) {
        String targetId = noti.getTargetId();
        // String type = noti.getType(); // Dùng biến này nếu muốn chia case

        if (targetId != null) {
            Intent intent = new Intent(this, PostDetailActivity.class);
            intent.putExtra("POST_ID", targetId);
            startActivity(intent);
        }
    }
}