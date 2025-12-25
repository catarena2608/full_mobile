package course.examples.nt118;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;

import org.json.JSONObject; // [MỚI] Import để parse JSON lỗi

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import course.examples.nt118.databinding.ActivityNewRecipePostBinding;
import course.examples.nt118.model.UploadPostResponse;
import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import course.examples.nt118.utils.TokenManager;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewRecipePostActivity extends AppCompatActivity {

    private static final String TAG = "NewRecipePost";
    private static final int PERMISSION_REQ_CODE = 101;

    private ActivityNewRecipePostBinding binding;
    private ProgressDialog progressDialog;

    private String currentUserID;
    private Uri thumbnailUri = null;

    private final Map<String, List<IngredientInputHolder>> ingredientMap = new HashMap<>();
    private final List<EditText> guideStepFields = new ArrayList<>();

    private static class IngredientInputHolder {
        EditText edName, edQuantity;
        public IngredientInputHolder(EditText name, EditText quantity) {
            this.edName = name;
            this.edQuantity = quantity;
        }
    }

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleImageSelection(result.getData().getData());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewRecipePostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!checkUserSession()) return;

        initUI();
        initDynamicDataContainers();
        setupListeners();
    }

    private boolean checkUserSession() {
        currentUserID = TokenManager.getUserId(this);
        if (TextUtils.isEmpty(currentUserID)) {
            Toast.makeText(this, "Phiên đăng nhập hết hạn!", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }
        return true;
    }

    private void initUI() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
    }

    private void initDynamicDataContainers() {
        ingredientMap.put("base", new ArrayList<>());
        ingredientMap.put("comple", new ArrayList<>());
        ingredientMap.put("spice", new ArrayList<>());
        ingredientMap.put("other", new ArrayList<>());
        addIngredientRow("base");
        addGuideStepRow(1);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSaveDraft.setOnClickListener(v -> Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show());
        binding.txtThumbnailUploadArea.setOnClickListener(v -> checkPermissionAndPickImage());
        binding.txtAddIngredient.setOnClickListener(v -> addIngredientRow("base"));
        binding.btnAddStep.setOnClickListener(v -> addGuideStepRow(guideStepFields.size() + 1));

        // Nút đăng chính
        binding.btnPost.setOnClickListener(v -> attemptUploadFlow());
    }

    // ==================================================================
    // 3. XỬ LÝ GIAO DIỆN ĐỘNG
    // ==================================================================

    private void addIngredientRow(String groupKey) {
        LinearLayout container = binding.layoutIngredientsListContainer;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setWeightSum(10);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 16, 0, 0);
        row.setLayoutParams(rowParams);

        EditText edName = createStyledEditText("Tên nguyên liệu", 7);
        EditText edQty = createStyledEditText("SL (g/ml)", 3);
        ((LinearLayout.LayoutParams) edQty.getLayoutParams()).setMargins(16, 0, 0, 0);

        row.addView(edName);
        row.addView(edQty);
        container.addView(row);

        if (ingredientMap.containsKey(groupKey)) {
            ingredientMap.get(groupKey).add(new IngredientInputHolder(edName, edQty));
        }
    }

    private void addGuideStepRow(int stepNumber) {
        EditText etStep = createStyledEditText("Bước " + stepNumber + ": Nhập hướng dẫn...", 0);
        etStep.setMinLines(2);
        etStep.setGravity(Gravity.TOP | Gravity.START);
        ((LinearLayout.LayoutParams) etStep.getLayoutParams()).setMargins(0, 24, 0, 0);

        guideStepFields.add(etStep);
        binding.layoutGuideStepsContainer.addView(etStep);
    }

    private EditText createStyledEditText(String hint, float weight) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setBackgroundResource(android.R.drawable.edit_text);
        et.setPadding(24, 24, 24, 24);
        et.setTextSize(14);
        LinearLayout.LayoutParams params;
        if (weight > 0) {
            params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
        } else {
            params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }
        et.setLayoutParams(params);
        return et;
    }

    // ==================================================================
    // 4. LUỒNG XỬ LÝ API (QUAN TRỌNG - ĐÃ CẬP NHẬT 403)
    // ==================================================================

    private void attemptUploadFlow() {
        // Validation
        if (TextUtils.isEmpty(binding.editRecipeName.getText())) {
            binding.editRecipeName.setError("Nhập tên món ăn!");
            return;
        }
        if (thumbnailUri == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh đại diện!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Bước 1: Đang tạo bài viết...");
        progressDialog.show();

        performStep1_CreatePost();
    }

    // BƯỚC 1: Gọi API upload bài viết (Common Post)
    private void performStep1_CreatePost() {
        List<MultipartBody.Part> files = prepareImagePart();
        RequestBody api_type = toRequestBody("Recipe");
        RequestBody api_caption = toRequestBody(binding.editDescription.getText().toString());
        RequestBody api_tag = toRequestBody("[]");
        RequestBody api_location = toRequestBody("");

        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        apiService.uploadPost(files, api_type, api_caption, api_tag, api_location)
                .enqueue(new Callback<UploadPostResponse>() {
                    @Override
                    public void onResponse(Call<UploadPostResponse> call, Response<UploadPostResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            // Lấy ID từ trường 'post' trong JSON trả về từ Node.js
                            String postId = null;
                            if (response.body().getPost() != null) {
                                postId = response.body().getPost().get_id(); // Trùng với _id bên Node
                            }

                            if (!TextUtils.isEmpty(postId)) {
                                Log.d(TAG, "Step 1 OK. PostID: " + postId);
                                progressDialog.setMessage("Bước 2: Đang lưu nội dung công thức...");
                                performStep2_CreateRecipe(postId);
                            } else {
                                progressDialog.dismiss();
                                handleError("Lỗi: Server không trả về Post ID.");
                            }

                        } else if (response.code() == 403) {
                            // [MỚI] Xử lý lỗi 403 ở bước 1
                            progressDialog.dismiss();
                            handle403Error(response);

                        } else {
                            progressDialog.dismiss();
                            handleError("Lỗi tạo Post: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<UploadPostResponse> call, Throwable t) {
                        progressDialog.dismiss();
                        handleError("Lỗi kết nối bước 1: " + t.getMessage());
                    }
                });
    }

    // BƯỚC 2: Gọi API lưu chi tiết công thức (Recipe Detail) gắn với PostID vừa tạo
    private void performStep2_CreateRecipe(String postId) {
        RequestBody api_postID = toRequestBody(postId);
        RequestBody api_name = toRequestBody(binding.editRecipeName.getText().toString());
        RequestBody api_description = toRequestBody(binding.editDescription.getText().toString());
        RequestBody api_ration = toRequestBody(binding.editRation.getText().toString());
        RequestBody api_time = toRequestBody(binding.editTime.getText().toString());
        RequestBody api_ingredients = toRequestBody(generateIngredientsJson());
        RequestBody api_guide = toRequestBody(generateGuideJson());
        RequestBody api_tags = toRequestBody("[]");

        // Gửi kèm ảnh thumbnail lần nữa nếu Backend API Recipe yêu cầu media
        List<MultipartBody.Part> media = prepareImagePart();

        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        apiService.uploadRecipe(
                toRequestBody(""), api_postID, api_name, api_description,
                api_ration, api_time, api_ingredients, api_guide, api_tags, media
        ).enqueue(new Callback<UploadPostResponse>() {
            @Override
            public void onResponse(Call<UploadPostResponse> call, Response<UploadPostResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(NewRecipePostActivity.this, "Đăng công thức thành công 🎉", Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();

                } else if (response.code() == 403) {
                    // [MỚI] Xử lý lỗi 403 ở bước 2
                    handle403Error(response);

                } else {
                    handleError("Lỗi lưu công thức: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<UploadPostResponse> call, Throwable t) {
                progressDialog.dismiss();
                handleError("Lỗi kết nối bước 2: " + t.getMessage());
            }
        });
    }

    // ==================================================================
    // 5. HELPERS & UTILS
    // ==================================================================

    // [MỚI] Hàm xử lý chung cho lỗi 403 để code gọn hơn
    private void handle403Error(Response<UploadPostResponse> response) {
        try {
            String errorBody = response.errorBody().string();
            JSONObject jsonObject = new JSONObject(errorBody);
            String message = jsonObject.optString("message", "Bạn đang bị cấm đăng bài");
            Toast.makeText(NewRecipePostActivity.this, message, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            handleError("Bạn không có quyền đăng bài");
        }
    }

    private String generateIngredientsJson() {
        Map<String, List<Map<String, String>>> root = new HashMap<>();
        for (Map.Entry<String, List<IngredientInputHolder>> entry : ingredientMap.entrySet()) {
            List<Map<String, String>> itemList = new ArrayList<>();
            for (IngredientInputHolder holder : entry.getValue()) {
                String name = holder.edName.getText().toString().trim();
                String qty = holder.edQuantity.getText().toString().trim();
                if (!name.isEmpty()) {
                    Map<String, String> item = new HashMap<>();
                    item.put("name", name);
                    item.put("quantity", qty);
                    itemList.add(item);
                }
            }
            if (!itemList.isEmpty()) root.put(entry.getKey(), itemList);
        }
        return new Gson().toJson(root);
    }

    private String generateGuideJson() {
        List<Map<String, Object>> steps = new ArrayList<>();
        for (int i = 0; i < guideStepFields.size(); i++) {
            String content = guideStepFields.get(i).getText().toString().trim();
            if (!content.isEmpty()) {
                Map<String, Object> step = new HashMap<>();
                step.put("step", i + 1);
                step.put("content", content);
                step.put("media", new ArrayList<>());
                steps.add(step);
            }
        }
        return new Gson().toJson(steps);
    }

    private RequestBody toRequestBody(String value) {
        return RequestBody.create(MediaType.parse("text/plain"), value == null ? "" : value);
    }

    private List<MultipartBody.Part> prepareImagePart() {
        List<MultipartBody.Part> parts = new ArrayList<>();
        if (thumbnailUri != null) {
            File file = uriToFile(thumbnailUri);
            if (file != null) {
                RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), file);
                parts.add(MultipartBody.Part.createFormData("media", file.getName(), reqFile));
            }
        }
        return parts;
    }

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

    private void handleImageSelection(Uri uri) {
        this.thumbnailUri = uri;
        String fileName = getFileName(uri);
        binding.txtThumbnailUploadArea.setText(fileName != null ? "Đã chọn: " + fileName : "Đã chọn ảnh");
    }

    private File uriToFile(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            File tempFile = new File(getCacheDir(), "upload_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) > 0) outputStream.write(buffer, 0, len);
            }
            return tempFile;
        } catch (IOException e) { return null; }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) result = cursor.getString(index);
                }
            }
        }
        return result;
    }

    private void handleError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        Log.e(TAG, msg);
    }
}