package com.example.parkguardai.ui.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.data.model.ScanRuleConfig;
import com.example.parkguardai.data.repository.AdminRepository;
import com.example.parkguardai.data.repository.UserProfileRepository;
import com.example.parkguardai.databinding.FragmentAdminMoreBinding;
import com.example.parkguardai.ui.common.AvatarLoader;
import com.example.parkguardai.ui.common.BiometricAuthHelper;
import com.example.parkguardai.ui.common.LogoutConfirmationDialog;
import com.example.parkguardai.ui.common.ViolationNotificationHelper;
import com.example.parkguardai.ui.student.StudentEditProfileFragment;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Màn hình Cài đặt, Hồ sơ Quản trị viên & Quy tắc quét lặp của Cổng Quản trị viên.
 */
public class AdminMoreFragment extends Fragment {
    private FragmentAdminMoreBinding binding;
    private AdminRepository adminRepository;
    private final ExecutorService imageIo = Executors.newSingleThreadExecutor();
    private int selectedIntervalIndex = 1; // 0: 30s, 1: 60s, 2: 120s
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!isAdded() || binding == null) return;
                if (granted) {
                    ViolationNotificationHelper.setPushEnabled(requireContext(), true);
                    ViolationNotificationHelper.startSecurityAlertListener(requireContext());
                    Toast.makeText(requireContext(), "Đã bật thông báo đẩy", Toast.LENGTH_SHORT).show();
                } else {
                    binding.switchAdminNotifications.setChecked(false);
                    ViolationNotificationHelper.setPushEnabled(requireContext(), false);
                    Toast.makeText(requireContext(),
                            "Cần cấp quyền Thông báo để nhận cảnh báo đẩy", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminMoreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adminRepository = AdminRepository.getInstance(requireContext());

        // Liên kết thanh điều hướng đáy (Tab Cài đặt)
        AdminNavigation.bind(this, binding.bottomNav, R.id.adminMoreFragment);

        // Nút quay lại
        binding.btnBack.setOnClickListener(v -> {
            if (!NavHostFragment.findNavController(this).navigateUp()) {
                requireActivity().onBackPressed();
            }
        });

        // Nút đăng xuất
        binding.btnSignOut.setOnClickListener(v -> LogoutConfirmationDialog.show(this));

        // Hồ sơ quản trị viên: chỉnh sửa ảnh đại diện, họ tên, số điện thoại & đổi mật khẩu
        View.OnClickListener openAdminEditProfile = v -> {
            Bundle args = new Bundle();
            args.putString(StudentEditProfileFragment.ARG_ROLE, "ADMIN");
            NavHostFragment.findNavController(this).navigate(R.id.studentEditProfileFragment, args);
        };
        binding.cardAdminProfile.setOnClickListener(openAdminEditProfile);
        binding.btnEditAdminProfile.setOnClickListener(openAdminEditProfile);
        binding.rowAdminChangePassword.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.changePasswordFragment));

        BiometricAuthHelper.bindSwitch(this, binding.switchAdminBiometric);
        ViolationNotificationHelper.bindSwitch(
                this, binding.switchAdminNotifications, notificationPermissionLauncher);
        if (ViolationNotificationHelper.isPushEnabled(requireContext())
                && ViolationNotificationHelper.hasNotificationPermission(requireContext())) {
            ViolationNotificationHelper.startSecurityAlertListener(requireContext());
        }

        // Tải hồ sơ quản trị viên & cấu hình quy tắc hiện tại
        loadAdminProfile();
        loadScanRules();

        // Xử lý chọn khoảng thời gian gộp lần đọc trùng (30s / 60s / 120s)
        binding.btnInterval30s.setOnClickListener(v -> selectInterval(0));
        binding.btnInterval60s.setOnClickListener(v -> selectInterval(1));
        binding.btnInterval120s.setOnClickListener(v -> selectInterval(2));

        // Xử lý nút Lưu quy tắc
        binding.btnSaveRules.setOnClickListener(v -> {
            int interval = 60;
            if (selectedIntervalIndex == 0) interval = 30;
            else if (selectedIntervalIndex == 2) interval = 120;

            boolean alternateIO = binding.switchAlternateIO.isChecked();

            binding.btnSaveRules.setEnabled(false);
            ScanRuleConfig config = new ScanRuleConfig(interval, alternateIO, true, 10);
            adminRepository.saveScanRules(config).addOnSuccessListener(success -> {
                if (!isAdded() || binding == null) return;
                binding.btnSaveRules.setEnabled(true);
                Toast.makeText(requireContext(), "Đã lưu và áp dụng quy tắc quét lặp lên hệ thống!", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                if (!isAdded() || binding == null) return;
                binding.btnSaveRules.setEnabled(true);
                Toast.makeText(requireContext(), "Lỗi lưu quy tắc: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        });
    }

    private void loadScanRules() {
        adminRepository.getScanRulesAsync().addOnSuccessListener(rules -> {
            if (binding == null || !isAdded() || rules == null) return;
            int interval = rules.getDuplicateIntervalSeconds();
            if (interval <= 30) {
                selectInterval(0);
            } else if (interval >= 120) {
                selectInterval(2);
            } else {
                selectInterval(1);
            }
            binding.switchAlternateIO.setChecked(rules.isEnableDuplicateAlert());
        }).addOnFailureListener(e -> {
            if (!isAdded()) return;
            Toast.makeText(requireContext(), "Không thể tải quy tắc quét từ hệ thống", Toast.LENGTH_SHORT).show();
        });
    }

    private void selectInterval(int index) {
        selectedIntervalIndex = index;
        int activeBg = R.drawable.bg_segmented_active;
        int inactiveBg = R.drawable.bg_segmented_inactive;
        int activeColor = ContextCompat.getColor(requireContext(), R.color.white);
        int inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_muted);

        binding.btnInterval30s.setBackgroundResource(index == 0 ? activeBg : inactiveBg);
        binding.btnInterval30s.setTextColor(index == 0 ? activeColor : inactiveColor);

        binding.btnInterval60s.setBackgroundResource(index == 1 ? activeBg : inactiveBg);
        binding.btnInterval60s.setTextColor(index == 1 ? activeColor : inactiveColor);

        binding.btnInterval120s.setBackgroundResource(index == 2 ? activeBg : inactiveBg);
        binding.btnInterval120s.setTextColor(index == 2 ? activeColor : inactiveColor);
    }

    private void loadAdminProfile() {
        UserProfileRepository.getInstance().getMyProfile().addOnSuccessListener(profile -> {
            if (binding == null || !isAdded() || profile == null) return;
            String name = !TextUtils.isEmpty(profile.getFullName()) ? profile.getFullName() : "Quản trị viên";
            binding.tvAdminProfileName.setText(name);
            binding.tvAdminAvatarInitials.setText(extractInitials(name));
            binding.tvAdminProfileEmail.setText(profile.getEmailCanonical() != null ? profile.getEmailCanonical() : "");
            String phone = profile.getPhoneCanonical() != null ? profile.getPhoneCanonical().trim() : "";
            binding.tvAdminProfilePhone.setText(!phone.isEmpty() ? phone : "Chưa cập nhật số điện thoại");

            AvatarLoader.loadCircular(
                    getActivity(), imageIo, profile.getAvatarUrl(),
                    binding.ivAdminAvatar, binding.tvAdminAvatarInitials);
        });
    }

    private String extractInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "AD";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAdminProfile();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        imageIo.shutdownNow();
        super.onDestroy();
    }
}
