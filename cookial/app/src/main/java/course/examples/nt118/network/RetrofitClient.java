package course.examples.nt118.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashSet;
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
    private static final String HEADER_COOKIE = "Cookie";
    private static final String HEADER_SET_COOKIE = "Set-Cookie";

    private static volatile RetrofitClient instance;
    private final ApiService apiService;

    // SỬA: Lấy SharedPreferences ngay từ constructor và dùng Application Context
    private final SharedPreferences sharedPreferences;

    private RetrofitClient(Context context) {
        // [QUAN TRỌNG] Luôn dùng getApplicationContext() để tránh leak Activity
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(createLoggingInterceptor())
                .addInterceptor(createAddCookiesInterceptor())
                .addInterceptor(createReceivedCookiesInterceptor())
                .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
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

    // ================= INTERCEPTORS =================

    private HttpLoggingInterceptor createLoggingInterceptor() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        return logging;
    }

    // SỬA: Dùng biến sharedPreferences của class
    private Interceptor createAddCookiesInterceptor() {
        return chain -> {
            Request.Builder builder = chain.request().newBuilder();
            Set<String> preferences = sharedPreferences.getStringSet(KEY_COOKIES, new HashSet<>());

            for (String cookie : preferences) {
                // SỬA: Chỉ lấy phần đầu tiên của Cookie
                String[] parser = cookie.split(";");
                String cleanCookie = parser[0];

                builder.addHeader(HEADER_COOKIE, cleanCookie);
                Log.d(TAG, "Sending Cookie: " + cleanCookie);
            }
            return chain.proceed(builder.build());
        };
    }

    // SỬA: Dùng biến sharedPreferences của class và dùng commit()
    private Interceptor createReceivedCookiesInterceptor() {
        return chain -> {
            Response originalResponse = chain.proceed(chain.request());

            if (!originalResponse.headers(HEADER_SET_COOKIE).isEmpty()) {
                HashSet<String> cookies = new HashSet<>(originalResponse.headers(HEADER_SET_COOKIE));

                // [QUAN TRỌNG] Dùng commit() để đảm bảo lưu xong trước khi đi tiếp
                sharedPreferences.edit().putStringSet(KEY_COOKIES, cookies).commit();

                for (String cookie : cookies) {
                    Log.d(TAG, "Received & Saved Cookie: " + cookie);
                }
            }
            return originalResponse;
        };
    }

    public static void clearCookies(Context context) {
        context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().remove(KEY_COOKIES).commit(); // Dùng commit luôn
    }
}