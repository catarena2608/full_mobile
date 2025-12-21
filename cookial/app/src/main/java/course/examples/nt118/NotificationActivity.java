package course.examples.nt118;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;

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
import course.examples.nt118.model.NotifyEvent; // Chỉ import cái này
import course.examples.nt118.network.SocketClient;
import course.examples.nt118.utils.TokenManager;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";
    private ActivityNotificationBinding binding;
    private final Gson gson = new Gson();

    private NotificationAdapter todayAdapter;
    private NotificationAdapter earlierAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupViews();
        setupRecyclerViews();
        connectSocketIfNeeded();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

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
            if (!token.isEmpty()) SocketClient.getInstance().connect(token);
        }
    }

    // =================================================================
    // 🔥 EVENT BUS SUBSCRIBER DUY NHẤT
    // =================================================================

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocketEvent(NotifyEvent event) {
        try {
            // TRƯỜNG HỢP 1: Nhận danh sách Init (JSONArray)
            if (event.isList()) {
                JSONArray data = event.getArrayData();
                Log.d(TAG, "📥 EventBus Init: " + data.length() + " items");

                Type listType = new TypeToken<List<Notify>>() {}.getType();
                List<Notify> allNotifies = gson.fromJson(data.toString(), listType);

                processNotificationList(allNotifies);
            }
            // TRƯỜNG HỢP 2: Nhận thông báo lẻ (JSONObject)
            else if (event.getJsonData() != null) {
                JSONObject data = event.getJsonData();
                Log.d(TAG, "🔔 EventBus Realtime: " + data.toString());

                Notify newNoti = gson.fromJson(data.toString(), Notify.class);
                if (newNoti != null) {
                    todayAdapter.addNotificationToTop(newNoti);
                    binding.rvNotificationsToday.smoothScrollToPosition(0);
                    binding.tvTodayLabel.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi xử lý EventBus Notify", e);
        }
    }

    // =================================================================
    // 🧠 LOGIC XỬ LÝ DỮ LIỆU
    // =================================================================

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

        binding.tvTodayLabel.setVisibility(todayList.isEmpty() ? View.GONE : View.VISIBLE);

        if (earlierList.isEmpty()) {
            binding.btnSeeEarlier.setVisibility(View.GONE);
            binding.layoutEarlierNotifications.setVisibility(View.GONE);
        } else {
            binding.btnSeeEarlier.setVisibility(View.VISIBLE);
            binding.layoutEarlierNotifications.setVisibility(View.GONE);
        }
    }

    private boolean isDateToday(Date date) {
        if (date == null) return false;
        return DateUtils.isToday(date.getTime());
    }

    private void onNotificationClick(Notify noti) {
        String targetId = noti.getTargetId();
        if (targetId != null) {
            Intent intent = new Intent(this, PostDetailActivity.class);
            intent.putExtra("POST_ID", targetId);
            startActivity(intent);
        }
    }
}