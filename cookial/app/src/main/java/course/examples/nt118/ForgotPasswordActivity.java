package course.examples.nt118;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    // Tự động lấy tên class làm TAG
    private static final String TAG = ForgotPasswordActivity.class.getSimpleName();

    // UI Components
    private EditText emailEditText;
    private Button sendCodeButton;
    private TextView backToLoginTextView;
    private LinearLayout btnGoogle, btnFacebook;

    // =========================================================================
    // 1. LIFECYCLE LOGS
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "1. onCreate: Khởi tạo màn hình Forgot Password");
        setContentView(R.layout.activity_forgot_password);

        // Không cần init thủ công nữa

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
        emailEditText = findViewById(R.id.emailEditText);
        sendCodeButton = findViewById(R.id.actionButton); // Nút Send Code
        backToLoginTextView = findViewById(R.id.backToLoginTextView);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);
    }

    private void setupListeners() {
        sendCodeButton.setOnClickListener(v -> handleSendOtp());

        backToLoginTextView.setOnClickListener(v -> {
            Log.d(TAG, "User click: Back to Login");
            Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
            // Xóa các activity phía trên Login để tránh chồng stack
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        View.OnClickListener socialListener = v -> {
            Log.d(TAG, "User click: Social Login (Pending)");
            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
        };

        btnGoogle.setOnClickListener(socialListener);
        btnFacebook.setOnClickListener(socialListener);
    }

    private void handleSendOtp() {
        String email = emailEditText.getText().toString().trim();

        if (!validateEmail(email)) {
            Log.w(TAG, "Validate thất bại: Email trống hoặc sai định dạng");
            return;
        }

        Log.d(TAG, "User click: Gửi OTP tới email: " + email);
        showLoading(true);

        // [CẤU TRÚC MỚI] Gọi Singleton
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        apiService.forgotPassword(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                showLoading(false);
                if (response.isSuccessful()) {
                    Log.i(TAG, "API ForgotPass thành công (200 OK)");
                    onOtpSentSuccess(email);
                } else {
                    Log.e(TAG, "API trả lỗi. Code: " + response.code());
                    String msg = "Gửi mã thất bại";
                    if (response.code() == 404) msg = "Email này chưa được đăng ký";
                    Toast.makeText(ForgotPasswordActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Lỗi kết nối API: " + t.getMessage(), t);
                Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================================================================
    // 3. HELPER METHODS
    // =========================================================================

    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showLoading(boolean isLoading) {
        sendCodeButton.setEnabled(!isLoading);
        sendCodeButton.setText(isLoading ? "Đang gửi..." : "Send Code");
        emailEditText.setEnabled(!isLoading);
    }

    private void onOtpSentSuccess(String email) {
        Toast.makeText(this, "Mã OTP đã gửi về email!", Toast.LENGTH_SHORT).show();

        Log.d(TAG, "Chuyển hướng -> VerifyOtpActivity (kèm email)");
        // Chuyển sang màn hình xác thực OTP
        Intent intent = new Intent(ForgotPasswordActivity.this, VerifyOtpActivity.class);
        intent.putExtra("email", email); // QUAN TRỌNG: Truyền email sang màn hình sau
        startActivity(intent);
        finish();
    }
}