package course.examples.nt118;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import course.examples.nt118.model.UserResponse;
import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import course.examples.nt118.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditPreferenceActivity extends AppCompatActivity {

    private static final String TAG = EditPreferenceActivity.class.getSimpleName();

    private ImageView btnBack, ivAvatar;
    private View btnSave;
    private String userId;

    private final List<CheckBox> allCheckBoxes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "1. onCreate: Khởi tạo màn hình Edit Preference");
        setContentView(R.layout.activity_edit_preference);

        userId = TokenManager.getUserId(this);
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "Lỗi session", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
        loadCurrentUserData();
    }

    @Override
    protected void onStart() { super.onStart(); Log.d(TAG, "2. onStart"); }

    @Override
    protected void onResume() { super.onResume(); Log.d(TAG, "3. onResume"); }

    @Override
    protected void onPause() { super.onPause(); Log.d(TAG, "4. onPause"); }

    @Override
    protected void onStop() { super.onStop(); Log.d(TAG, "5. onStop"); }

    @Override
    protected void onRestart() { super.onRestart(); Log.d(TAG, "6. onRestart"); }

    @Override
    protected void onDestroy() { super.onDestroy(); Log.d(TAG, "7. onDestroy"); }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        ivAvatar = findViewById(R.id.iv_pref_avatar);
        btnSave = findViewById(R.id.btn_save_pref);

        ViewGroup rootView = findViewById(android.R.id.content);
        findCheckBoxes(rootView);
    }

    private void findCheckBoxes(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof CheckBox) {
                allCheckBoxes.add((CheckBox) child);
            } else if (child instanceof ViewGroup) {
                findCheckBoxes((ViewGroup) child);
            }
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> savePreferences());
    }

    // ==================================================================
    // 3. DATA LOADING (ĐÃ SỬA ĐỂ KHỚP VỚI USER RESPONSE MỚI)
    // ==================================================================

    private void loadCurrentUserData() {
        Log.d(TAG, "API: Fetching current preferences...");
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        apiService.getUserById(userId, userId).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body();

                    // 1. Load Avatar
                    String avatarUrl = (user.getAvatar() != null && !user.getAvatar().isEmpty())
                            ? user.getAvatar()
                            : "https://i.pravatar.cc/150?u=" + userId;

                    Glide.with(EditPreferenceActivity.this)
                            .load(avatarUrl)
                            .placeholder(R.drawable.chef_hat)
                            .circleCrop()
                            .into(ivAvatar);

                    // 2. Load Checkboxes (SỬA LOGIC Ở ĐÂY)
                    // Vì preference bây giờ là Object, không phải String, ta cần lấy dữ liệu từ các list con
                    UserResponse.Preference prefs = user.getPreference();

                    if (prefs != null) {
                        List<String> activePreferences = new ArrayList<>();

                        // Gom tất cả sở thích từ các nhóm (allergy, illness, diet) vào 1 list chung
                        if (prefs.getAllergy() != null) activePreferences.addAll(prefs.getAllergy());
                        if (prefs.getIllness() != null) activePreferences.addAll(prefs.getIllness());
                        if (prefs.getDiet() != null) activePreferences.addAll(prefs.getDiet());

                        Log.d(TAG, "User Preferences found: " + activePreferences.toString());

                        // Duyệt qua tất cả Checkbox trên màn hình để tick vào cái nào có trong list
                        for (CheckBox cb : allCheckBoxes) {
                            String cbText = cb.getText().toString().trim();
                            if (activePreferences.contains(cbText)) {
                                cb.setChecked(true);
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.e(TAG, "Lỗi load data user", t);
            }
        });
    }

    // ==================================================================
    // 4. SAVE LOGIC
    // ==================================================================

    private void savePreferences() {
        // Gom text của các checkbox đang tick vào 1 list
        List<String> selectedItems = new ArrayList<>();
        for (CheckBox cb : allCheckBoxes) {
            if (cb.isChecked()) {
                selectedItems.add(cb.getText().toString());
            }
        }

        // TODO: Server hiện tại trả về Object {allergy, diet...}.
        // Nếu Backend yêu cầu gửi lên đúng cấu trúc đó, bạn cần sửa logic này để phân loại checkbox.
        // Hiện tại ta tạm gửi chuỗi String gộp, nếu Backend hỗ trợ nhận chuỗi thì sẽ OK.
        String prefResult = TextUtils.join(", ", selectedItems);
        Log.d(TAG, "Saving preferences: " + prefResult);

        ApiService api = RetrofitClient.getInstance(this).getApiService();
        Map<String, Object> body = new HashMap<>();
        body.put("preference", prefResult);

        api.editProfile(body).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditPreferenceActivity.this, "Đã lưu sở thích!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(EditPreferenceActivity.this, "Lỗi lưu: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Toast.makeText(EditPreferenceActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}