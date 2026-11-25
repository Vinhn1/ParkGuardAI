package com.example.parkguardai.ui.student;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.data.model.UserProfile;
import com.example.parkguardai.data.model.VehicleItem;
import com.example.parkguardai.databinding.FragmentStudentProfileBinding;
import com.example.parkguardai.ui.common.AvatarLoader;
import com.example.parkguardai.ui.common.BiometricAuthHelper;
import com.example.parkguardai.ui.common.LogoutConfirmationDialog;
import com.example.parkguardai.ui.common.ViolationNotificationHelper;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Màn hình Hồ sơ sinh viên, quản lý phương tiện và cài đặt tài khoản từ Cloud Firestore. */
public class StudentProfileFragment extends Fragment {

    private FragmentStudentProfileBinding binding;
    private StudentViewModel viewModel;
    private final ExecutorService imageIo = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!isAdded() || binding == null) return;
                if (granted) {
                    ViolationNotificationHelper.setPushEnabled(requireContext(), true);
                    Toast.makeText(requireContext(),
                            "Đã bật thông báo đẩy khi có vi phạm mới", Toast.LENGTH_SHORT).show();
                } else {
                    binding.switchNotifications.setChecked(false);
                    ViolationNotificationHelper.setPushEnabled(requireContext(), false);
                    Toast.makeText(requireContext(),
                            "Cần cấp quyền Thông báo để nhận cảnh báo đẩy", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StudentViewModel.class);

        StudentNavigation.bind(this, binding.bottomNav, R.id.studentProfileFragment);

        viewModel.getProfile().observe(getViewLifecycleOwner(), this::bindProfileData);
        viewModel.getVehicles().observe(getViewLifecycleOwner(), this::bindVehiclesData);

        View.OnClickListener openEditProfile = v -> {
            Bundle args = new Bundle();
            args.putString(StudentEditProfileFragment.ARG_ROLE, "STUDENT");
            NavHostFragment.findNavController(this).navigate(R.id.studentEditProfileFragment, args);
        };
        binding.cardStudentProfile.setOnClickListener(openEditProfile);
        binding.btnEditStudentProfile.setOnClickListener(openEditProfile);

        binding.btnAddVehicle.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.studentAddVehicleFragment));

        binding.cardVehicle.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.studentAddVehicleFragment));

        binding.rowChangePassword.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.changePasswordFragment));

        binding.btnLogoutCard.setOnClickListener(v ->
                LogoutConfirmationDialog.show(this));

        setupSecuritySwitches();

        viewModel.loadProfile(requireContext());
        viewModel.loadVehicles(requireContext());
    }

    private void setupSecuritySwitches() {
        binding.switchBiometric.setChecked(BiometricAuthHelper.isEnabled(requireContext()));
        binding.switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            if (isChecked) {
                if (!BiometricAuthHelper.canAuthenticate(requireContext())) {
                    buttonView.setChecked(false);
                    Toast.makeText(requireContext(),
                            BiometricAuthHelper.unavailableReason(requireContext()),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                BiometricAuthHelper.setEnabled(requireContext(), true);
                Toast.makeText(requireContext(),
                        BiometricAuthHelper.hasSavedCredentials(requireContext())
                                ? "Đã bật đăng nhập vân tay / Face ID"
                                : "Đã bật. Đăng nhập lại 1 lần để lưu khóa sinh trắc học.",
                        Toast.LENGTH_SHORT).show();
            } else {
                BiometricAuthHelper.setEnabled(requireContext(), false);
                Toast.makeText(requireContext(),
                        "Đã tắt đăng nhập vân tay / Face ID", Toast.LENGTH_SHORT).show();
            }
        });

        boolean pushOn = ViolationNotificationHelper.isPushEnabled(requireContext())
                && ViolationNotificationHelper.hasNotificationPermission(requireContext());
        binding.switchNotifications.setChecked(pushOn);
        binding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        && !ViolationNotificationHelper.hasNotificationPermission(requireContext())) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    return;
                }
                ViolationNotificationHelper.setPushEnabled(requireContext(), true);
                Toast.makeText(requireContext(),
                        "Đã bật thông báo đẩy vi phạm", Toast.LENGTH_SHORT).show();
            } else {
                ViolationNotificationHelper.setPushEnabled(requireContext(), false);
                Toast.makeText(requireContext(),
                        "Đã tắt thông báo đẩy", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindProfileData(UserProfile profile) {
        if (binding == null || profile == null) return;
        String name = profile.getFullName() != null && !profile.getFullName().isEmpty()
                ? profile.getFullName() : "Sinh viên";
        binding.tvStudentName.setText(name);
        binding.tvAvatarInitials.setText(extractInitials(name));

        AvatarLoader.loadCircular(
                getActivity(), imageIo, profile.getAvatarUrl(),
                binding.ivStudentAvatar, binding.tvAvatarInitials);

        String code = profile.getStudentCode() != null ? profile.getStudentCode().trim() : "";
        String clazz = profile.getStudentClass() != null ? profile.getStudentClass().trim() : "";
        String email = profile.getEmailCanonical() != null ? profile.getEmailCanonical().trim() : "";

        if (!code.isEmpty() || !clazz.isEmpty()) {
            binding.tvStudentCodeClass.setText(code + (!code.isEmpty() && !clazz.isEmpty() ? " • " : "") + clazz);
        } else if (!email.isEmpty()) {
            binding.tvStudentCodeClass.setText(email);
        } else {
            binding.tvStudentCodeClass.setText("Sinh viên");
        }
        binding.tvStudentPhone.setVisibility(View.GONE);
    }

    private void bindVehiclesData(List<VehicleItem> vehicles) {
        if (binding == null || getContext() == null) return;
        if (vehicles == null || vehicles.isEmpty()) {
            binding.tvPlateNumber.setText("Chưa đăng ký xe");
            binding.tvVehicleModel.setText("Nhấn + Thêm xe để đăng ký phương tiện");
            binding.tvVehicleStatusBadge.setText("Chưa có");
            binding.tvVehicleStatusBadge.setBackgroundResource(R.drawable.bg_segmented_inactive);
            binding.tvVehicleStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            return;
        }

        VehicleItem mainVehicle = vehicles.get(0);
        binding.tvPlateNumber.setText(mainVehicle.plateNumber);
        String details = mainVehicle.model;
        if (!mainVehicle.color.isEmpty()) {
            details = details.isEmpty() ? mainVehicle.color : details + " • " + mainVehicle.color;
        }
        if (vehicles.size() > 1) {
            details = details + " (+" + (vehicles.size() - 1) + " xe khác)";
        }
        binding.tvVehicleModel.setText(details);

        if (mainVehicle.isApproved()) {
            binding.tvVehicleStatusBadge.setText(R.string.student_profile_status_approved);
            binding.tvVehicleStatusBadge.setBackgroundResource(R.drawable.bg_badge_processed);
            binding.tvVehicleStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_emerald));
        } else if (mainVehicle.isPending()) {
            binding.tvVehicleStatusBadge.setText("Chờ duyệt");
            binding.tvVehicleStatusBadge.setBackgroundResource(R.drawable.bg_badge_warning_amber);
            binding.tvVehicleStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.warn_yellow));
        } else {
            binding.tvVehicleStatusBadge.setText("Từ chối");
            binding.tvVehicleStatusBadge.setBackgroundResource(R.drawable.bg_badge_unprocessed);
            binding.tvVehicleStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.violation_red));
        }
    }

    private String extractInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "SV";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null && getContext() != null) {
            viewModel.loadProfile(requireContext());
            viewModel.loadVehicles(requireContext());
        }
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
