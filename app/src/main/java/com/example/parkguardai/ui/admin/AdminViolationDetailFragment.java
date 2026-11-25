package com.example.parkguardai.ui.admin;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.data.datasource.CloudinaryImageDataSource;
import com.example.parkguardai.data.model.ViolationItem;
import com.example.parkguardai.data.repository.AdminRepository;
import com.example.parkguardai.databinding.FragmentAdminViolationDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Màn hình Chi tiết biên bản vi phạm trong Cổng Quản trị viên (Admin Portal).
 * Tải trực tiếp từ Firestore và thực thi xác nhận nộp phạt / hủy biên bản thật trên DB.
 */
public class AdminViolationDetailFragment extends Fragment {
    public static final String ARG_VIOLATION_ID = "arg_violation_id";

    private FragmentAdminViolationDetailBinding binding;
    private AdminRepository adminRepository;
    private final ExecutorService imageIo = Executors.newSingleThreadExecutor();
    private final CloudinaryImageDataSource imageLoader = new CloudinaryImageDataSource();
    private String currentViolationId = "";
    private ViolationItem currentViolation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminViolationDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adminRepository = AdminRepository.getInstance(requireContext());

        if (getArguments() != null && getArguments().containsKey(ARG_VIOLATION_ID)) {
            currentViolationId = getArguments().getString(ARG_VIOLATION_ID, "");
        }

        binding.backButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigateUp());

        if (TextUtils.isEmpty(currentViolationId)) {
            binding.textViolationDetailInfo.setText("Không xác định được mã biên bản vi phạm.");
            binding.confirmPaidButton.setEnabled(false);
            binding.cancelViolationButton.setEnabled(false);
            return;
        }

        loadViolationDetails();

        binding.confirmPaidButton.setOnClickListener(v -> {
            binding.confirmPaidButton.setEnabled(false);
            adminRepository.confirmViolationPayment(currentViolationId)
                    .addOnSuccessListener(success -> {
                        if (!isAdded() || binding == null) return;
                        Snackbar.make(binding.getRoot(),
                                "Đã xác nhận hoàn tất xử phạt cho biên bản " + currentViolationId,
                                Snackbar.LENGTH_LONG).show();
                        v.postDelayed(() -> {
                            if (isAdded()) NavHostFragment.findNavController(this).navigateUp();
                        }, 600);
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded() || binding == null) return;
                        binding.confirmPaidButton.setEnabled(true);
                        Snackbar.make(binding.getRoot(),
                                "Không thể cập nhật trạng thái: " + e.getMessage(),
                                Snackbar.LENGTH_LONG).show();
                    });
        });

        binding.cancelViolationButton.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Hủy biên bản vi phạm")
                    .setMessage("Bạn có chắc chắn muốn hủy biên bản " + currentViolationId + " do có giải trình hoặc sai sót?")
                    .setPositiveButton("Xác nhận hủy", (dialog, which) -> {
                        binding.cancelViolationButton.setEnabled(false);
                        adminRepository.cancelViolation(currentViolationId, "Giải trình hợp lệ hoặc sai sót kỹ thuật")
                                .addOnSuccessListener(success -> {
                                    if (!isAdded() || binding == null) return;
                                    Snackbar.make(binding.getRoot(),
                                            "Đã hủy bỏ biên bản vi phạm " + currentViolationId,
                                            Snackbar.LENGTH_SHORT).show();
                                    binding.getRoot().postDelayed(() -> {
                                        if (isAdded()) NavHostFragment.findNavController(this).navigateUp();
                                    }, 600);
                                })
                                .addOnFailureListener(e -> {
                                    if (!isAdded() || binding == null) return;
                                    binding.cancelViolationButton.setEnabled(true);
                                    Snackbar.make(binding.getRoot(),
                                            "Không thể hủy biên bản: " + e.getMessage(),
                                            Snackbar.LENGTH_LONG).show();
                                });
                    })
                    .setNegativeButton("Quay lại", null)
                    .show();
        });
    }

    private void loadViolationDetails() {
        adminRepository.getViolationById(currentViolationId)
                .addOnSuccessListener(v -> {
                    if (binding == null) return;
                    currentViolation = v;
                    renderViolation(v);
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    binding.textViolationDetailInfo.setText("Không tải được dữ liệu biên bản: " + e.getMessage());
                    binding.confirmPaidButton.setEnabled(false);
                    binding.cancelViolationButton.setEnabled(false);
                });
    }

    private void renderViolation(ViolationItem v) {
        if (binding == null || getContext() == null) return;
        binding.textViolationPlate.setText(v.plateNumber);

        if (v.isResolved()) {
            binding.textViolationStatus.setText("ĐÃ XỬ LÝ");
            binding.textViolationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green));
            binding.confirmPaidButton.setEnabled(false);
            binding.confirmPaidButton.setText("ĐÃ HOÀN TẤT XỬ PHẠT");
        } else if (v.isCancelled()) {
            binding.textViolationStatus.setText("ĐÃ HỦY BỎ");
            binding.textViolationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            binding.confirmPaidButton.setEnabled(false);
            binding.cancelViolationButton.setEnabled(false);
        } else if (v.isAppealed()) {
            binding.textViolationStatus.setText("ĐANG KHIẾU NẠI");
            binding.textViolationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.warn_yellow));
        } else {
            binding.textViolationStatus.setText("CHƯA XỬ LÝ");
            binding.textViolationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red));
        }

        String gpsText;
        if (v.latitude != null && v.longitude != null) {
            String acc = v.locationAccuracy != null ? String.format(Locale.US, " (sai số %.0fm)", v.locationAccuracy) : "";
            gpsText = String.format(Locale.US, "%.5f, %.5f%s", v.latitude, v.longitude, acc);
        } else {
            gpsText = "Không có dữ liệu tọa độ";
        }

        StringBuilder info = new StringBuilder();
        info.append("• Mã biên bản: ").append(v.id).append("\n");
        info.append("• Lỗi vi phạm: ").append(v.violationName).append("\n");
        info.append("• Thời điểm: ").append(!v.timestamp.isEmpty() ? v.timestamp : "Chưa rõ").append("\n");
        info.append("• Địa điểm: ").append(v.location).append("\n");
        info.append("• Tọa độ GPS: ").append(gpsText).append("\n");
        info.append("• Bảo vệ lập: ").append(v.guardName).append("\n");
        if (!v.penalty.isEmpty()) {
            info.append("• Mức chế tài: ").append(v.penalty).append("\n");
        }
        if (!v.note.isEmpty()) {
            info.append("• Ghi chú: ").append(v.note).append("\n");
        }
        if (!v.complaintReason.isEmpty()) {
            info.append("• Nội dung khiếu nại: ").append(v.complaintReason);
        }
        binding.textViolationDetailInfo.setText(info.toString().trim());

        if (!TextUtils.isEmpty(v.primaryEvidenceUrl)) {
            binding.textEvidenceOverlay.setText("Biển số xác nhận: " + v.plateNumber);
            String url = v.primaryEvidenceUrl;
            imageIo.execute(() -> {
                try {
                    Bitmap bmp = imageLoader.loadPreview(url);
                    Activity act = getActivity();
                    if (act != null) {
                        act.runOnUiThread(() -> {
                            if (binding != null) {
                                binding.imgViolationEvidence.setImageBitmap(bmp);
                            }
                        });
                    }
                } catch (Exception ignored) { }
            });
        } else {
            binding.textEvidenceOverlay.setText("Chưa có ảnh hiện trường cho biên bản " + v.id);
        }
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
