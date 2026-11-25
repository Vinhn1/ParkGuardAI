package com.example.parkguardai.ui.admin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.databinding.FragmentAdminCreateGuardBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.FirebaseFirestoreException;

/**
 * Màn hình Tạo tài khoản cho nhân viên bảo vệ mới (trong cổng Quản trị viên).
 * Hỗ trợ nhập thông tin định danh, email nội bộ, mật khẩu khởi tạo, ca trực và khu vực kiểm soát.
 */
public class AdminCreateGuardFragment extends Fragment {
    private FragmentAdminCreateGuardBinding binding;
    private int selectedShiftIndex = 0; // 0: Sáng, 1: Chiều, 2: Đêm
    private int selectedScopeIndex = 0; // 0: Cổng chính, 1: Cổng phụ, 2: Bãi xe SV A, 3: Bãi xe cán bộ B

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminCreateGuardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Nút quay lại
        binding.btnBack.setOnClickListener(v -> {
            if (!NavHostFragment.findNavController(this).navigateUp()) {
                requireActivity().onBackPressed();
            }
        });

        // Xử lý chọn Ca trực
        binding.btnShiftMorning.setOnClickListener(v -> selectShift(0));
        binding.btnShiftAfternoon.setOnClickListener(v -> selectShift(1));
        binding.btnShiftNight.setOnClickListener(v -> selectShift(2));

        // Xử lý chọn Khu vực phụ trách
        binding.chipMainGate.setOnClickListener(v -> selectScope(0));
        binding.chipSubGate.setOnClickListener(v -> selectScope(1));
        binding.chipLotA.setOnClickListener(v -> selectScope(2));
        binding.chipLotB.setOnClickListener(v -> selectScope(3));

        // Xử lý nút Tạo tài khoản
        binding.btnSubmitCreateGuard.setOnClickListener(v -> handleCreateGuard());

        // Tự động cuộn ô đang nhập lên trên bàn phím
        setupKeyboardAutoScroll(
                binding.etGuardName,
                binding.etGuardCode,
                binding.etGuardPhone,
                binding.etGuardEmail,
                binding.etGuardPassword
        );
    }

    private void setupKeyboardAutoScroll(View... fields) {
        for (View field : fields) {
            field.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && binding != null) {
                    binding.scrollViewCreateGuard.postDelayed(() -> {
                        if (binding == null) return;
                        int targetY = Math.max(0, v.getTop());
                        View parent = (View) v.getParent();
                        while (parent != null && parent != binding.scrollViewCreateGuard) {
                            targetY += parent.getTop();
                            parent = parent.getParent() instanceof View ? (View) parent.getParent() : null;
                        }
                        int offset = Math.round(96 * getResources().getDisplayMetrics().density);
                        binding.scrollViewCreateGuard.smoothScrollTo(0, Math.max(0, targetY - offset));
                    }, 180L);
                }
            });
        }
    }

    private void selectShift(int index) {
        selectedShiftIndex = index;
        int activeBg = R.drawable.bg_segmented_active;
        int inactiveBg = R.drawable.bg_segmented_inactive;
        int activeColor = ContextCompat.getColor(requireContext(), R.color.white);
        int inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_muted);

        binding.btnShiftMorning.setBackgroundResource(index == 0 ? activeBg : inactiveBg);
        binding.btnShiftMorning.setTextColor(index == 0 ? activeColor : inactiveColor);

        binding.btnShiftAfternoon.setBackgroundResource(index == 1 ? activeBg : inactiveBg);
        binding.btnShiftAfternoon.setTextColor(index == 1 ? activeColor : inactiveColor);

        binding.btnShiftNight.setBackgroundResource(index == 2 ? activeBg : inactiveBg);
        binding.btnShiftNight.setTextColor(index == 2 ? activeColor : inactiveColor);
    }

    private void selectScope(int index) {
        selectedScopeIndex = index;
        int activeBg = R.drawable.bg_badge_processed;
        int inactiveBg = R.drawable.bg_card_surface;
        int activeColor = ContextCompat.getColor(requireContext(), R.color.success_green);
        int inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_muted);

        binding.chipMainGate.setBackgroundResource(index == 0 ? activeBg : inactiveBg);
        binding.chipMainGate.setTextColor(index == 0 ? activeColor : inactiveColor);

        binding.chipSubGate.setBackgroundResource(index == 1 ? activeBg : inactiveBg);
        binding.chipSubGate.setTextColor(index == 1 ? activeColor : inactiveColor);

        binding.chipLotA.setBackgroundResource(index == 2 ? activeBg : inactiveBg);
        binding.chipLotA.setTextColor(index == 2 ? activeColor : inactiveColor);

        binding.chipLotB.setBackgroundResource(index == 3 ? activeBg : inactiveBg);
        binding.chipLotB.setTextColor(index == 3 ? activeColor : inactiveColor);
    }

    private void handleCreateGuard() {
        String name = binding.etGuardName.getText() != null ? binding.etGuardName.getText().toString().trim() : "";
        String code = binding.etGuardCode.getText() != null ? binding.etGuardCode.getText().toString().trim() : "";
        String phone = binding.etGuardPhone.getText() != null ? binding.etGuardPhone.getText().toString().trim() : "";
        String email = binding.etGuardEmail.getText() != null ? binding.etGuardEmail.getText().toString().trim() : "";
        String password = binding.etGuardPassword.getText() != null ? binding.etGuardPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            binding.etGuardName.setError("Vui lòng nhập họ và tên bảo vệ");
            binding.etGuardName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(code)) {
            binding.etGuardCode.setError("Vui lòng nhập mã nhân viên");
            binding.etGuardCode.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            binding.etGuardEmail.setError("Vui lòng nhập email cấp cho bảo vệ");
            binding.etGuardEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.etGuardPassword.setError("Mật khẩu tạm thời tối thiểu 6 ký tự");
            binding.etGuardPassword.requestFocus();
            return;
        }

        String shiftName;
        if (selectedShiftIndex == 0) {
            shiftName = "Ca sáng (06:00 - 14:00)";
        } else if (selectedShiftIndex == 1) {
            shiftName = "Ca chiều (14:00 - 22:00)";
        } else {
            shiftName = "Ca đêm (22:00 - 06:00)";
        }

        String scopeName;
        if (selectedScopeIndex == 0) {
            scopeName = "Cổng chính A";
        } else if (selectedScopeIndex == 1) {
            scopeName = "Cổng phụ B";
        } else if (selectedScopeIndex == 2) {
            scopeName = "Bãi xe sinh viên A";
        } else {
            scopeName = "Bãi xe cán bộ B";
        }

        // Ghi nhận tài khoản bảo vệ mới vào Firebase Auth & Cloud Firestore
        binding.btnSubmitCreateGuard.setEnabled(false);
        com.example.parkguardai.data.repository.AdminRepository.getInstance(requireContext())
                .createGuardAccount(name, code, phone, email, password, shiftName, scopeName)
                .addOnSuccessListener(newGuard -> {
                    if (!isAdded() || binding == null) return;
                    binding.btnSubmitCreateGuard.setEnabled(true);

                    // Tạo dialog xác nhận cấp tài khoản thành công kèm thông tin bàn giao
                    String credentialSummary = "Họ tên: " + name + "\n"
                            + "Mã NV: " + code + "\n"
                            + "Tài khoản: " + email + "\n"
                            + "Mật khẩu: " + password + "\n"
                            + "Phân ca: " + shiftName + "\n"
                            + "Khu vực: " + scopeName;

                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Cấp tài khoản thành công")
                            .setMessage("Tài khoản bảo vệ đã được tạo trên Firebase Auth và gửi email xác minh tới " + email + ":\n\n" + credentialSummary)
                            .setPositiveButton("Bàn giao & Quay lại", (dialog, which) -> {
                                Toast.makeText(requireContext(), R.string.admin_guard_create_success, Toast.LENGTH_SHORT).show();
                                NavHostFragment.findNavController(this).navigateUp();
                            })
                            .setNeutralButton("Sao chép thông tin", (dialog, which) -> {
                                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                                if (clipboard != null) {
                                    ClipData clip = ClipData.newPlainText("ParkGuard Guard Credentials", credentialSummary);
                                    clipboard.setPrimaryClip(clip);
                                    Toast.makeText(requireContext(), "Đã sao chép thông tin tài khoản bảo vệ!", Toast.LENGTH_SHORT).show();
                                }
                                NavHostFragment.findNavController(this).navigateUp();
                            })
                            .setCancelable(false)
                            .show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    binding.btnSubmitCreateGuard.setEnabled(true);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Không thể tạo tài khoản bảo vệ")
                            .setMessage(formatCreateGuardError(e))
                            .setPositiveButton("Đóng", null)
                            .show();
                });
    }

    private String formatCreateGuardError(@Nullable Exception e) {
        if (e == null) {
            return "Đã xảy ra lỗi không xác định khi tạo tài khoản bảo vệ.";
        }
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "Email này đã tồn tại trên Firebase Authentication.\n\nVui lòng nhập một địa chỉ email khác chưa từng đăng ký.";
        }
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "Mật khẩu chưa đủ mạnh (Firebase yêu cầu tối thiểu 6 ký tự). Vui lòng nhập mật khẩu mạnh hơn.";
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Địa chỉ email không đúng định dạng hợp lệ. Vui lòng kiểm tra lại.";
        }
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException ffe = (FirebaseFirestoreException) e;
            if (ffe.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "Cloud Firestore từ chối quyền ghi (PERMISSION_DENIED).\n\nNguyên nhân thường gặp:\n1. Chưa Publish file firestore.rules mới lên Firebase Console.\n2. Tài khoản Admin hiện tại chưa xác minh email hoặc chưa có accessGrants/{uid} với role = \"ADMIN\" và active = true.";
            }
            return "Lỗi Cloud Firestore (" + ffe.getCode().name() + "): " + ffe.getMessage();
        }
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
