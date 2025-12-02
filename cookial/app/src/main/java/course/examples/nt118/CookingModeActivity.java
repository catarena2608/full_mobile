package course.examples.nt118;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;

import course.examples.nt118.model.RecipeResponse;
import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CookingModeActivity extends AppCompatActivity {

    private static final String TAG = CookingModeActivity.class.getSimpleName();

    // UI Components
    private TextView stepCounterTextView, stepNumberTextView, instructionTextView, timerTextView, tipTextView;
    private ImageButton nextButton, prevButton, pauseButton, micButton;

    // Data
    private List<RecipeResponse.Step> steps;
    private int currentStep = 0;
    private String postID;

    // Timer State
    private CountDownTimer countDownTimer;
    private long timeRemaining = 0;
    private boolean isTimerRunning = false;

    // TTS
    private TextToSpeech tts;

    // ==================================================================
    // 1. LIFECYCLE LOGS
    // ==================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "1. onCreate: Khởi tạo CookingMode");
        setContentView(R.layout.activity_cooking_mode);

        postID = getIntent().getStringExtra("postID");
        if (TextUtils.isEmpty(postID)) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID bài viết", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupTTS();

        // Load dữ liệu
        loadRecipeFromBackend();

        setupListeners();
    }

    @Override
    protected void onStart() { super.onStart(); Log.d(TAG, "2. onStart"); }

    @Override
    protected void onResume() { super.onResume(); Log.d(TAG, "3. onResume"); }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "4. onPause");
        // Tạm dừng Timer nếu app bị ẩn (tùy chọn UX)
        if (isTimerRunning) toggleTimer();
        // Dừng đọc giọng nói
        if (tts != null) tts.stop();
    }

    @Override
    protected void onStop() { super.onStop(); Log.d(TAG, "5. onStop"); }

    @Override
    protected void onRestart() { super.onRestart(); Log.d(TAG, "6. onRestart"); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "7. onDestroy: Giải phóng Timer và TTS");
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    // ==================================================================
    // 2. SETUP UI & LISTENERS
    // ==================================================================

    private void initViews() {
        stepCounterTextView = findViewById(R.id.stepCounterTextView);
        stepNumberTextView = findViewById(R.id.stepNumberTextView);
        instructionTextView = findViewById(R.id.instructionTextView);
        tipTextView = findViewById(R.id.tipTextView);
        timerTextView = findViewById(R.id.timerTextView);

        nextButton = findViewById(R.id.nextButton);
        prevButton = findViewById(R.id.prevButton);
        pauseButton = findViewById(R.id.pauseButton);
        micButton = findViewById(R.id.micButton);
    }

    private void setupListeners() {
        pauseButton.setOnClickListener(v -> toggleTimer());

        nextButton.setOnClickListener(v -> {
            Log.d(TAG, "User click: Next Step");
            showNextStep();
        });

        prevButton.setOnClickListener(v -> {
            Log.d(TAG, "User click: Prev Step");
            showPrevStep();
        });

        micButton.setOnClickListener(v -> {
            Log.d(TAG, "User click: Speak Instruction");
            speakCurrentInstruction();
        });

        // Nút Back trên toolbar (nếu có) hoặc nút back cứng
        // findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    // ==================================================================
    // 3. API & DATA LOADING
    // ==================================================================

    private void loadRecipeFromBackend() {
        Log.d(TAG, "API: Fetching recipe detail for ID: " + postID);

        // Gọi Singleton ApiService
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        Call<RecipeResponse> call = apiService.getRecipeByPostID(postID);

        call.enqueue(new Callback<RecipeResponse>() {
            @Override
            public void onResponse(Call<RecipeResponse> call, Response<RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Log.i(TAG, "API Success: Loaded recipe steps");

                    steps = response.body().recipe.guide;
                    String totalTimeStr = response.body().recipe.time; // ví dụ "15 phút"

                    // Khởi tạo Timer
                    long totalTimeMs = parseMinutes(totalTimeStr) * 60 * 1000L;
                    startTimer(totalTimeMs);

                    // Hiển thị bước đầu tiên
                    showStep(currentStep);
                } else {
                    Log.e(TAG, "API Error or No Data");
                    Toast.makeText(CookingModeActivity.this, "Không tải được dữ liệu công thức", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RecipeResponse> call, Throwable t) {
                Log.e(TAG, "Network Error", t);
                Toast.makeText(CookingModeActivity.this, "Lỗi kết nối server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int parseMinutes(String timeText) {
        if (timeText == null) return 10;
        try {
            // Lấy tất cả số trong chuỗi (vd: "Khoảng 15-20 phút" -> lấy 1520 -> logic này cần cẩn thận)
            // Tốt nhất lấy số đầu tiên tìm thấy
            String numberOnly = timeText.replaceAll("[^0-9]", "");
            if (numberOnly.isEmpty()) return 10; // Default
            return Integer.parseInt(numberOnly);
        } catch (Exception e) {
            Log.w(TAG, "Parse time error: " + timeText);
            return 10;
        }
    }

    // ==================================================================
    // 4. TIMER LOGIC
    // ==================================================================

    private void startTimer(long durationMs) {
        if (countDownTimer != null) countDownTimer.cancel();

        timeRemaining = durationMs;
        Log.d(TAG, "Timer Started: " + durationMs + "ms");

        countDownTimer = new CountDownTimer(timeRemaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemaining = millisUntilFinished;
                timerTextView.setText(formatTime(millisUntilFinished));
            }

            @Override
            public void onFinish() {
                Log.i(TAG, "Timer Finished");
                timerTextView.setText("00:00");
                isTimerRunning = false;
                // Đổi icon về Play hoặc trạng thái hoàn thành
                pauseButton.setImageResource(android.R.drawable.ic_media_play);
                Toast.makeText(CookingModeActivity.this, "Đã hết thời gian nấu!", Toast.LENGTH_LONG).show();
            }
        };

        countDownTimer.start();
        isTimerRunning = true;
        // Icon Pause (Đang chạy thì hiện nút để Pause)
        pauseButton.setImageResource(android.R.drawable.ic_media_pause);
    }

    private void toggleTimer() {
        if (isTimerRunning) {
            // Đang chạy -> Bấm để Pause
            Log.d(TAG, "Action: Pause Timer");
            countDownTimer.cancel();
            isTimerRunning = false;
            // Hiện icon Play
            pauseButton.setImageResource(android.R.drawable.ic_media_play);
        } else {
            // Đang Pause -> Bấm để Resume
            Log.d(TAG, "Action: Resume Timer");
            startTimer(timeRemaining);
        }
    }

    private String formatTime(long ms) {
        int minutes = (int) (ms / 1000) / 60;
        int seconds = (int) (ms / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    // ==================================================================
    // 5. STEP NAVIGATION & TTS
    // ==================================================================

    private void showStep(int index) {
        if (steps == null || steps.isEmpty()) return;

        // Index an toàn
        if (index < 0) index = 0;
        if (index >= steps.size()) index = steps.size() - 1;

        RecipeResponse.Step step = steps.get(index);

        // Update UI
        stepCounterTextView.setText("Bước " + (index + 1) + "/" + steps.size());
        stepNumberTextView.setText(String.valueOf(step.step));
        instructionTextView.setText(step.content);

        // Nếu API có trường 'media' cho từng bước, load ảnh ở đây (nếu có ImageView)

        // Đọc to
        speakCurrentInstruction();
    }

    private void showNextStep() {
        if (steps != null && currentStep < steps.size() - 1) {
            currentStep++;
            showStep(currentStep);
        } else {
            Toast.makeText(this, "Đây là bước cuối cùng", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPrevStep() {
        if (currentStep > 0) {
            currentStep--;
            showStep(currentStep);
        } else {
            Toast.makeText(this, "Đây là bước đầu tiên", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("vi", "VN"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS: Language not supported");
                    Toast.makeText(this, "Thiết bị chưa hỗ trợ giọng đọc tiếng Việt", Toast.LENGTH_SHORT).show();
                } else {
                    Log.i(TAG, "TTS Initialized");
                }
            } else {
                Log.e(TAG, "TTS Initialization Failed");
            }
        });
    }

    private void speakCurrentInstruction() {
        if (tts != null && instructionTextView.getText() != null) {
            String text = instructionTextView.getText().toString();
            // QUEUE_FLUSH: Ngắt câu đang đọc dở để đọc câu mới
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
}