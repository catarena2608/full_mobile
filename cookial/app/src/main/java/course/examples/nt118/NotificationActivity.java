package course.examples.nt118;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import course.examples.nt118.adapter.NotificationAdapter;
import course.examples.nt118.databinding.ActivityNotificationBinding;
import course.examples.nt118.model.Notify;
import course.examples.nt118.model.NotifyEvent;
import course.examples.nt118.model.UserResponse;
import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import course.examples.nt118.network.SocketClient;
import course.examples.nt118.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// ⚠️ Đảm bảo import đúng Activity Profile của bạn
// import course.examples.nt118.ui.profile.OtherUserProfileActivity;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";
    private static final String PREF_NAME = "MyNotificationCache";
    private static final String KEY_NOTIFY_LIST = "cached_notify_list";

    private ActivityNotificationBinding binding;
    private final Gson gson = new Gson();

    private NotificationAdapter todayAdapter;
    private NotificationAdapter earlierAdapter;

    private ApiService apiService;
    private String currentUserId;

    // List gốc chứa toàn bộ thông báo
    private final List<Notify> masterList = new ArrayList<>();

    // =================================================================
    // ♻️ LIFECYCLE METHODS (LOGGING ĐẦY ĐỦ)
    // =================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "♻️ Lifecycle: onCreate");

        binding = ActivityNotificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getInstance(this).getApiService();
        currentUserId = TokenManager.getUserId(this);

        setupViews();
        setupRecyclerViews();

        // 1. Load cache cũ lên ngay lập tức
        loadFromCache();

        // 2. Kết nối Socket
        connectSocketIfNeeded();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "♻️ Lifecycle: onStart");
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "♻️ Lifecycle: onResume");
        // Có thể refresh lại list ở đây nếu cần thiết
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "♻️ Lifecycle: onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "♻️ Lifecycle: onStop");
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "♻️ Lifecycle: onDestroy");
    }

    // =================================================================
    // 🛠 SETUP & UI
    // =================================================================

    private void setupViews() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.layoutEarlierNotifications.setVisibility(View.GONE);

        binding.btnSeeEarlier.setOnClickListener(v -> {
            binding.layoutEarlierNotifications.setVisibility(View.VISIBLE);
            binding.btnSeeEarlier.setVisibility(View.GONE);
        });
    }

    private void setupRecyclerViews() {
        todayAdapter = new NotificationAdapter(this, this::onNotificationClick);
        binding.rvNotificationsToday.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotificationsToday.setAdapter(todayAdapter);

        earlierAdapter = new NotificationAdapter(this, this::onNotificationClick);
        binding.rvNotificationsEarlier.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotificationsEarlier.setAdapter(earlierAdapter);
    }

    private void connectSocketIfNeeded() {
        if (!SocketClient.getInstance().isConnected()) {
            String token = TokenManager.getTokenFromCookie(this);
            if (token != null && !token.isEmpty()) {
                Log.d(TAG, "🔌 Connecting Socket...");
                SocketClient.getInstance().connect(token);
            }
        }
    }

    // =================================================================
    // 💾 CACHE MANAGER
    // =================================================================

    private void saveToCache() {
        try {
            SharedPreferences pref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            String jsonString = gson.toJson(masterList);
            editor.putString(KEY_NOTIFY_LIST, jsonString);
            editor.apply();
            Log.d(TAG, "💾 Saved cache: " + masterList.size() + " items");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error saving cache", e);
        }
    }

    private void loadFromCache() {
        try {
            SharedPreferences pref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String jsonString = pref.getString(KEY_NOTIFY_LIST, null);

            if (jsonString != null) {
                Type listType = new TypeToken<List<Notify>>() {}.getType();
                List<Notify> cachedList = gson.fromJson(jsonString, listType);
                if (cachedList != null) {
                    masterList.clear();
                    masterList.addAll(cachedList);
                    processNotificationList(masterList);
                    Log.d(TAG, "📂 Loaded cache: " + masterList.size() + " items");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error loading cache", e);
        }
    }

    // =================================================================
    // ⚡ EVENT BUS - SOCKET RECEIVER
    // =================================================================

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocketEvent(NotifyEvent event) {
        try {
            // Case 1: Nhận danh sách Init (Lần đầu vào)
            if (event.isList()) {
                JSONArray data = event.getArrayData();
                Log.d(TAG, "📥 SOCKET INIT DATA: " + data.toString()); // Log raw data để debug

                Type listType = new TypeToken<List<Notify>>() {}.getType();
                List<Notify> newNotifies = gson.fromJson(data.toString(), listType);

                if (newNotifies != null && !newNotifies.isEmpty()) {
                    masterList.clear();
                    masterList.addAll(newNotifies);

                    processNotificationList(masterList);
                    fetchUserInfoForList(masterList);
                    saveToCache();
                }
            }
            // Case 2: Nhận 1 thông báo Realtime mới
            else if (event.getJsonData() != null) {
                JSONObject data = event.getJsonData();
                Log.d(TAG, "🔔 SOCKET REALTIME DATA: " + data.toString()); // Log raw data để debug

                Notify newNoti = gson.fromJson(data.toString(), Notify.class);
                if (newNoti != null) {
                    masterList.add(0, newNoti); // Thêm vào đầu list
                    fetchActorInfo(newNoti, true); // True để scroll lên top
                    saveToCache();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error handling EventBus", e);
        }
    }

    // =================================================================
    // 🌐 API & LOGIC (Hydration)
    // =================================================================

    private void fetchUserInfoForList(List<Notify> allNotifies) {
        for (Notify notify : allNotifies) {
            fetchActorInfo(notify, false);
        }
    }

    private void fetchActorInfo(Notify notify, boolean isRealtime) {
        String actorId = notify.getActorId();
        if (actorId == null || actorId.isEmpty()) return;
        if (currentUserId == null) currentUserId = TokenManager.getUserId(this);

        apiService.getUserById(actorId, currentUserId).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body().getRealUser();
                    notify.setActor(user);

                    if (isRealtime) {
                        todayAdapter.addNotificationToTop(notify);
                        binding.rvNotificationsToday.smoothScrollToPosition(0);

                        // Có thông báo mới -> Ẩn text "Empty"
                        binding.tvEmptyToday.setVisibility(View.GONE);
                        binding.rvNotificationsToday.setVisibility(View.VISIBLE);
                    } else {
                        todayAdapter.notifyDataSetChanged();
                        earlierAdapter.notifyDataSetChanged();
                    }
                    saveToCache(); // Update cache có avatar
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Failed to fetch user info: " + t.getMessage());
            }
        });
    }

    private void processNotificationList(List<Notify> allNotifies) {
        List<Notify> todayList = new ArrayList<>();
        List<Notify> earlierList = new ArrayList<>();

        for (Notify notify : allNotifies) {
            if (isDateToday(notify.getCreatedAt())) {
                todayList.add(notify);
            } else {
                earlierList.add(notify);
            }
        }

        todayAdapter.setData(todayList);
        earlierAdapter.setData(earlierList);

        // --- XỬ LÝ UI: HÔM NAY ---
        if (todayList.isEmpty()) {
            binding.rvNotificationsToday.setVisibility(View.GONE);
            binding.tvEmptyToday.setVisibility(View.VISIBLE);
        } else {
            binding.rvNotificationsToday.setVisibility(View.VISIBLE);
            binding.tvEmptyToday.setVisibility(View.GONE);
        }
        binding.tvTodayLabel.setVisibility(View.VISIBLE);

        // --- XỬ LÝ UI: TRƯỚC ĐÓ ---
        if (earlierList.isEmpty()) {
            binding.rvNotificationsEarlier.setVisibility(View.GONE);
            binding.tvEmptyEarlier.setVisibility(View.VISIBLE);

            // Nếu không có tin cũ, ẩn nút xem thêm nếu chưa bấm
            if (binding.layoutEarlierNotifications.getVisibility() != View.VISIBLE) {
                binding.btnSeeEarlier.setVisibility(View.GONE);
            }
        } else {
            binding.rvNotificationsEarlier.setVisibility(View.VISIBLE);
            binding.tvEmptyEarlier.setVisibility(View.GONE);

            // Nếu có tin cũ và chưa mở layout -> hiện nút
            if (binding.layoutEarlierNotifications.getVisibility() != View.VISIBLE) {
                binding.btnSeeEarlier.setVisibility(View.VISIBLE);
            }
        }
    }

    private boolean isDateToday(Date date) {
        if (date == null) return false;
        return DateUtils.isToday(date.getTime());
    }

    // =================================================================
    // 🎯 LOGIC CLICK NAVIGATION (ĐÃ FIX LỖI)
    // =================================================================
    private void onNotificationClick(Notify noti) {
        String type = noti.getType();
        String targetId = noti.getTargetId(); // ID bài viết
        String actorId = noti.getActorId();   // ID người dùng

        Log.d(TAG, "👆 Clicked Notification - Type: " + type + ", TargetID: " + targetId);

        if (type == null) return;

        Intent intent = null;

        switch (type) {
            // 🟢 NHÓM 1: Bài viết -> Mở PostDetail
            case "like":
            case "comment":
            case "new_post":
            case "reply":
                // FIX LỖI: Kiểm tra null TRONG case này
                if (targetId != null && !targetId.isEmpty()) {
                    intent = new Intent(this, PostDetailActivity.class);
                    intent.putExtra("POST_ID", targetId);
                } else {
                    Log.e(TAG, "❌ ERROR: Target ID (Post ID) is NULL for type: " + type);
                    // Có thể Toast báo lỗi cho user biết
                }
                break;

            // 🔵 NHÓM 2: Follow -> Mở Profile
            case "follow":
                if (actorId != null && !actorId.isEmpty()) {
                    intent = new Intent(this, OtherUserProfileActivity.class);
                    intent.putExtra("USER_ID", actorId);
                } else {
                    Log.e(TAG, "❌ ERROR: Actor ID is NULL for type: follow");
                }
                break;

            default:
                Log.w(TAG, "⚠️ Unknown notification type: " + type);
                break;
        }

        if (intent != null) {
            startActivity(intent);
        }
    }
}