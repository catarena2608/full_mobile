package course.examples.nt118;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import course.examples.nt118.model.UserResponse;
import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import course.examples.nt118.utils.TokenManager;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

    // --- Views ---
    private ImageView avatarImageView, backButton;
    private EditText nameEditText, emailEditText, linkEditText, usernameEditText;
    private Button saveButton, preferenceButton;
    // (Optional) Thêm ProgressBar vào layout XML của bạn để UX tốt hơn
    // private ProgressBar loadingProgressBar;

    // --- Data ---
    private String userId;
    private Uri selectedAvatarUri = null;
    private File selectedAvatarFile = null; // File thực tế để upload
    private ApiService apiService;

    // --- Launchers ---
    // 1. Launcher chọn ảnh
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedAvatarUri = result.getData().getData();
                    if (selectedAvatarUri != null) {
                        displayAvatar(selectedAvatarUri);
                        // Chuyển Uri thành File để sẵn sàng upload
                        selectedAvatarFile = getFileFromUri(selectedAvatarUri);
                    }
                }
            }
    );

    // 2. Launcher xin quyền (Android 13+ vs Old Android)
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openGallery();
                } else {
                    Toast.makeText(this, "Bạn cần cấp quyền để đổi ảnh đại diện", Toast.LENGTH_SHORT).show();
                }
            }
    );

    // ==================================================================
    // 1. LIFECYCLE
    // ==================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Kiểm tra Token/User hợp lệ
        if (!validateUserSession()) return;

        // Khởi tạo API
        apiService = RetrofitClient.getInstance(this).getApiService();

        initViews();
        setupListeners();
        loadCurrentUserProfile();
    }

    // ==================================================================
    // 2. INITIALIZATION & SETUP
    // ==================================================================

    private boolean validateUserSession() {
        userId = TokenManager.getUserId(this);
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "Phiên đăng nhập hết hạn", Toast.LENGTH_SHORT).show();
            // Điều hướng về Login nếu cần
            finish();
            return false;
        }
        return true;
    }

    private void initViews() {
        avatarImageView = findViewById(R.id.iv_avatar);
        backButton = findViewById(R.id.btn_back);
        nameEditText = findViewById(R.id.et_name);
        emailEditText = findViewById(R.id.et_email);
        linkEditText = findViewById(R.id.et_link);
        saveButton = findViewById(R.id.btn_save);
        preferenceButton = findViewById(R.id.btn_preference);

        // Disable các trường không cho sửa
        if (emailEditText != null) emailEditText.setEnabled(false);
        if (usernameEditText != null) usernameEditText.setEnabled(false);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        preferenceButton.setOnClickListener(v -> {
            Intent intent = new Intent(EditProfileActivity.this, EditPreferenceActivity.class);
            startActivity(intent);
        });

        avatarImageView.setOnClickListener(v -> checkPermissionAndPickImage());

        saveButton.setOnClickListener(v -> handleSaveProfile());
    }

    // ==================================================================
    // 3. LOGIC: LOAD DATA
    // ==================================================================

    private void loadCurrentUserProfile() {
        setLoadingState(true);
        apiService.getUserById(userId, userId).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                setLoadingState(false);
                if (response.isSuccessful() && response.body() != null) {
                    populateUserData(response.body());
                } else {
                    showError("Không thể tải thông tin: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                setLoadingState(false);
                Log.e(TAG, "Load Profile Error", t);
                showError("Lỗi kết nối server");
            }
        });
    }

    private void populateUserData(UserResponse user) {
        nameEditText.setText(user.getName());
        if (emailEditText != null) emailEditText.setText(user.getEmail());

        // Xử lý link (nếu là List thì lấy phần tử đầu, nếu String thì lấy trực tiếp)
        if (user.getLink() != null && !user.getLink().isEmpty()) {
            linkEditText.setText(user.getLink().get(0));
        }

        // Hiển thị Avatar từ URL
        String avatarUrl = (user.getAvatar() != null && !user.getAvatar().isEmpty())
                ? user.getAvatar()
                : "https://ui-avatars.com/api/?name=" + user.getName(); // Fallback image thông minh hơn

        Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_launcher_background) // Nên có placeholder
                .circleCrop()
                .into(avatarImageView);
    }

    // ==================================================================
    // 4. LOGIC: UPDATE PROFILE
    // ==================================================================

    private void handleSaveProfile() {
        String newName = nameEditText.getText().toString().trim();
        String newLink = linkEditText.getText().toString().trim();

        if (newName.isEmpty()) {
            showError("Tên hiển thị không được để trống");
            return;
        }

        // TODO: Logic phân chia
        // Trường hợp 1: Backend chỉ nhận JSON (Code hiện tại)
        updateProfileJson(newName, newLink);

        // Trường hợp 2: Backend nhận Multipart (Có upload ảnh)
        // Nếu selectedAvatarFile != null -> gọi hàm uploadMultipart(newName, newLink, selectedAvatarFile);
    }

    private void updateProfileJson(String name, String link) {
        setLoadingState(true);

        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("link", link);
        // Lưu ý: Nếu backend yêu cầu link là mảng, hãy dùng: Collections.singletonList(link)

        apiService.editProfile(body).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                setLoadingState(false);
                if (response.isSuccessful()) {
                    Toast.makeText(EditProfileActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();

                    // Trả kết quả về để màn hình trước reload
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("UPDATED", true);
                    setResult(RESULT_OK, resultIntent);

                    finish();
                } else {
                    showError("Cập nhật thất bại: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                setLoadingState(false);
                Log.e(TAG, "Update Error", t);
                showError("Lỗi kết nối khi cập nhật");
            }
        });
    }

    /* // --- SAMPLE MULTIPART CODE (Dùng khi backend hỗ trợ upload ảnh) ---
    private void uploadMultipart(String name, String link, File file) {
        RequestBody namePart = RequestBody.create(MediaType.parse("text/plain"), name);
        RequestBody linkPart = RequestBody.create(MediaType.parse("text/plain"), link);

        MultipartBody.Part avatarPart = null;
        if (file != null) {
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            avatarPart = MultipartBody.Part.createFormData("avatar", file.getName(), requestFile);
        }

        // Cần thêm method editProfileMultipart trong ApiService
        apiService.editProfileMultipart(namePart, linkPart, avatarPart)...
    }
    */

    // ==================================================================
    // 5. IMAGE & PERMISSION UTILS
    // ==================================================================

    private void checkPermissionAndPickImage() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        requestPermissionLauncher.launch(permission);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void displayAvatar(Uri uri) {
        Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(avatarImageView);
    }

    // Helper: Chuyển Uri thành File (Quan trọng cho Android 10+)
    private File getFileFromUri(Uri uri) {
        try {
            // Tạo file tạm trong cache của app
            File tempFile = File.createTempFile("avatar_upload", ".jpg", getCacheDir());
            tempFile.deleteOnExit();

            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 OutputStream outputStream = new FileOutputStream(tempFile)) {
                if (inputStream == null) return null;

                byte[] buffer = new byte[4 * 1024]; // 4KB buffer
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
            }
            return tempFile;
        } catch (IOException e) {
            Log.e(TAG, "Lỗi tạo file tạm", e);
            return null;
        }
    }

    // ==================================================================
    // 6. UI HELPERS
    // ==================================================================

    private void setLoadingState(boolean isLoading) {
        saveButton.setEnabled(!isLoading);
        saveButton.setText(isLoading ? "Đang lưu..." : "Lưu thay đổi");
        // Nếu có ProgressBar: loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}