package course.examples.nt118;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import java.util.HashMap;
import java.util.Map;

import course.examples.nt118.model.UserResponse;
import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import course.examples.nt118.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = EditProfileActivity.class.getSimpleName();
    private static final int PERMISSION_REQ_CODE = 200;

    private ImageView avatarImageView, backButton;
    private EditText nameEditText, emailEditText, linkEditText, usernameEditText;
    private Button saveButton, preferenceButton;

    private String userId;
    private Uri selectedAvatarUri = null;

    // Launcher chọn ảnh từ Gallery
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedAvatarUri = result.getData().getData();
                    if (selectedAvatarUri != null) {
                        Log.d(TAG, "User picked image: " + selectedAvatarUri.toString());
                        Glide.with(this)
                                .load(selectedAvatarUri)
                                .circleCrop()
                                .into(avatarImageView);
                    }
                }
            }
    );

    // ==================================================================
    // 1. LIFECYCLE LOGS
    // ==================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "1. onCreate: Khởi tạo EditProfileActivity");
        setContentView(R.layout.activity_edit_profile);

        // Lấy UserID từ Token
        userId = TokenManager.getUserId(this);
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "Lỗi session", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();

        // Load thông tin hiện tại để hiển thị lên EditText
        loadCurrentUserProfile();
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

    // ==================================================================
    // 2. INIT & LISTENERS
    // ==================================================================

    private void initViews() {
        avatarImageView = findViewById(R.id.iv_avatar);
        backButton = findViewById(R.id.btn_back);
        nameEditText = findViewById(R.id.et_name);
        emailEditText = findViewById(R.id.et_email);
        linkEditText = findViewById(R.id.et_link); // Link profile (Facebook/Insta...)
        saveButton = findViewById(R.id.btn_save);
        preferenceButton = findViewById(R.id.btn_preference);

        // Email thường không cho sửa
        if (emailEditText != null) emailEditText.setEnabled(false);
        // Username cũng thường là duy nhất
        if (usernameEditText != null) usernameEditText.setEnabled(false);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> {
            Log.d(TAG, "User click: Back");
            finish();
        });

        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "User click: Save Profile");
            updateUserProfile();
        });

        avatarImageView.setOnClickListener(v -> checkPermissionAndPickImage());

        preferenceButton.setOnClickListener(v -> {
            Log.d(TAG, "User click: Edit Preferences");
            Intent intent = new Intent(EditProfileActivity.this, EditPreferenceActivity.class);
            // Có thể truyền UserID nếu cần, nhưng bên kia tự lấy từ Token cũng được
            startActivity(intent);
        });
    }

    // ==================================================================
    // 3. LOAD DATA (GET)
    // ==================================================================

    private void loadCurrentUserProfile() {
        Log.d(TAG, "API: Loading user profile...");
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        apiService.getUserById(userId, userId).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body();

                    // Fill data vào EditText
                    nameEditText.setText(user.getName());

                    if (emailEditText != null) emailEditText.setText(user.getEmail());

                    // Giả sử link là mảng string, lấy phần tử đầu tiên
                    if (user.getLink() != null && !user.getLink().isEmpty()) {
                        linkEditText.setText(user.getLink().get(0));
                    }

                    // Load avatar
                    String avatarUrl = (user.getAvatar() != null && !user.getAvatar().isEmpty())
                            ? user.getAvatar()
                            : "https://i.pravatar.cc/150?u=" + userId;

                    Glide.with(EditProfileActivity.this)
                            .load(avatarUrl)
                            .circleCrop()
                            .into(avatarImageView);

                    Log.i(TAG, "Data loaded for: " + user.getName());
                } else {
                    Log.e(TAG, "Load failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.e(TAG, "Network Error loading profile", t);
            }
        });
    }

    // ==================================================================
    // 4. UPDATE DATA (PATCH)
    // ==================================================================

    private void updateUserProfile() {
        String newName = nameEditText.getText().toString().trim();
        String newLink = linkEditText.getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("name", newName);
        // Backend có thể yêu cầu link là Array hoặc String, tùy chỉnh ở đây:
        // Nếu backend nhận mảng: body.put("link", Collections.singletonList(newLink));
        body.put("link", newLink);

        // NOTE: Hiện tại API editProfile nhận Map<String, Object> (JSON Body)
        // Nếu muốn upload ảnh (Multipart), cần một endpoint khác hoặc logic Base64.
        // Ở đây mình chỉ update Text info.

        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        apiService.editProfile(body).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "Update Success");
                    Toast.makeText(EditProfileActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();

                    // Báo hiệu cho màn hình Profile biết data đã thay đổi để reload
                    Intent intent = new Intent();
                    intent.putExtra("UPDATED", true);
                    setResult(RESULT_OK, intent);

                    finish();
                } else {
                    Log.e(TAG, "Update Failed: " + response.code());
                    Toast.makeText(EditProfileActivity.this, "Lỗi cập nhật: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.e(TAG, "Network Error Update", t);
                Toast.makeText(EditProfileActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================================================================
    // 5. IMAGE PICKER LOGIC
    // ==================================================================

    private void checkPermissionAndPickImage() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQ_CODE);
        } else {
            openGallery();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageLauncher.launch(Intent.createChooser(intent, "Chọn ảnh đại diện"));
    }

    // Xử lý kết quả xin quyền (Optional nhưng tốt cho Clean Code)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Cần cấp quyền để chọn ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }
}