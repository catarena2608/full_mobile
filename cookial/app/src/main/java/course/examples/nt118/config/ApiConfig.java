package course.examples.nt118.config;

public class ApiConfig {

    // ================= CONFIG URL =================
    // 1. Dùng cho Android Emulator
    private static final String BASE_URL_EMULATOR = "http://10.0.2.2:3000/api/";

    // 2. Dùng cho Máy thật (Cùng Wifi). Thay IP này bằng IP máy tính của bạn (ipconfig/ifconfig)
    private static final String BASE_URL_DEVICE = "http://192.168.1.15:3000/api/";

    // 3. Server Production (Vercel/AWS...)
    private static final String BASE_URL_PRODUCTION = "http://192.168.100.200/api/";

    // ==> CHỌN URL HIỆN TẠI Ở ĐÂY <==
    private static final String CURRENT_BASE_URL = BASE_URL_PRODUCTION;

    // ================= GETTER =================
    public static String getBaseUrl() {
        return CURRENT_BASE_URL;
    }

    // ================= TIMEOUTS =================
    public static final long CONNECT_TIMEOUT = 60; // giây
    public static final long READ_TIMEOUT = 60;    // giây
}