package course.examples.nt118;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

import course.examples.nt118.model.LoginResponse;
import course.examples.nt118.model.UserResponse;
import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import course.examples.nt118.utils.TokenManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    // Tự động lấy tên class làm TAG
    private static final String TAG = RegisterActivity.class.getSimpleName();

    // UI Components
    private EditText nameEditText,usernameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private TextView signInTextView;
    private LinearLayout btnGoogle, btnFacebook;

    // =========================================================================
    // 1. LIFECYCLE LOGS (Để theo dõi vòng đời Activity)
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "1. onCreate: Khởi tạo màn hình Register");
        setContentView(R.layout.activity_register);

        // Không cần RetrofitClient.init(this) nữa

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
    // 2. UI & LISTENERS
    // =========================================================================

    private void initViews() {
        nameEditText = findViewById(R.id.nameEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.signUpButton);
        signInTextView = findViewById(R.id.signInTextView);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);
    }

    private void setupListeners() {
        registerButton.setOnClickListener(v -> handleRegister());

        signInTextView.setOnClickListener(v -> navigateToLogin());

        View.OnClickListener socialListener = v -> {
            Log.d(TAG, "User click: Social Login (Feature pending)");
            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
        };

        btnGoogle.setOnClickListener(socialListener);
        btnFacebook.setOnClickListener(socialListener);
    }

    // =========================================================================
    // 3. LOGIC REGISTER (Đăng ký)
    // =========================================================================

    private void handleRegister() {
        String name = nameEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // Validate Input
        if (!validateInput(name, email, password, confirmPassword)) {
            Log.w(TAG, "Validate thất bại: Input không hợp lệ");
            return;
        }

        Log.d(TAG, "Bắt đầu gọi API Register với Email: " + email);
        showLoading(true);

        // [CẤU TRÚC MỚI] Gọi Singleton Instance
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("password", password);
        // Random avatar demo
        body.put("avatar", "https://i.pravatar.cc/150?img=" + (System.currentTimeMillis() % 10));

        apiService.registerUser(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "API Register thành công. Đang tiến hành Auto Login...");
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();

                    // Gọi hàm đăng nhập tự động để lấy Cookie và UserID
                    performAutoLogin(email, password);
                } else {
                    showLoading(false);
                    Log.e(TAG, "API Register thất bại. Code: " + response.code());
                    handleRegisterError(response);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Lỗi kết nối Register: " + t.getMessage(), t);
                Toast.makeText(RegisterActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================================================================
    // 4. LOGIC AUTO LOGIN (Tự động đăng nhập sau khi đăng ký)
    // =========================================================================

    private void performAutoLogin(String email, String password) {
        Log.d(TAG, "Bắt đầu Auto Login...");
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        Map<String, String> loginBody = new HashMap<>();
        loginBody.put("email", email);
        loginBody.put("password", password);

        apiService.loginUser(loginBody).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body().getUser();
                    if (user != null) {
                        Log.i(TAG, "Auto Login thành công. UserID: " + user.getId());
                        TokenManager.saveUserId(RegisterActivity.this, user.getId());
                        navigateToHome(user.getId());
                    } else {
                        Log.e(TAG, "Auto Login: User object null");
                        navigateToLogin();
                    }
                } else {
                    Log.w(TAG, "Auto Login thất bại. Code: " + response.code());
                    Toast.makeText(RegisterActivity.this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Lỗi kết nối Auto Login", t);
                navigateToLogin();
            }
        });
    }

    // =========================================================================
    // 5. HELPER METHODS
    // =========================================================================

    private boolean validateInput(String name, String email, String password, String confirm) {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void handleRegisterError(Response<?> response) {
        String errorMessage = "Đăng ký thất bại";
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                if (errorBody.contains("email") || errorBody.contains("Email")) {
                    errorMessage = "Email này đã được sử dụng";
                } else {
                    errorMessage = "Lỗi server: " + response.code();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi parse error body", e);
        }
        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean isLoading) {
        registerButton.setEnabled(!isLoading);
        registerButton.setText(isLoading ? "Đang xử lý..." : "Sign up");
    }

    private void navigateToHome(String userId) {
        Log.d(TAG, "Chuyển hướng -> HomeActivity");
        Toast.makeText(this, "Chào mừng bạn gia nhập!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
        intent.putExtra("USER_ID", userId);
        // Xóa Stack để user không Back lại màn hình Register được
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Log.d(TAG, "Chuyển hướng -> LoginActivity");
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}