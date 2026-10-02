package com.example.parkguardai;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.parkguardai.databinding.*;

/**
 * Activity gốc của ứng dụng ParkGuard.
 *
 * Activity này chỉ đóng vai trò chứa NavHost để quản lý các màn hình Fragment.
 * Logic nghiệp vụ được đặt ở các lớp và module phù hợp thay vì xử lý trực tiếp tại đây.
 */

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Cho phép các màn hình như Camera và Map sử dụng toàn bộ vùng hiển thị.
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

    }
}