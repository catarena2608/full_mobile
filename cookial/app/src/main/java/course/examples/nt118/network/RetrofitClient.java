package course.examples.nt118.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import course.examples.nt118.config.ApiConfig;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String TAG = "RetrofitClient";
    private static final String PREF_NAME = "MY_APP_PREFS";
    private static final String KEY_COOKIES = "COOKIES";

    // Header chuẩn
    private static final String HEADER_COOKIE = "Cookie";
    private static final String HEADER_SET_COOKIE = "Set-Cookie";

    private static volatile RetrofitClient instance;
    private final ApiService apiService;
    private final SharedPreferences sharedPreferences;

    private RetrofitClient(Context context) {
        // Dùng Application Context để tránh leak memory
        this.sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(createLoggingInterceptor())
                .addInterceptor(createAddCookiesInterceptor())      // 1. Gửi Cookie đi
                .addInterceptor(createReceivedCookiesInterceptor()) // 2. Nhận Cookie về
                .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true) // Tự động thử lại nếu rớt mạng nhẹ
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiConfig.getBaseUrl())
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public static RetrofitClient getInstance(Context context) {
        if (instance == null) {
            synchronized (RetrofitClient.class) {
                if (instance == null) {
                    instance = new RetrofitClient(context);
                }
            }
        }
        return instance;
    }

    public ApiService getApiService() {
        return apiService;
    }

    private HttpLoggingInterceptor createLoggingInterceptor() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        return logging;
    }

    /**
     * INTERCEPTOR 1: GỬI COOKIE
     * Logic: Lấy tất cả cookie đã lưu -> Gom thành 1 chuỗi duy nhất -> Gửi đi
     */
    private Interceptor createAddCookiesInterceptor() {
        return chain -> {
            Request.Builder builder = chain.request().newBuilder();

            // Lấy danh sách cookie từ bộ nhớ
            Set<String> preferences = sharedPreferences.getStringSet(KEY_COOKIES, new HashSet<>());

            if (!preferences.isEmpty()) {
                StringBuilder cookieHeader = new StringBuilder();

                for (String cookie : preferences) {
                    if (cookieHeader.length() > 0) {
                        cookieHeader.append("; "); // Ngăn cách bằng dấu chấm phẩy và khoảng trắng
                    }
                    // Chỉ lấy phần "name=value", bỏ qua các phần như Path, Expires...
                    String[] parser = cookie.split(";");
                    cookieHeader.append(parser[0]);
                }

                // QUAN TRỌNG: Chỉ addHeader 1 lần duy nhất với chuỗi đã gom
                builder.addHeader(HEADER_COOKIE, cookieHeader.toString());
                Log.d(TAG, "Sending Cookie Header: " + cookieHeader.toString());
            }

            return chain.proceed(builder.build());
        };
    }

    /**
     * INTERCEPTOR 2: NHẬN VÀ LƯU COOKIE
     * Logic: Nhận cookie mới -> Trộn (Merge) với cookie cũ -> Lưu lại
     */
    private Interceptor createReceivedCookiesInterceptor() {
        return chain -> {
            Response originalResponse = chain.proceed(chain.request());

            if (!originalResponse.headers(HEADER_SET_COOKIE).isEmpty()) {

                // 1. Lấy Cookies cũ ra trước
                Set<String> oldCookies = sharedPreferences.getStringSet(KEY_COOKIES, new HashSet<>());

                // Dùng Map để lọc trùng (Key là tên cookie, Value là toàn bộ chuỗi cookie)
                Map<String, String> cookieMap = new HashMap<>();

                // Đổ cookie cũ vào Map
                for (String c : oldCookies) {
                    String[] parser = c.split("=", 2);
                    if (parser.length == 2) {
                        cookieMap.put(parser[0].trim(), c);
                    }
                }

                // 2. Lấy Cookies mới từ Server
                for (String header : originalResponse.headers(HEADER_SET_COOKIE)) {
                    // Cập nhật vào Map (Cái mới sẽ đè cái cũ)
                    String[] parser = header.split("=", 2);
                    if (parser.length == 2) {
                        // Lấy tên cookie (phần trước dấu =)
                        String key = parser[0].trim();
                        cookieMap.put(key, header);
                        Log.d(TAG, "Received & Updated Cookie: " + key);
                    }
                }

                // 3. Lưu lại tập hợp mới vào SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putStringSet(KEY_COOKIES, new HashSet<>(cookieMap.values()));
                editor.commit(); // Dùng commit để đảm bảo lưu đồng bộ
            }
            return originalResponse;
        };
    }

    // Hàm xóa cookie khi Logout
    public static void clearCookies(Context context) {
        context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_COOKIES)
                .commit();
        Log.d(TAG, "Cleared all cookies");
    }
}