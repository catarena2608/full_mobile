package course.examples.nt118; // Nhớ đổi package name cho đúng project của bạn

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.OutputStream;

public class MyQrCodeActivity extends AppCompatActivity {

    private ImageView ivQrCode, btnBack;
    private TextView tvQrUsername;
    private ImageButton btnShare, btnDownload, btnCopy;

    private Bitmap currentQrBitmap; // Biến lưu ảnh QR để dùng cho nút Share/Download
    private String userId;          // ID của user hiện tại
    private String username;        // Tên hiển thị

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code); // Tên file xml của bạn

        initViews();
        getDataFromIntent();
        setupActions();
    }

    private void initViews() {
        ivQrCode = findViewById(R.id.iv_qr_code);
        tvQrUsername = findViewById(R.id.tv_qr_username);
        btnBack = findViewById(R.id.btn_back);

        // Các ID này bạn nhớ thêm vào XML như mình dặn ở trên nhé
        btnShare = findViewById(R.id.btn_share);
        btnDownload = findViewById(R.id.btn_download);
        btnCopy = findViewById(R.id.btn_copy);
    }

    private void getDataFromIntent() {
        // Nhận dữ liệu từ ProfileActivity truyền sang
        userId = getIntent().getStringExtra("USER_ID");
        username = getIntent().getStringExtra("USERNAME");

        // Set tên user lên giao diện
        if (username != null) {
            tvQrUsername.setText(username);
        }

        // Tạo mã QR ngay khi mở màn hình
        if (userId != null) {
            generateQRCode(userId);
        }
    }

    private void generateQRCode(String id) {
        try {
            // Định dạng chuỗi nội dung QR: "cookial_user:" + id
            String content = "cookial_user:" + id;

            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            // Tạo Bitmap kích thước 500x500
            currentQrBitmap = barcodeEncoder.encodeBitmap(content, BarcodeFormat.QR_CODE, 500, 500);

            // Hiển thị lên ImageView
            ivQrCode.setImageBitmap(currentQrBitmap);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tạo mã QR", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupActions() {
        // 1. Nút Back
        btnBack.setOnClickListener(v -> finish());

        // 2. Nút Copy ID
        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("UserID", userId);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Đã sao chép ID người dùng!", Toast.LENGTH_SHORT).show();
        });

        // 3. Nút Download (Lưu ảnh)
        btnDownload.setOnClickListener(v -> {
            if (currentQrBitmap != null) {
                saveImageToGallery(currentQrBitmap);
            }
        });

        // 4. Nút Share (Chia sẻ ảnh)
        btnShare.setOnClickListener(v -> {
            if (currentQrBitmap != null) {
                shareImage(currentQrBitmap);
            }
        });
    }

    // Hàm lưu ảnh vào thư viện (Gallery)
    private void saveImageToGallery(Bitmap bitmap) {
        String fileName = "Cookial_QR_" + System.currentTimeMillis() + ".jpg";
        OutputStream fos;

        try {
            // Cấu hình lưu ảnh vào MediaStore (Android 10+ chuẩn)
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Cookial");

            Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = getContentResolver().openOutputStream(imageUri);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            if (fos != null) fos.close();

            Toast.makeText(this, "Đã lưu mã QR vào thư viện ảnh!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi lưu ảnh!", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm chia sẻ ảnh qua các app khác (Zalo, Messenger...)
    private void shareImage(Bitmap bitmap) {
        try {
            // Lưu tạm ảnh vào cache để share
            String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Cookial QR", null);
            Uri uri = Uri.parse(path);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/jpeg");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_TEXT, "Kết bạn với tôi trên Cookial nhé! ID: " + userId);
            startActivity(Intent.createChooser(intent, "Chia sẻ mã QR qua"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Không thể chia sẻ ảnh", Toast.LENGTH_SHORT).show();
        }
    }
}