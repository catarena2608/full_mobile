package course.examples.nt118;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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

    private static final String TAG = "EditProfileActivity";

    // --- Views ---
    private ImageView avatarImageView, coverImageView, backButton;
    private EditText nameEditText, emailEditText, linkEditText;
    private Button saveButton, preferenceButton;

    // --- Data ---
    private String userId;
    private ApiService apiService;

    // Ảnh Avatar
    private Uri selectedAvatarUri = null;

    // Ảnh Bìa
    private Uri selectedCoverUri = null;

    // Cờ kiểm tra đang chọn ảnh nào (true = avatar, false = cover)
    private boolean isPickingAvatar = true;

    // ==================================================================
    // LAUNCHERS
    // ==================================================================

    // 1. Launcher chọn ảnh từ thư viện
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        if (isPickingAvatar) {
                            // Xử lý Avatar
                            selectedAvatarUri = uri;
                            // Hiển thị tròn
                            Glide.with(this).load(uri).circleCrop().into(avatarImageView);
                        } else {
                            // Xử lý Cover Photo
                            selectedCoverUri = uri;
                            // Hiển thị chữ nhật (centerCrop)
                            Glide.with(this).load(uri).centerCrop().into(coverImageView);
                        }
                    }
                }
            }
    );

    // 2. Launcher xin quyền
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) openGallery();
                else Toast.makeText(this, "Cần cấp quyền để chọn ảnh", Toast.LENGTH_SHORT).show();
            }
    );

    // ==================================================================
    // LIFECYCLE
    // ==================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Lấy UserID
        userId = TokenManager.getUserId(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        initViews();
        setupListeners();
        loadCurrentUserProfile();
    }

    private void initViews() {
        avatarImageView = findViewById(R.id.iv_avatar);
        coverImageView = findViewById(R.id.iv_cover_photo); // Ảnh bìa

        nameEditText = findViewById(R.id.et_name);
        emailEditText = findViewById(R.id.et_email);
        linkEditText = findViewById(R.id.et_link);

        saveButton = findViewById(R.id.btn_save);
        backButton = findViewById(R.id.btn_back);
        preferenceButton = findViewById(R.id.btn_preference);

        // Không cho sửa email
        if (emailEditText != null) emailEditText.setEnabled(false);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        // Click Avatar
        avatarImageView.setOnClickListener(v -> {
            isPickingAvatar = true;
            checkPermissionAndPickImage();
        });

        // Click Cover Photo
        coverImageView.setOnClickListener(v -> {
            isPickingAvatar = false;
            checkPermissionAndPickImage();
        });

        // Click Save -> Gọi hàm xử lý JSON
        saveButton.setOnClickListener(v -> handleSaveProfileJSON());

        preferenceButton.setOnClickListener(v -> {
            startActivity(new Intent(this, EditPreferenceActivity.class));
        });
    }

    // ==================================================================
    // LOGIC LOAD DATA
    // ==================================================================

    private void loadCurrentUserProfile() {
        apiService.getUserById(userId, userId).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    populateUserData(response.body());
                }
            }
            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.e(TAG, "Load Error", t);
            }
        });
    }

    private void populateUserData(UserResponse user) {
        nameEditText.setText(user.getName());
        if (emailEditText != null) emailEditText.setText(user.getEmail());

        if (user.getLink() != null && !user.getLink().isEmpty()) {
            linkEditText.setText(user.getLink().get(0));
        }

        // Load Avatar
        Glide.with(this)
                .load(user.getAvatar())
                .placeholder(R.drawable.ic_launcher_background)
                .circleCrop()
                .into(avatarImageView);

        // Load Cover Photo (Giả sử getter là getCover_photo)
        Glide.with(this)
                .load(user.getCoverImage())
                .placeholder(android.R.color.darker_gray)
                .centerCrop()
                .into(coverImageView);
    }

    // ==================================================================
    // LOGIC SAVE (JSON BODY) - QUAN TRỌNG
    // ==================================================================

    private void handleSaveProfileJSON() {
        String name = nameEditText.getText().toString().trim();
        String link = linkEditText.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        saveButton.setText("Đang lưu...");
        saveButton.setEnabled(false);

        // 1. Tạo Map chứa dữ liệu JSON
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("link", link);
        // Nếu backend yêu cầu link là mảng: body.put("link", Collections.singletonList(link));

        // 2. Chuyển đổi Avatar sang Base64 (Nếu có chọn mới)
        if (selectedAvatarUri != null) {
            String base64Avatar = encodeImageToBase64(selectedAvatarUri);
            if (base64Avatar != null) {
                body.put("avatar", base64Avatar); // Key phải khớp backend
            }
        }

        // 3. Chuyển đổi Cover Photo sang Base64 (Nếu có chọn mới)
        if (selectedCoverUri != null) {
            String base64Cover = encodeImageToBase64(selectedCoverUri);
            if (base64Cover != null) {
                body.put("cover_photo", base64Cover); // Key phải khớp backend
            }
        }

        // 4. Gọi API với Body Map
        apiService.editProfile(body).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                saveButton.setText("Save");
                saveButton.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(EditProfileActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(EditProfileActivity.this, "Lỗi Server: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                saveButton.setText("Save");
                saveButton.setEnabled(true);
                Log.e(TAG, "API Failure", t);
                Toast.makeText(EditProfileActivity.this, "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================================================================
    // UTILS: CONVERT IMAGE TO BASE64
    // ==================================================================

    private String encodeImageToBase64(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Resize ảnh nếu quá to để tránh lỗi OutOfMemory hoặc quá tải Payload
            // Ví dụ: Giới hạn kích thước khoảng 800x800 pixel
            bitmap = getResizedBitmap(bitmap, 800);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // Nén ảnh sang JPEG, chất lượng 80%
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            // Trả về chuỗi Base64
            // Dùng NO_WRAP để tránh xuống dòng trong chuỗi JSON
            return "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP);

        } catch (Exception e) {
            Log.e(TAG, "Lỗi convert ảnh sang Base64", e);
            return null;
        }
    }

    // Hàm phụ trợ để resize ảnh nhỏ lại
    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private void checkPermissionAndPickImage() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        requestPermissionLauncher.launch(permission);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }
}