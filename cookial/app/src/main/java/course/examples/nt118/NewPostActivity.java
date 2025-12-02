package course.examples.nt118;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView; // Import ImageView
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import course.examples.nt118.model.UploadPostResponse;
import course.examples.nt118.network.RetrofitClient;
import course.examples.nt118.utils.TokenManager;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewPostActivity extends AppCompatActivity {

    private static final String TAG = NewPostActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSION_CODE = 100;

    // --- UI Components ---
    private ImageView btnBack; // [SỬA] Đổi thành ImageView
    private Button btnPost;
    private Button btnTypeMoment, btnTypeRecipe;
    private TextView txtMediaUploadArea, txtCharCount;
    private EditText editDescription, editLocation;
    private ProgressDialog progressDialog;

    // --- Data Variables ---
    private String selectedType = "Moment";
    private final List<Uri> selectedImageUris = new ArrayList<>();
    private String currentUserID;

    // --- Image Picker Launcher ---
    private final ActivityResultLauncher<Intent> pickImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleImageSelection(result.getData());
                }
            }
    );

    // ==================================================================
    // 1. LIFECYCLE LOGS
    // ==================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "1. onCreate: Khởi tạo NewPostActivity");
        setContentView(R.layout.activity_new_post);

        // Check Session
        currentUserID = TokenManager.getUserId(this);
        if (TextUtils.isEmpty(currentUserID)) {
            Toast.makeText(this, "Phiên đăng nhập hết hạn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
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
    protected void onDestroy() { super.onDestroy(); Log.d(TAG, "7. onDestroy"); }

    // ==================================================================
    // 2. INIT UI & LISTENERS
    // ==================================================================

    private void initViews() {
        // [SỬA] Ánh xạ btnBack
        btnBack = findViewById(R.id.btn_back);

        btnPost = findViewById(R.id.btn_post);
        btnTypeMoment = findViewById(R.id.btn_type_moment);
        btnTypeRecipe = findViewById(R.id.btn_type_recipe);

        txtMediaUploadArea = findViewById(R.id.txt_media_upload_area);
        editDescription = findViewById(R.id.edit_description);
        txtCharCount = findViewById(R.id.txt_char_count);
        editLocation = findViewById(R.id.edit_location);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang đăng bài...");
        progressDialog.setCancelable(false);

        updateTypeButtonsUI(); // Mặc định là Moment
    }

    private void setupListeners() {
        // Back -> Finish
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Log.d(TAG, "User click: Cancel Post");
                finish();
            });
        }

        // Switch to Recipe Mode -> Open NewRecipePostActivity
        if (btnTypeRecipe != null) {
            btnTypeRecipe.setOnClickListener(v -> {
                Log.d(TAG, "Switch to Recipe Mode");
                startActivity(new Intent(this, NewRecipePostActivity.class));
                finish(); // Đóng màn hình hiện tại để chuyển sang màn hình Công thức
            });
        }

        // Stay on Moment Mode
        if (btnTypeMoment != null) {
            btnTypeMoment.setOnClickListener(v -> {
                if (!selectedType.equals("Moment")) {
                    selectedType = "Moment";
                    updateTypeButtonsUI();
                }
            });
        }

        // Count chars
        editDescription.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                txtCharCount.setText(s.length() + "/500");
            }
        });

        // Pick Image
        txtMediaUploadArea.setOnClickListener(v -> {
            Log.d(TAG, "User click: Pick Image");
            checkPermissionAndPickImage();
        });

        // Upload Post
        if (btnPost != null) {
            btnPost.setOnClickListener(v -> {
                Log.d(TAG, "User click: Post");
                performUpload();
            });
        }
    }

    // --- UI Logic ---
    private void updateTypeButtonsUI() {
        boolean isMoment = "Moment".equals(selectedType);
        setButtonState(btnTypeMoment, isMoment);
        setButtonState(btnTypeRecipe, !isMoment);
    }

    private void setButtonState(Button btn, boolean isActive) {
        if (btn == null) return;
        // Màu cam cho active, xám cho inactive
        int bg = isActive ? 0xFFFF9800 : 0xFFE0E0E0;
        int txt = isActive ? 0xFFFFFFFF : 0xFF000000;
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bg));
        btn.setTextColor(txt);
    }

    private void handleImageSelection(Intent data) {
        selectedImageUris.clear();
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                selectedImageUris.add(data.getClipData().getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            selectedImageUris.add(data.getData());
        }

        if (selectedImageUris.isEmpty()) {
            txtMediaUploadArea.setText("Bấm để thêm ảnh/video");
            txtMediaUploadArea.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_photo_camera, 0, 0);
        } else {
            Log.d(TAG, "Selected " + selectedImageUris.size() + " images.");
            txtMediaUploadArea.setText("Đã chọn " + selectedImageUris.size() + " ảnh");
            txtMediaUploadArea.setCompoundDrawablesWithIntrinsicBounds(0, android.R.drawable.checkbox_on_background, 0, 0);
        }
    }

    // ==================================================================
    // 3. UPLOAD LOGIC
    // ==================================================================

    private void performUpload() {
        String caption = editDescription.getText().toString().trim();
        String locationName = editLocation.getText().toString().trim();

        // 1. Validate Input
        if (caption.isEmpty()) {
            editDescription.setError("Nội dung không được để trống");
            editDescription.requestFocus();
            return;
        }

        if (selectedImageUris.isEmpty()) {
            editDescription.setError("Vui lòng chọn ít nhất 1 ảnh");
            editDescription.requestFocus();
            return;
        }

        progressDialog.show();

        // 2. Prepare Data (Multipart)
        RequestBody rType = createTextPart(selectedType); // "Moment"
        RequestBody rCaption = createTextPart(caption);
        RequestBody rTag = createTextPart(new Gson().toJson(new ArrayList<>())); // Empty tags JSON

        // Location JSON
        Map<String, Object> locMap = new HashMap<>();
        locMap.put("name", locationName.isEmpty() ? "Check-in" : locationName);
        locMap.put("type", "Point");
        locMap.put("coordinates", new double[]{106.7, 10.8}); // Fake GPS Coordinates
        RequestBody rLocation = createTextPart(new Gson().toJson(locMap));

        // Images
        List<MultipartBody.Part> listImageParts = prepareImageParts();

        Log.d(TAG, "Uploading post with " + listImageParts.size() + " images.");

        // 3. Call API (Dùng Application Context)
        RetrofitClient.getInstance(getApplicationContext()).getApiService()
                .uploadPost(listImageParts, rType, rCaption, rTag, rLocation)
                .enqueue(new Callback<UploadPostResponse>() {
                    @Override
                    public void onResponse(Call<UploadPostResponse> call, Response<UploadPostResponse> response) {
                        progressDialog.dismiss();
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Log.i(TAG, "Upload Success!");
                            Toast.makeText(NewPostActivity.this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Log.e(TAG, "Upload Failed. Code: " + response.code());
                            String errorMsg = (response.body() != null) ? response.body().getMessage() : "Lỗi server";
                            Toast.makeText(NewPostActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<UploadPostResponse> call, Throwable t) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Network Error Upload", t);
                        Toast.makeText(NewPostActivity.this, "Lỗi kết nối!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ==================================================================
    // 4. HELPER METHODS
    // ==================================================================

    private RequestBody createTextPart(String value) {
        return RequestBody.create(MediaType.parse("text/plain"), value);
    }

    private List<MultipartBody.Part> prepareImageParts() {
        List<MultipartBody.Part> parts = new ArrayList<>();
        for (Uri uri : selectedImageUris) {
            File file = uriToFile(uri);
            if (file != null) {
                // "image/*" để server biết đây là file ảnh
                RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), file);

                // Key "media" phải khớp với cấu hình Multer trên Backend
                MultipartBody.Part part = MultipartBody.Part.createFormData("media", file.getName(), reqFile);
                parts.add(part);
            }
        }
        return parts;
    }

    private void checkPermissionAndPickImage() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_PERMISSION_CODE);
        } else {
            openGallery();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickImagesLauncher.launch(Intent.createChooser(intent, "Chọn ảnh"));
    }

    private File uriToFile(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            File tempFile = new File(getCacheDir(), "upload_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
            return tempFile;
        } catch (IOException e) {
            Log.e(TAG, "File convert error", e);
            return null;
        }
    }
}