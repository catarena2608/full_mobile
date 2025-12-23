package course.examples.nt118;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
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

    private static final String TAG = "CookingModeLifecycle";
    private static final int VOICE_RECOGNITION_CODE = 999;

    // UI Components
    private TextView stepCounterTextView, stepNumberTextView, instructionTextView, timerTextView, tipTextView;
    private ImageButton nextButton, prevButton, pauseButton, micButton;
    private ImageView backButton;

    // Data
    private List<RecipeResponse.Step> steps;
    private int currentStep = 0;

    // Helpers
    private CountDownTimer countDownTimer;
    private long timeRemaining = 0;
    private boolean isTimerRunning = false;
    private TextToSpeech tts;

    // --------------------------------------------------------------------------
    // LIFECYCLE METHODS
    // --------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Activity Created");
        setContentView(R.layout.activity_cooking_mode);

        initViews();
        setupTTS();
        setupBackNavigation();
        loadRecipeFromBackend();
        setupEvents();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Activity become visible");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Activity interacting with user");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Activity partially obscured");
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Activity hidden");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart: Activity restarting");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: Cleanup resources");
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }

    // --------------------------------------------------------------------------
    // SETUP & INITIALIZATION
    // --------------------------------------------------------------------------

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
        backButton = findViewById(R.id.backButton);
    }

    private void setupEvents() {
        pauseButton.setOnClickListener(v -> toggleTimer());
        nextButton.setOnClickListener(v -> showNextStep());
        prevButton.setOnClickListener(v -> showPrevStep());
        micButton.setOnClickListener(v -> startVoiceControl());
    }

    private void setupBackNavigation() {
        Runnable showDialogAction = this::showExitConfirmationDialog;

        // UI Back Button
        backButton.setOnClickListener(v -> showDialogAction.run());

        // System Back Gesture/Button
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showDialogAction.run();
            }
        });
    }

    private void setupTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("vi", "VN"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS: Language not supported");
                }
            } else {
                Log.e(TAG, "TTS: Initialization failed");
            }
        });
    }

    // --------------------------------------------------------------------------
    // BUSINESS LOGIC: DATA LOADING
    // --------------------------------------------------------------------------

    private void loadRecipeFromBackend() {
        String postID = getIntent().getStringExtra("postID");
        if (postID == null) {
            Log.e(TAG, "loadRecipe: Missing PostID");
            return;
        }

        ApiService api = RetrofitClient.getInstance(this).getApiService();
        api.getRecipeByPostID(postID).enqueue(new Callback<RecipeResponse>() {
            @Override
            public void onResponse(Call<RecipeResponse> call, Response<RecipeResponse> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().success) {
                    Log.e(TAG, "loadRecipe: Failed to load data");
                    return;
                }
                setupRecipeData(response.body().getRecipe());
            }

            @Override
            public void onFailure(Call<RecipeResponse> call, Throwable t) {
                Log.e(TAG, "loadRecipe: Network error", t);
                Toast.makeText(CookingModeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecipeData(RecipeResponse.Recipe recipe) {
        if (recipe == null) return;

        steps = recipe.getGuide();
        if (steps != null && !steps.isEmpty()) {
            showStep(currentStep);
        } else {
            instructionTextView.setText("Chưa có hướng dẫn.");
        }

        String totalTime = recipe.getTime();
        long duration = (totalTime != null && !totalTime.isEmpty())
                ? parseMinutes(totalTime) * 60 * 1000L
                : 15 * 60 * 1000L;

        startTimer(duration);
    }

    // --------------------------------------------------------------------------
    // BUSINESS LOGIC: TIMER & NAVIGATION
    // --------------------------------------------------------------------------

    private int parseMinutes(String timeText) {
        try {
            return Integer.parseInt(timeText.replaceAll("\\D+", ""));
        } catch (Exception e) {
            return 10;
        }
    }

    private void startTimer(long durationMs) {
        if (countDownTimer != null) countDownTimer.cancel();

        timeRemaining = durationMs;
        countDownTimer = new CountDownTimer(timeRemaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemaining = millisUntilFinished;
                timerTextView.setText(formatTime(millisUntilFinished));
            }

            @Override
            public void onFinish() {
                timerTextView.setText("Hoàn thành!");
                isTimerRunning = false;
            }
        };

        countDownTimer.start();
        isTimerRunning = true;
        pauseButton.setImageResource(R.drawable.ic_pause_white);
    }

    private void toggleTimer() {
        if (isTimerRunning) {
            countDownTimer.cancel();
            isTimerRunning = false;
            pauseButton.setImageResource(R.drawable.ic_play_white);
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

        RecipeResponse.Step step = steps.get(index);
        stepCounterTextView.setText(String.format("Bước %d trên %d", index + 1, steps.size()));
        stepNumberTextView.setText(String.valueOf(step.getStep()));
        instructionTextView.setText(step.getContent());

        speakCurrentInstruction();
    }

    private void showNextStep() {
        if (steps == null) return;
        if (currentStep < steps.size() - 1) {
            currentStep++;
            showStep(currentStep);
        } else {
            navigateToFinish();
        }
    }

    private void showPrevStep() {
        if (steps != null && currentStep > 0) {
            currentStep--;
            showStep(currentStep);
        }
    }

    private void navigateToFinish() {
        Log.d(TAG, "Navigating to Finish Screen");

        Intent intent = new Intent(this, CookingEndActivity.class);
        intent.putExtra("postID", getIntent().getStringExtra("postID"));
        startActivity(intent);
        finish(); // onDestroy will be called
    }

    private void showExitConfirmationDialog() {
        boolean wasRunning = isTimerRunning;
        if (isTimerRunning) toggleTimer();

        new AlertDialog.Builder(this)
                .setTitle("Dừng nấu ăn?")
                .setMessage("Tiến trình sẽ bị hủy. Bạn muốn rời khỏi không?")
                .setPositiveButton("Rời khỏi", (dialog, which) -> {
                    Log.d(TAG, "User chose to Exit");
                    finish();
                })
                .setNegativeButton("Ở lại", (dialog, which) -> {
                    dialog.dismiss();
                    if (wasRunning) toggleTimer();
                })
                .setCancelable(false)
                .show();
    }

    // --------------------------------------------------------------------------
    // VOICE CONTROL
    // --------------------------------------------------------------------------

    private void speakCurrentInstruction() {
        if (instructionTextView.getText() != null && tts != null) {
            tts.speak(instructionTextView.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void startVoiceControl() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Lệnh: Tiếp / Lùi / Dừng / Hoàn thành");

        try {
            startActivityForResult(intent, VOICE_RECOGNITION_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Không hỗ trợ giọng nói", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_RECOGNITION_CODE && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results == null || results.isEmpty()) return;

            String command = results.get(0).toLowerCase();
            Log.d(TAG, "Voice Command: " + command);

            if (command.contains("tiếp") || command.contains("next")) {
                showNextStep();
            } else if (command.contains("lùi") || command.contains("trước") || command.contains("quay lại")) {
                showPrevStep();
            } else if (command.contains("dừng") || command.contains("pause")) {
                if (isTimerRunning) toggleTimer();
            } else if (command.contains("chạy") || command.contains("tiếp tục")) {
                if (!isTimerRunning) toggleTimer();
            } else if (command.contains("hoàn thành") || command.contains("xong")) {
                navigateToFinish();
            }
        }
    }
}