package course.examples.nt118;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import course.examples.nt118.adapter.NotificationAdapter;
import course.examples.nt118.databinding.ActivityNotificationBinding;
import course.examples.nt118.model.Notify;
import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import course.examples.nt118.network.SocketClient;
import course.examples.nt118.utils.TokenManager;
import io.socket.emitter.Emitter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";
    private ActivityNotificationBinding binding;

    private NotificationAdapter adapterToday;
    private NotificationAdapter adapterEarlier;

    private String userId;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Lấy UserID từ SharedPrefs (TokenManager)
        userId = TokenManager.getUserId(this);

        setupViews();
        setupRecyclerViews();

        // 1. Gọi API lấy dữ liệu lịch sử (REST API)
        fetchNotifications();

        // 2. Kích hoạt lắng nghe Socket (Real-time)
        initSocketListener();
    }

    private void setupViews() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Nút "Xem trước đó"
        binding.btnSeeEarlier.setOnClickListener(v -> {
            binding.btnSeeEarlier.setVisibility(View.GONE);
            binding.layoutEarlierNotifications.setVisibility(View.VISIBLE);
        });
    }

    private void setupRecyclerViews() {
        // Adapter cho thông báo hôm nay
        adapterToday = new NotificationAdapter(this, this::onNotificationClick);
        binding.rvNotificationsToday.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotificationsToday.setAdapter(adapterToday);

        // Adapter cho thông báo cũ hơn
        adapterEarlier = new NotificationAdapter(this, this::onNotificationClick);
        binding.rvNotificationsEarlier.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotificationsEarlier.setAdapter(adapterEarlier);
    }

    private void fetchNotifications() {
        ApiService api = RetrofitClient.getInstance(this).getApiService();

        api.getNotifications(userId).enqueue(new Callback<List<Notify>>() {
            @Override
            public void onResponse(Call<List<Notify>> call, Response<List<Notify>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    filterNotifications(response.body());
                } else {
                    Log.w(TAG, "API Error or Empty: " + response.code());
                    // Ẩn loading hoặc hiện thông báo trống nếu cần
                }
            }

            @Override
            public void onFailure(Call<List<Notify>> call, Throwable t) {
                Log.e(TAG, "API Failure", t);
                Toast.makeText(NotificationActivity.this, "Không thể tải thông báo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterNotifications(List<Notify> all) {
        List<Notify> todayList = new ArrayList<>();
        List<Notify> earlierList = new ArrayList<>();

        for (Notify noti : all) {
            if (isDateToday(noti.getCreatedAt())) {
                todayList.add(noti);
            } else {
                earlierList.add(noti);
            }
        }

        // Cập nhật UI
        adapterToday.setData(todayList);
        adapterEarlier.setData(earlierList);

        binding.tvTodayLabel.setVisibility(todayList.isEmpty() ? View.GONE : View.VISIBLE);

        // Logic hiển thị nút "Xem cũ hơn"
        if (earlierList.isEmpty()) {
            binding.btnSeeEarlier.setVisibility(View.GONE);
            binding.layoutEarlierNotifications.setVisibility(View.GONE);
        } else {
            binding.btnSeeEarlier.setVisibility(View.VISIBLE);
        }
    }

    // ================== SOCKET IO LOGIC ==================

    private void initSocketListener() {
        // Kiểm tra token trước khi connect
        String token = TokenManager.getTokenFromCookie(this); // Đảm bảo hàm này trả về Raw JWT String

        if (token == null || token.isEmpty()) {
            Log.e(TAG, "No token found, cannot connect Socket.");
            return;
        }

        // Nếu chưa kết nối thì kết nối lại
        if (!SocketClient.isConnected()) {
            SocketClient.connect(token);
        }

        // Đăng ký sự kiện
        if (SocketClient.getSocket() != null) {
            // Xóa listener cũ để tránh bị duplicate event (nhận 2 lần thông báo)
            SocketClient.getSocket().off("notify", onNewNotification);

            // Đăng ký mới
            SocketClient.getSocket().on("notify", onNewNotification);
            Log.i(TAG, "✅ Đã đăng ký lắng nghe sự kiện 'notify'");
        }
    }

    /**
     * Listener xử lý sự kiện real-time
     */
    private final Emitter.Listener onNewNotification = args -> {
        // Socket.IO chạy trên background thread, bắt buộc dùng runOnUiThread để vẽ UI
        runOnUiThread(() -> {
            if (args.length > 0 && args[0] instanceof JSONObject) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    Log.d(TAG, "📩 Socket Data: " + data.toString());

                    // Parse JSON sang Object Notify
                    Notify newNoti = gson.fromJson(data.toString(), Notify.class);

                    // Kiểm tra null để tránh crash
                    if (newNoti == null) return;

                    // Chỉ thêm vào danh sách "Hôm nay"
                    if (adapterToday != null) {
                        // Thêm vào đầu danh sách (index 0)
                        adapterToday.addNotificationToTop(newNoti);

                        // Scroll lên đầu để user thấy
                        binding.rvNotificationsToday.smoothScrollToPosition(0);
                          binding.tvTodayLabel.setVisibility(View.VISIBLE);

                        // Có thể hiện thêm 1 Toast nhỏ hoặc rung điện thoại
                        // Toast.makeText(NotificationActivity.this, "Bạn có thông báo mới!", Toast.LENGTH_SHORT).show();
                    }

                } catch (JsonSyntaxException e) {
                    Log.e(TAG, "Gson Parse Error", e);
                } catch (Exception e) {
                    Log.e(TAG, "Socket Handle Error", e);
                }
            }
        });
    };

    // ================== UTILS ==================

    private boolean isDateToday(String dateString) {
        if (dateString == null) return false;
        try {
            // Format này phải khớp với định dạng server trả về (ISO 8601)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Server thường trả về giờ UTC

            Date date = sdf.parse(dateString);
            if (date != null) {
                return DateUtils.isToday(date.getTime());
            }
        } catch (Exception e) {
            // Thử format dự phòng nếu server trả về kiểu khác (ít mili giây hơn chẳng hạn)
            Log.w(TAG, "Date parse warning: " + dateString);
        }
        return false;
    }

    private void onNotificationClick(Notify noti) {
        // Backend trả về field là targetID, hãy chắc chắn getter trong model Notify đúng
        String postId = noti.getTargetId(); // Hoặc getTargetPostID() tùy model của bạn

        if (postId != null && !postId.isEmpty()) {
            Intent intent = new Intent(this, PostDetailActivity.class);
            intent.putExtra("POST_ID", postId);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Không tìm thấy bài viết liên quan", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cực kỳ quan trọng: Gỡ listener khi thoát màn hình
        if (SocketClient.getSocket() != null) {
            SocketClient.getSocket().off("notify", onNewNotification);
        }
    }
}