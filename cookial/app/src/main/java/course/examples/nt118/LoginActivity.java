package course.examples.nt118;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

import course.examples.nt118.db.UserDao; // Đã sửa thành UserDao
import course.examples.nt118.model.LoginResponse;
import course.examples.nt118.model.UserResponse;
import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import course.examples.nt118.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView signupTextView, forgotPasswordTextView;

    // Khai báo UserDao để lưu thông tin người dùng vào SQLite
    private UserDao userDao;

    // ==================================================================
    // 1. LIFECYCLE LOGS
    // ==================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Dòng này đầu tiên
        Log.d(TAG, "4. onCreate");

        // [QUAN TRỌNG] Kiểm tra đăng nhập ngay tại đây
        String savedUserId = TokenManager.getUserId(this);
        if (!TextUtils.isEmpty(savedUserId)) {
            Log.i(TAG, "Auto Login: OK -> Home");

            // Khởi tạo Retrofit ngay để nó nạp Cookie từ SharedPreferences
            RetrofitClient.getInstance(this);

            navigateToHome(savedUserId);
            return; // DỪNG LUÔN, không chạy tiếp các dòng dưới
        }

        // Nếu chưa đăng nhập mới nạp giao diện
        setContentView(R.layout.activity_login);

        userDao = new UserDao(this);
        initViews();
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "2. onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "3. onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "4. onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "5. onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "6. onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "7. onDestroy");
        // Đóng database nếu cần thiết (SQLiteOpenHelper thường tự quản lý)
        if (userDao != null) {
            userDao.close();
        }
    }

    // ==================================================================
    // 2. LOGIC CODE
    // ==================================================================

    private void initViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.signInButton);
        signupTextView = findViewById(R.id.signUpTextView);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> loginUser());

        if (signupTextView != null) {
            signupTextView.setOnClickListener(v -> {
                Log.d(TAG, "User click: Chuyển sang màn hình Đăng ký");
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            });
        }

        if (forgotPasswordTextView != null) {
            forgotPasswordTextView.setOnClickListener(v -> {
                Log.d(TAG, "User click: Quên mật khẩu");
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
            });
        }
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (!validateInput(email, password)) {
            Log.w(TAG, "Validate thất bại: Email hoặc Pass không hợp lệ");
            return;
        }

        Log.d(TAG, "Bắt đầu gọi API Login với Email: " + email);
        showLoading(true);

        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        apiService.loginUser(body).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body().getUser();
                    if (user != null) {
                        Log.i(TAG, "API Login thành công. UserID: " + user.getId());

                        // 1. Lưu UserID vào SharedPreferences (để duy trì phiên đăng nhập)
                        TokenManager.saveUserId(LoginActivity.this, user.getId());
                        Log.d(TAG, "Đã lưu UserID vào TokenManager");

                        // 2. [QUAN TRỌNG] Lưu toàn bộ thông tin User vào SQLite bằng UserDao
                        userDao.saveUser(user);
                        Log.d(TAG, "Đã lưu thông tin chi tiết User vào SQLite (UserDao)");

                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                        navigateToHome(user.getId());
                    } else {
                        Log.e(TAG, "API trả về 200 OK nhưng User object bị null");
                        Toast.makeText(LoginActivity.this, "Lỗi dữ liệu người dùng", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "API Login thất bại. Code: " + response.code() + ", Message: " + response.message());

                    String msg = "Đăng nhập thất bại";
                    if (response.code() == 401) msg = "Sai email hoặc mật khẩu";
                    else if (response.code() == 404) msg = "Tài khoản không tồn tại";

                    Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Lỗi kết nối API (Network/Timeout): " + t.getMessage(), t);
                Toast.makeText(LoginActivity.this, "Lỗi kết nối server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToHome(String userId) {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
        Log.d(TAG, "Đóng LoginActivity (finish)");
        finish();
    }

    private boolean validateInput(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email không đúng định dạng", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showLoading(boolean isLoading) {
        loginButton.setEnabled(!isLoading);
        loginButton.setText(isLoading ? "Đang xử lý..." : "Sign in");
    }
}