package course.examples.nt118;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import course.examples.nt118.model.RecipeResponse;
import course.examples.nt118.network.ApiService;
import course.examples.nt118.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CookingModeActivity extends AppCompatActivity {

    TextView stepCounterTextView, stepNumberTextView, instructionTextView, timerTextView, tipTextView;
    ImageButton nextButton, prevButton, pauseButton, micButton;

    // Dùng List Step từ model RecipeResponse
    List<RecipeResponse.Step> steps;
    int currentStep = 0;

    CountDownTimer countDownTimer;
    long timeRemaining = 0;
    boolean isTimerRunning = false;

    TextToSpeech tts;

    private static final int VOICE_RECOGNITION_CODE = 999;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cooking_mode);

        initViews();
        setupTTS();
        loadRecipeFromBackend();

        pauseButton.setOnClickListener(v -> toggleTimer());
        nextButton.setOnClickListener(v -> showNextStep());
        prevButton.setOnClickListener(v -> showPrevStep());

        // 👉 Nút mic để điều khiển giọng nói
        micButton.setOnClickListener(v -> startVoiceControl());
    }

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

    private void loadRecipeFromBackend() {
        String postID = getIntent().getStringExtra("postID");
        if (postID == null) {
            Toast.makeText(this, "Thiếu ID bài viết", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gọi API Service thông qua Singleton getInstance(this)
        ApiService api = RetrofitClient.getInstance(this).getApiService();
        Call<RecipeResponse> call = api.getRecipeByPostID(postID);

        call.enqueue(new Callback<RecipeResponse>() {
            @Override
            public void onResponse(Call<RecipeResponse> call, Response<RecipeResponse> response) {
                // Kiểm tra response thành công và có dữ liệu
                if (!response.isSuccessful() || response.body() == null || !response.body().success) {
                    Toast.makeText(CookingModeActivity.this, "Không tải được dữ liệu recipe", Toast.LENGTH_SHORT).show();
                    return;
                }

                // [SỬA ĐỔI] Dùng Getter để lấy dữ liệu từ RecipeResponse mới
                RecipeResponse.Recipe recipe = response.body().getRecipe();

                if (recipe != null) {
                    steps = recipe.getGuide(); // Dùng getGuide()

                    if (steps == null || steps.isEmpty()) {
                        instructionTextView.setText("Chưa có hướng dẫn cho món này.");
                    } else {
                        showStep(currentStep);
                    }

                    // Xử lý thời gian (Dùng getTime())
                    String totalTime = recipe.getTime();
                    if (totalTime != null && !totalTime.isEmpty()) {
                        startTimer(parseMinutes(totalTime) * 60 * 1000L);
                    } else {
                        // Mặc định 15 phút nếu server không trả về time
                        startTimer(15 * 60 * 1000L);
                    }
                }
            }

            @Override
            public void onFailure(Call<RecipeResponse> call, Throwable t) {
                Toast.makeText(CookingModeActivity.this, "Lỗi kết nối server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int parseMinutes(String timeText) {
        try {
            // Lấy tất cả các chữ số trong chuỗi (VD: "45 mins" -> "45")
            return Integer.parseInt(timeText.replaceAll("\\D+", ""));
        } catch (Exception e) {
            return 10; // Mặc định 10 phút nếu lỗi parse
        }
    }

    private void startTimer(long durationMs) {
        timeRemaining = durationMs;

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(timeRemaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemaining = millisUntilFinished;
                timerTextView.setText(formatTime(millisUntilFinished));
            }

            @Override
            public void onFinish() {
                timerTextView.setText("Hoàn thành!");

                // Chuyển sang màn hình kết thúc (nếu có)
                // Intent intent = new Intent(CookingModeActivity.this, CookingEndActivity.class);
                // startActivity(intent);
                // finish();
            }
        };

        countDownTimer.start();
        isTimerRunning = true;
        pauseButton.setImageResource(R.drawable.ic_pause_white); // Đổi icon sang Pause
    }

    private void toggleTimer() {
        if (isTimerRunning) {
            countDownTimer.cancel();
            isTimerRunning = false;
            pauseButton.setImageResource(R.drawable.ic_play_white); // Đổi icon sang Play (cần icon này trong drawable)
        } else {
            startTimer(timeRemaining);
        }
    }

    private String formatTime(long ms) {
        int minutes = (int) (ms / 1000) / 60;
        int seconds = (int) (ms / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void showStep(int index) {
        if (steps == null || steps.isEmpty()) return;

        // [SỬA ĐỔI] Dùng Getter cho Step
        RecipeResponse.Step step = steps.get(index);

        stepCounterTextView.setText("Bước " + (index + 1) + " trên " + steps.size());

        // step.getStep() trả về int, cần convert sang String
        stepNumberTextView.setText(String.valueOf(step.getStep()));
        instructionTextView.setText(step.getContent());

        speakCurrentInstruction();
    }

    private void showNextStep() {
        if (steps != null && currentStep < steps.size() - 1) {
            currentStep++;
            showStep(currentStep);
        } else {
            Toast.makeText(this, "Đã là bước cuối cùng", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPrevStep() {
        if (steps != null && currentStep > 0) {
            currentStep--;
            showStep(currentStep);
        }
    }

    private void setupTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("vi", "VN"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Ngôn ngữ tiếng Việt không được hỗ trợ", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Khởi tạo giọng nói thất bại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void speakCurrentInstruction() {
        if (instructionTextView.getText() != null) {
            String text = instructionTextView.getText().toString();
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // -------------------------------
    // 🔊 VOICE CONTROL
    // -------------------------------

    private void startVoiceControl() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy nói: tiếp / lùi / tạm dừng");

        try {
            startActivityForResult(intent, VOICE_RECOGNITION_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Thiết bị không hỗ trợ nhận dạng giọng nói", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VOICE_RECOGNITION_CODE &&
                resultCode == Activity.RESULT_OK && data != null) {

            ArrayList<String> results =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            if (results == null || results.isEmpty()) return;

            String command = results.get(0).toLowerCase();

            // Xử lý lệnh giọng nói
            if (command.contains("tiếp") || command.contains("next")) {
                showNextStep();
            }
            else if (command.contains("lùi") || command.contains("quay lại") || command.contains("trước")) {
                showPrevStep();
            }
            else if (command.contains("dừng") || command.contains("pause")) {
                if (isTimerRunning) toggleTimer();
            }
            else if (command.contains("chạy") || command.contains("bắt đầu") || command.contains("tiếp tục")) {
                if (!isTimerRunning) toggleTimer();
            }
            else {
                Toast.makeText(this, "Không hiểu lệnh: " + command, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }
}