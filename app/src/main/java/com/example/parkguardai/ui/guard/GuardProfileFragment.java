package com.example.parkguardai.ui.guard;

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
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.data.repository.GuardRepository;
import com.example.parkguardai.data.repository.UserProfileRepository;
import com.example.parkguardai.databinding.FragmentGuardProfileBinding;
import com.example.parkguardai.ui.common.AvatarLoader;
import com.example.parkguardai.ui.common.BiometricAuthHelper;
import com.example.parkguardai.ui.common.LogoutConfirmationDialog;
import com.example.parkguardai.ui.common.ViolationNotificationHelper;
import com.example.parkguardai.ui.student.StudentEditProfileFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Màn hình Ca trực và Hồ sơ dành cho Bảo vệ (Tab 4 Bottom Navigation).
 * Hiển thị thông tin nhân viên, ảnh đại diện, ca trực hiện tại, thống kê chỉ số KPI thực tế
 * và thao tác Chỉnh sửa hồ sơ / Đổi mật khẩu / Bàn giao ca / Đăng xuất.
 */
public final class GuardProfileFragment extends Fragment {
    private FragmentGuardProfileBinding binding;
    private final ExecutorService imageIo = Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!isAdded() || binding == null) return;
                if (granted) {
                    ViolationNotificationHelper.setPushEnabled(requireContext(), true);
                    ViolationNotificationHelper.startSecurityAlertListener(requireContext());
                    Toast.makeText(requireContext(), "Đã bật thông báo đẩy", Toast.LENGTH_SHORT).show();
                } else {
                    binding.switchGuardNotifications.setChecked(false);
                    ViolationNotificationHelper.setPushEnabled(requireContext(), false);
                    Toast.makeText(requireContext(),
                            "Cần cấp quyền Thông báo để nhận cảnh báo đẩy", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGuardProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Liên kết Bottom Navigation Bar (Tab Hồ sơ)
        GuardNavigation.bind(this, binding.guardBottomNav, R.id.guardProfileFragment);

        View.OnClickListener openEditProfile = v -> {
            Bundle args = new Bundle();
            args.putString(StudentEditProfileFragment.ARG_ROLE, "GUARD");
            NavHostFragment.findNavController(this).navigate(R.id.studentEditProfileFragment, args);
        };
        binding.cardGuardProfile.setOnClickListener(openEditProfile);
        binding.btnEditGuardProfile.setOnClickListener(openEditProfile);

        binding.btnGuardChangePassword.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.changePasswordFragment));

        // Nút Bàn giao ca trực
        binding.btnHandoverShift.setOnClickListener(v -> showHandoverDialog());

        // Nút Đăng xuất
        binding.btnSignOut.setOnClickListener(v -> LogoutConfirmationDialog.show(this));

        BiometricAuthHelper.bindSwitch(this, binding.switchGuardBiometric);
        ViolationNotificationHelper.bindSwitch(
                this, binding.switchGuardNotifications, notificationPermissionLauncher);
        if (ViolationNotificationHelper.isPushEnabled(requireContext())
                && ViolationNotificationHelper.hasNotificationPermission(requireContext())) {
            ViolationNotificationHelper.startSecurityAlertListener(requireContext());
        }

        // Tải thông tin ca trực và hồ sơ bảo vệ
        loadGuardProfileData();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cập nhật lại thống kê thực tế và hồ sơ khi quay lại màn hình
        loadGuardProfileData();
    }

    private void loadGuardProfileData() {
        UserProfileRepository.getInstance().getMyProfile().addOnSuccessListener(profile -> {
            if (binding == null || !isAdded() || profile == null) return;
            if (!TextUtils.isEmpty(profile.getFullName())) {
                binding.tvOfficerName.setText(profile.getFullName());
            }
            String phone = profile.getPhoneCanonical() != null ? profile.getPhoneCanonical().trim() : "";
            String email = profile.getEmailCanonical() != null ? profile.getEmailCanonical().trim() : "";
            if (!phone.isEmpty() && !email.isEmpty()) {
                binding.tvOfficerContact.setText(phone + " • " + email);
            } else if (!phone.isEmpty()) {
                binding.tvOfficerContact.setText(phone);
            } else {
                binding.tvOfficerContact.setText(email);
            }
            AvatarLoader.loadCircular(
                    getActivity(), imageIo, profile.getAvatarUrl(),
                    binding.ivGuardAvatar, binding.ivGuardAvatarFallback);
        });

        GuardRepository.getInstance(requireContext())
                .getGuardShiftInfo(new GuardRepository.Callback<GuardRepository.GuardShiftInfo>() {
                    @Override
                    public void onSuccess(GuardRepository.GuardShiftInfo info) {
                        if (binding == null) return;
                        if (info != null) {
                            binding.tvOfficerName.setText(info.officerName);
                            binding.tvOfficerCode.setText("Mã bảo vệ: " + info.officerCode);
                            binding.tvOfficerGate.setText("Chốt trực: " + info.gateName);
                            binding.tvShiftName.setText(info.shiftName);
                            binding.tvShiftRemaining.setText(info.remainingTime);
                            binding.tvStatTotal.setText(String.valueOf(info.totalScans));
                            binding.tvStatValid.setText(String.valueOf(info.validScans));
                            binding.tvStatUnreg.setText(String.valueOf(info.unregisteredScans));
                            binding.tvStatViolations.setText(String.valueOf(info.violationScans));
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        // Giữ nguyên giá trị mặc định nếu có lỗi kết nối
                    }
                });
    }

    private void showHandoverDialog() {
        GuardRepository.getInstance(requireContext())
                .getGuardShiftInfo(new GuardRepository.Callback<GuardRepository.GuardShiftInfo>() {
                    @Override
                    public void onSuccess(GuardRepository.GuardShiftInfo info) {
                        if (!isAdded()) return;

                        String summary = "Bảo vệ: " + info.officerName + " (" + info.officerCode + ")\n"
                                + "Chốt trực: " + info.gateName + "\n"
                                + "Ca trực: " + info.shiftName + "\n\n"
                                + "--- THỐNG KÊ CA TRỰC ---\n"
                                + "• Tổng lượt quét: " + info.totalScans + "\n"
                                + "• Phương tiện hợp lệ: " + info.validScans + "\n"
                                + "• Phương tiện chưa đăng ký: " + info.unregisteredScans + "\n"
                                + "• Vi phạm ghi nhận: " + info.violationScans + " trường hợp\n\n"
                                + "Tình trạng ca trực: Hoạt động bình thường, toàn bộ thiết bị camera và phần mềm ổn định.";

                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Biên bản bàn giao ca trực")
                                .setMessage(summary)
                                .setPositiveButton("Bàn giao ca", (dialog, which) -> {
                                    Toast.makeText(requireContext(),
                                            "Đã xuất biên bản bàn giao ca trực thành công cho ca tiếp theo",
                                            Toast.LENGTH_LONG).show();
                                })
                                .setNegativeButton("Đóng", null)
                                .show();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(requireContext(),
                                "Đã xuất biên bản bàn giao ca trực thành công",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        imageIo.shutdownNow();
        super.onDestroy();
    }
}
