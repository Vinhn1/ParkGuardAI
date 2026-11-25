package com.example.parkguardai.ui.student;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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

import com.example.parkguardai.data.model.UserProfile;
import com.example.parkguardai.data.repository.UserProfileRepository;
import com.example.parkguardai.databinding.FragmentStudentEditProfileBinding;
import com.example.parkguardai.ui.common.AvatarLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Màn hình Chỉnh sửa hồ sơ cá nhân dùng chung cho cả 3 vai trò (Sinh viên, Bảo vệ, Quản trị viên):
 * hỗ trợ tải ảnh đại diện lên Cloudinary, sửa họ tên, số điện thoại, lớp/khoa trực tiếp lên Firestore.
 */
public class StudentEditProfileFragment extends Fragment {
    public static final String ARG_ROLE = "arg_role";

    private FragmentStudentEditProfileBinding binding;
    private UserProfileRepository profileRepository;
    private final ExecutorService imageIo = Executors.newSingleThreadExecutor();
    private String role = "STUDENT";
    private File selectedAvatarFile;
    private boolean studentCodeInitiallyEmpty = false;

    private final ActivityResultLauncher<String> pickAvatarLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && getContext() != null && binding != null) {
                    File copied = copyUriToCache(uri, "avatar_" + System.currentTimeMillis() + ".jpg");
                    if (copied != null) {
                        selectedAvatarFile = copied;
                        Bitmap raw = BitmapFactory.decodeFile(copied.getAbsolutePath());
                        if (raw != null) {
                            binding.ivEditAvatar.setImageBitmap(AvatarLoader.toCircleBitmap(raw));
                            binding.ivEditAvatar.setVisibility(View.VISIBLE);
                            binding.tvEditAvatarInitials.setVisibility(View.GONE);
                        }
                        binding.tvAvatarStatus.setText("Đã chọn ảnh mới (Nhấn Lưu để tải lên)");
                    } else {
                        Toast.makeText(requireContext(), "Không thể đọc tệp ảnh đã chọn", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profileRepository = UserProfileRepository.getInstance();

        if (getArguments() != null) {
            role = getArguments().getString(ARG_ROLE, "STUDENT");
        }

        configureFieldsForRole();

        binding.backButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigateUp());

        binding.avatarPickerContainer.setOnClickListener(v -> pickAvatarLauncher.launch("image/*"));
        binding.btnPickAvatar.setOnClickListener(v -> pickAvatarLauncher.launch("image/*"));

        binding.saveButton.setOnClickListener(v -> handleSave());

        loadProfile();
    }

    private void configureFieldsForRole() {
        if ("ADMIN".equalsIgnoreCase(role)) {
            binding.tvEditProfileTitle.setText("Hồ sơ Quản trị viên");
            binding.studentCodeLayout.setVisibility(View.GONE);
            binding.facultyLayout.setVisibility(View.GONE);
        } else if ("GUARD".equalsIgnoreCase(role) || "SECURITY_GUARD".equalsIgnoreCase(role)) {
            binding.tvEditProfileTitle.setText("Hồ sơ Nhân viên bảo vệ");
            binding.studentCodeLayout.setHint("Mã nhân viên bảo vệ (Cố định)");
            binding.studentCodeLayout.setEnabled(false);
            binding.facultyLayout.setVisibility(View.GONE);
        } else {
            binding.tvEditProfileTitle.setText("Chỉnh sửa hồ sơ sinh viên");
            binding.studentCodeLayout.setVisibility(View.VISIBLE);
            binding.facultyLayout.setVisibility(View.VISIBLE);
        }
    }

    private void loadProfile() {
        binding.saveButton.setEnabled(false);
        profileRepository.getMyProfile().addOnSuccessListener(profile -> {
            if (binding == null || !isAdded()) return;
            binding.saveButton.setEnabled(true);
            bindProfile(profile);
        }).addOnFailureListener(e -> {
            if (binding == null || !isAdded()) return;
            binding.saveButton.setEnabled(true);
            Toast.makeText(requireContext(), "Không tải được hồ sơ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void bindProfile(UserProfile profile) {
        String name = profile.getFullName() != null ? profile.getFullName() : "";
        binding.nameInput.setText(name);
        binding.tvEditAvatarInitials.setText(extractInitials(name));

        if (profile.getPhoneCanonical() != null) {
            binding.phoneInput.setText(profile.getPhoneCanonical());
        }
        if (profile.getEmailCanonical() != null) {
            binding.emailInput.setText(profile.getEmailCanonical());
        }

        if ("GUARD".equalsIgnoreCase(role) || "SECURITY_GUARD".equalsIgnoreCase(role)) {
            String staffCode = profile.getStaffCode();
            if (!TextUtils.isEmpty(staffCode)) {
                binding.studentCodeInput.setText(staffCode);
                binding.studentCodeLayout.setVisibility(View.VISIBLE);
            } else {
                binding.studentCodeLayout.setVisibility(View.GONE);
            }
        } else if (!"ADMIN".equalsIgnoreCase(role)) {
            String code = profile.getStudentCode() != null ? profile.getStudentCode() : "";
            binding.studentCodeInput.setText(code);
            studentCodeInitiallyEmpty = code.trim().isEmpty();
            // Cho phép sinh viên điền bổ sung MSSV nếu trước đây bị trống; đã có rồi thì giữ cố định
            binding.studentCodeLayout.setEnabled(studentCodeInitiallyEmpty);
            binding.studentCodeLayout.setHint(studentCodeInitiallyEmpty
                    ? "Mã số sinh viên (MSSV)" : "Mã số sinh viên (Cố định)");

            if (profile.getStudentClass() != null) {
                binding.facultyInput.setText(profile.getStudentClass());
            }
        }

        AvatarLoader.loadCircular(
                getActivity(), imageIo, profile.getAvatarUrl(),
                binding.ivEditAvatar, binding.tvEditAvatarInitials);
    }

    private void handleSave() {
        String name = binding.nameInput.getText() != null
                ? binding.nameInput.getText().toString().trim() : "";
        String phone = binding.phoneInput.getText() != null
                ? binding.phoneInput.getText().toString().replaceAll("[\\s\\-.]", "").trim() : "";
        String studentClass = ("STUDENT".equalsIgnoreCase(role) && binding.facultyInput.getText() != null)
                ? binding.facultyInput.getText().toString().trim() : null;
        String studentCode = ("STUDENT".equalsIgnoreCase(role) && studentCodeInitiallyEmpty
                && binding.studentCodeInput.getText() != null)
                ? binding.studentCodeInput.getText().toString().trim() : null;

        if (name.length() < 2) {
            binding.nameLayout.setError("Vui lòng nhập họ và tên hợp lệ (tối thiểu 2 ký tự)");
            return;
        }
        binding.nameLayout.setError(null);

        if (!phone.isEmpty() && !phone.matches("^[+]?[0-9]{9,15}$")) {
            binding.phoneLayout.setError("Số điện thoại không hợp lệ (9-15 chữ số)");
            return;
        }
        binding.phoneLayout.setError(null);

        binding.saveButton.setEnabled(false);
        binding.saveButton.setText(selectedAvatarFile != null ? "Đang tải ảnh & lưu..." : "Đang lưu...");

        profileRepository.updateMyProfile(name, phone, studentClass, studentCode, selectedAvatarFile)
                .addOnSuccessListener(updated -> {
                    if (!isAdded() || binding == null) return;
                    Toast.makeText(requireContext(), "Đã cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(this).navigateUp();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    binding.saveButton.setEnabled(true);
                    binding.saveButton.setText(com.example.parkguardai.R.string.student_save);
                    Toast.makeText(requireContext(),
                            "Không thể lưu hồ sơ: " + (e.getMessage() != null ? e.getMessage() : "Lỗi hệ thống"),
                            Toast.LENGTH_LONG).show();
                });
    }

    @Nullable
    private File copyUriToCache(@NonNull Uri uri, @NonNull String fileName) {
        try {
            File outFile = new File(requireContext().getCacheDir(), fileName);
            try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(outFile)) {
                if (in == null) return null;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            return outFile;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            if ("ADMIN".equalsIgnoreCase(role)) return "AD";
            if ("GUARD".equalsIgnoreCase(role)) return "BV";
            return "SV";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
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
