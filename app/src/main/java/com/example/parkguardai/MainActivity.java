package com.example.parkguardai;

import android.os.Bundle;
import android.animation.ValueAnimator;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.parkguardai.databinding.ActivityMainBinding;

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
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightNavigationBars(true);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            int bottom = Math.max(systemBars.bottom, ime.bottom);
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(binding.getRoot());

        splashScreen.setOnExitAnimationListener(provider -> {
            View splashView = provider.getView();
            if (!ValueAnimator.areAnimatorsEnabled()) {
                provider.remove();
                return;
            }
            splashView.animate()
                    .alpha(0f)
                    .scaleX(1.04f)
                    .scaleY(1.04f)
                    .setDuration(220L)
                    .withEndAction(provider::remove)
                    .start();
        });
    }
}
