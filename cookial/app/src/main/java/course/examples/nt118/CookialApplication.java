package course.examples.nt118;

import android.app.Application;
import android.util.Log;

import course.examples.nt118.network.RetrofitClient;

/**
 * Application class để khởi tạo các components chung của app
 * Được khai báo trong AndroidManifest.xml
 */
public class CookialApplication extends Application {

    private static final String TAG = "CookialApplication";

    @Override
    public void onCreate() {
        super.onCreate();
    }
}

