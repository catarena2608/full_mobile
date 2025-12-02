package course.examples.nt118;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    // Tự động lấy tên class làm TAG
    private static final String TAG = ResetPasswordActivity.class.getSimpleName();

    // UI Components
    private EditText newPasswordEditText, confirmPasswordEditText;
    private Button continueButton;

    // Data
    private String email;

    // =========================================================================
    // 1. LIFECYCLE LOGS
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "1. onCreate: Khởi tạo màn hình Reset Password");
        setContentView(R.layout.activity_reset_password);

        // Lấy email từ màn hình trước
        email = getIntent().getStringExtra("email");
        if (email == null) {
            Log.e(TAG, "Lỗi Critical: Không nhận được Email từ Intent");
            Toast.makeText(this, "Lỗi: Không xác định được tài khoản", Toast.LENGTH_LONG).show();
            finish();
            return;
        } else {
            Log.i(TAG, "Reset Password cho Email: " + email);
        }

        // Không cần RetrofitClient.init(this)

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
    protected void onRestart() { super.onRestart(); Log.d(TAG, "6. onRestart"); }

    @Override
    protected void onDestroy() { super.onDestroy(); Log.d(TAG, "7. onDestroy"); }

    // =========================================================================
    // 2. UI & LOGIC
    // =========================================================================

    private void initViews() {
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        continueButton = findViewById(R.id.continueButton);
    }

    private void setupListeners() {
        continueButton.setOnClickListener(v -> {
            Log.d(TAG, "User click: Continue (Đổi mật khẩu)");
            handleResetPassword();
        });
    }

    private void handleResetPassword() {
        String newPass = newPasswordEditText.getText().toString().trim();
        String confirmPass = confirmPasswordEditText.getText().toString().trim();

        // Validate
        if (!validateInput(newPass, confirmPass)) {
            Log.w(TAG, "Validate thất bại: Mật khẩu không hợp lệ hoặc không khớp");
            return;
        }

        // [SECURE LOG] Không log password ra console!
        Log.d(TAG, "Input hợp lệ. Đang gọi API Reset Password...");
        showLoading(true);

        // Gọi Singleton Instance
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", newPass);

        apiService.resetPassword(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                showLoading(false);
                if (response.isSuccessful()) {
                    Log.i(TAG, "API Reset Password thành công!");
                    navigateToLogin();
                } else {
                    Log.e(TAG, "API trả về lỗi. Code: " + response.code());
                    Toast.makeText(ResetPasswordActivity.this, "Đổi mật khẩu thất bại. Thử lại sau.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Lỗi kết nối API: " + t.getMessage(), t);
                Toast.makeText(ResetPasswordActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================================================================
    // 3. HELPER METHODS
    // =========================================================================

    private boolean validateInput(String newPass, String confirmPass) {
        if (newPass.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu mới", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (newPass.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (confirmPass.isEmpty()) {
            Toast.makeText(this, "Vui lòng xác nhận mật khẩu", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showLoading(boolean isLoading) {
        continueButton.setEnabled(!isLoading);
        continueButton.setText(isLoading ? "Đang xử lý..." : "Continue");
        newPasswordEditText.setEnabled(!isLoading);
        confirmPasswordEditText.setEnabled(!isLoading);
    }

    private void navigateToLogin() {
        Log.d(TAG, "Chuyển hướng về LoginActivity");
        Toast.makeText(ResetPasswordActivity.this, "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
        // Clear Task để user không back lại được màn hình này
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}