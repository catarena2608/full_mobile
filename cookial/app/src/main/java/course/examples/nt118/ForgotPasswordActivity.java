package course.examples.nt118;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
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

    private static final String TAG = ForgotPasswordActivity.class.getSimpleName();

    // UI Components
    private EditText emailEditText;
    private Button sendCodeButton;
    private TextView backToLoginTextView;
    // REMOVED: private LinearLayout btnGoogle, btnFacebook; (Not used in XML)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initViews();
        setupListeners();
    }

    private void initViews() {
        emailEditText = findViewById(R.id.emailEditText);
        sendCodeButton = findViewById(R.id.resetPasswordButton);
        backToLoginTextView = findViewById(R.id.backToLoginTextView);

        // REMOVED: These caused the crash because they don't exist in XML
        // btnGoogle = findViewById(R.id.btnGoogle);
        // btnFacebook = findViewById(R.id.btnFacebook);
    }

    private void setupListeners() {
        sendCodeButton.setOnClickListener(v -> handleSendOtp());

        backToLoginTextView.setOnClickListener(v -> {
            Log.d(TAG, "User click: Back to Login");
            Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // REMOVED: Social login listeners
        /* View.OnClickListener socialListener = v -> {
            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
        };
        btnGoogle.setOnClickListener(socialListener);
        btnFacebook.setOnClickListener(socialListener);
        */
    }

    private void handleSendOtp() {
        String email = emailEditText.getText().toString().trim();

        if (!validateEmail(email)) {
            return;
        }

        showLoading(true);

        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        apiService.forgotPassword(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                showLoading(false);
                if (response.isSuccessful()) {
                    onOtpSentSuccess(email);
                } else {
                    String msg = "Gửi mã thất bại";
                    if (response.code() == 404) msg = "Email này chưa được đăng ký";
                    Toast.makeText(ForgotPasswordActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

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
        Intent intent = new Intent(ForgotPasswordActivity.this, VerifyOtpActivity.class);
        intent.putExtra("email", email);
        startActivity(intent);
        finish();
    }
}