package course.examples.nt118;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyOtpActivity extends AppCompatActivity {

    // Tự động lấy tên class làm TAG
    private static final String TAG = VerifyOtpActivity.class.getSimpleName();

    // UI Components
    private final EditText[] otpFields = new EditText[6];
    private Button continueButton;
    private TextView resendTextView;

    // Data
    private String email;
    private CountDownTimer countDownTimer;
    private boolean isResendEnabled = false;

    // =========================================================================
    // 1. LIFECYCLE LOGS
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "1. onCreate: Khởi tạo màn hình Verify OTP");
        setContentView(R.layout.activity_verify_otp);

        email = getIntent().getStringExtra("email");
        if (email == null) {
            Log.e(TAG, "Lỗi Critical: Không nhận được Email từ Intent");
            Toast.makeText(this, "Lỗi: Không xác định được tài khoản", Toast.LENGTH_SHORT).show();
            finish();
            return;
        } else {
            Log.i(TAG, "Đang xác thực OTP cho Email: " + email);
        }

        initViews();
        setupOtpInputs();
        setupListeners();
        startResendTimer();
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
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "7. onDestroy: Hủy Timer để tránh Memory Leak");
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    // =========================================================================
    // 2. UI & UX LOGIC
    // =========================================================================

    private void initViews() {
        otpFields[0] = findViewById(R.id.otp1);
        otpFields[1] = findViewById(R.id.otp2);
        otpFields[2] = findViewById(R.id.otp3);
        otpFields[3] = findViewById(R.id.otp4);
        otpFields[4] = findViewById(R.id.otp5);
        otpFields[5] = findViewById(R.id.otp6);

        continueButton = findViewById(R.id.continueButton);
        resendTextView = findViewById(R.id.resendTextView);
    }

    // Xử lý tự động nhảy ô nhập liệu khi gõ số hoặc xóa số
    private void setupOtpInputs() {
        for (int i = 0; i < otpFields.length; i++) {
            final int currentIndex = i;

            otpFields[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Nếu nhập 1 ký tự -> Nhảy sang ô tiếp theo
                    if (s.length() == 1 && currentIndex < otpFields.length - 1) {
                        otpFields[currentIndex + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Xử lý nút Backspace (Xóa)
            otpFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (otpFields[currentIndex].getText().toString().isEmpty() && currentIndex > 0) {
                        // Nếu ô hiện tại rỗng -> Quay lại ô trước đó và xóa
                        otpFields[currentIndex - 1].requestFocus();
                        otpFields[currentIndex - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private void setupListeners() {
        continueButton.setOnClickListener(v -> handleVerifyOtp());

        resendTextView.setOnClickListener(v -> {
            if (isResendEnabled) {
                Log.d(TAG, "User click: Resend OTP");
                resendOtp();
            } else {
                Log.d(TAG, "User click Resend nhưng chưa hết giờ đếm ngược");
            }
        });
    }

    // =========================================================================
    // 3. LOGIC TIMER (Đếm ngược)
    // =========================================================================

    private void startResendTimer() {
        Log.d(TAG, "Bắt đầu đếm ngược 60s...");
        isResendEnabled = false;
        resendTextView.setTextColor(getResources().getColor(android.R.color.black));

        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                resendTextView.setText("Không nhận được mã? Gửi lại sau " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "Timer kết thúc. Cho phép gửi lại.");
                isResendEnabled = true;
                resendTextView.setText("Không nhận được mã? Nhấn để GỬI LẠI");
                resendTextView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            }
        }.start();
    }

    // =========================================================================
    // 4. API CALLS
    // =========================================================================

    private void handleVerifyOtp() {
        String otp = getOtpFromFields();

        if (otp.length() != 6) {
            Log.w(TAG, "Validate thất bại: OTP chưa đủ 6 số");
            Toast.makeText(this, "Vui lòng nhập đủ 6 số OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Đang gọi API Verify OTP...");
        showLoading(true);

        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("otp", otp);

        apiService.verifyOtp(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                showLoading(false);
                if (response.isSuccessful()) {
                    Log.i(TAG, "API Verify thành công! Chuyển sang ResetPasswordActivity");
                    Toast.makeText(VerifyOtpActivity.this, "Xác thực thành công!", Toast.LENGTH_SHORT).show();

                    // Chuyển sang trang đặt lại mật khẩu (Quan trọng: Kèm email)
                    Intent intent = new Intent(VerifyOtpActivity.this, ResetPasswordActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                } else {
                    Log.e(TAG, "Verify thất bại. Code: " + response.code());
                    Toast.makeText(VerifyOtpActivity.this, "Mã OTP không đúng hoặc đã hết hạn", Toast.LENGTH_SHORT).show();
                    clearOtpFields();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Lỗi kết nối API Verify: " + t.getMessage(), t);
                Toast.makeText(VerifyOtpActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resendOtp() {
        Toast.makeText(this, "Đang gửi lại mã...", Toast.LENGTH_SHORT).show();
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();

        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        // Gọi lại endpoint Forgot Password để gửi lại mã
        apiService.forgotPassword(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.isSuccessful()){
                    Log.i(TAG, "API Resend thành công. Reset Timer.");
                    Toast.makeText(VerifyOtpActivity.this, "Đã gửi lại mã OTP!", Toast.LENGTH_SHORT).show();
                    startResendTimer(); // Reset lại đồng hồ
                } else {
                    Log.e(TAG, "API Resend lỗi. Code: " + response.code());
                    Toast.makeText(VerifyOtpActivity.this, "Gửi lại thất bại", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Lỗi kết nối API Resend", t);
                Toast.makeText(VerifyOtpActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================================================================
    // 5. HELPER METHODS
    // =========================================================================

    private String getOtpFromFields() {
        StringBuilder sb = new StringBuilder();
        for (EditText et : otpFields) {
            sb.append(et.getText().toString().trim());
        }
        return sb.toString();
    }

    private void clearOtpFields() {
        for (EditText et : otpFields) {
            et.setText("");
        }
        otpFields[0].requestFocus();
    }

    private void showLoading(boolean isLoading) {
        continueButton.setEnabled(!isLoading);
        continueButton.setText(isLoading ? "Đang xác thực..." : "Continue");
        for(EditText et : otpFields) et.setEnabled(!isLoading);
    }
}