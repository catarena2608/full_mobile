package course.examples.nt118;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class CookingEndActivity extends AppCompatActivity {

    private ImageView ivSuccessIcon;
    private TextView tvTitle, tvDescription;
    private Button btnTakePhoto, btnUpload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cooking_end);

        // Ánh xạ view
        ivSuccessIcon = findViewById(R.id.iv_success_icon);
        tvTitle = findViewById(R.id.tv_title);
        tvDescription = findViewById(R.id.tv_description);
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        btnUpload = findViewById(R.id.btn_upload);

        // Sự kiện button chụp ảnh
        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Code mở camera
                // Ví dụ:
                // Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // startActivityForResult(intent, 100);
            }
        });

        // Sự kiện button upload ảnh
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Code mở màn hình upload hoặc chọn ảnh
                // Ví dụ:
                // Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                // intent.setType("image/*");
                // startActivity(intent);
            }
        });

        Button btnBackHome = findViewById(R.id.btn_back_home);

        btnBackHome.setOnClickListener(v -> {
            Intent intent = new Intent(CookingEndActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

    }
}
