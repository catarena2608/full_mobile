package course.examples.nt118.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.Gson;

import course.examples.nt118.model.UserResponse;

public class UserDao extends SQLiteOpenHelper {

    private static final String DB_NAME = "CookialUser.db"; // Tên file DB riêng cho User
    private static final int DB_VERSION = 1;
    private static final String TABLE_USER = "user";

    // Các cột trong bảng User
    private static final String COL_ID = "id";
    private static final String COL_NAME = "name";
    private static final String COL_EMAIL = "email";
    private static final String COL_AVATAR = "avatar";
    private static final String COL_PREFERENCE = "preference";

    public UserDao(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng User
        String createTable = "CREATE TABLE " + TABLE_USER + " (" +
                COL_ID + " TEXT PRIMARY KEY, " +
                COL_NAME + " TEXT, " +
                COL_EMAIL + " TEXT, " +
                COL_AVATAR + " TEXT, " +
                COL_PREFERENCE + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        onCreate(db);
    }

    // --- HÀM LƯU USER (Gọi khi Login thành công) ---
    public void saveUser(UserResponse user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COL_ID, user.getId());
        values.put(COL_NAME, user.getName());
        values.put(COL_EMAIL, user.getEmail());
        values.put(COL_AVATAR, user.getAvatar());

        // Chuyển Object Preference thành chuỗi JSON để lưu
        if (user.getPreference() != null) {
            String prefJson = new Gson().toJson(user.getPreference());
            values.put(COL_PREFERENCE, prefJson);
        }

        // Lưu đè nếu đã tồn tại
        db.insertWithOnConflict(TABLE_USER, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    // --- HÀM LẤY USER (Dùng cho Profile) ---
    @SuppressLint("Range")
    public UserResponse getUser() {
        SQLiteDatabase db = this.getReadableDatabase();
        UserResponse user = null;
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USER + " LIMIT 1", null);

        if (cursor.moveToFirst()) {
            user = new UserResponse();
            user.setId(cursor.getString(cursor.getColumnIndex(COL_ID)));
            user.setName(cursor.getString(cursor.getColumnIndex(COL_NAME)));
            user.setEmail(cursor.getString(cursor.getColumnIndex(COL_EMAIL)));
            user.setAvatar(cursor.getString(cursor.getColumnIndex(COL_AVATAR)));

            // Lấy JSON Preference và chuyển ngược lại thành Object
            String prefString = cursor.getString(cursor.getColumnIndex(COL_PREFERENCE));
            if (prefString != null) {
                UserResponse.Preference pref = new Gson().fromJson(prefString, UserResponse.Preference.class);
                user.setPreference(pref);
            }
        }
        cursor.close();
        db.close();
        return user;
    }

    // Hàm xóa user khi logout
    public void clearUser() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_USER);
        db.close();
    }
}