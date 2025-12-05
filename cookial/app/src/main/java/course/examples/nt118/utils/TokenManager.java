package course.examples.nt118.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class TokenManager {

    // [QUAN TRỌNG] Dùng tên này để khớp với RetrofitClient (để xóa cookie khi logout được sạch sẽ)
    private static final String PREF_NAME = "MY_APP_PREFS";
    private static final String KEY_USER_ID = "USER_ID";

        private static SharedPreferences getPrefs(Context context) {
            Log.d("TokenManager", "GetPrefs called from: " + context.getClass().getSimpleName());
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }

    // --- LƯU USER ID ---
    public static void saveUserId(Context context, String userId) {
        Log.d("TokenManager", "SAVING UserID: " + userId); // <--- LOG NÀY
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(KEY_USER_ID, userId);
        boolean success = editor.commit();
        Log.d("TokenManager", "Save Result: " + success); // <--- LOG NÀY
    }

    // --- LẤY USER ID ---
    public static String getUserId(Context context) {
        String id = getPrefs(context).getString(KEY_USER_ID, null);
        Log.d("TokenManager", "READING UserID: " + id); // <--- LOG NÀY
        return id;
    }

    // --- XÓA SESSION (KHI LOGOUT) ---
    public static void clearSession(Context context) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        // Xóa UserID
        editor.remove(KEY_USER_ID);
        // Xóa luôn COOKIES (nếu RetrofitClient dùng chung file này)
        editor.remove("COOKIES");
        editor.apply();
    }
    public static String getTokenFromCookie(Context context) {
        // Lấy SharedPreferences (Dùng hàm getPrefs có sẵn để đảm bảo đúng file)
        SharedPreferences prefs = getPrefs(context);

        // Lấy tập hợp các cookie đã lưu (Key "COOKIES" phải khớp với RetrofitClient)
        java.util.Set<String> cookies = prefs.getStringSet("COOKIES", new java.util.HashSet<>());

        for (String cookie : cookies) {
            // Tìm cookie chứa token (Format thường là: token=eyJhbGci...; Path=/; ...)
            if (cookie.trim().startsWith("token=")) {
                // Cắt lấy phần token chính
                String[] parts = cookie.split(";");
                String tokenPart = parts[0]; // Lấy "token=eyJhbGci..."

                // Bỏ chữ "token=" để lấy mã sạch
                return tokenPart.replace("token=", "");
            }
        }
        return null; // Không tìm thấy token
    }
}