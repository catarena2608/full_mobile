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
}