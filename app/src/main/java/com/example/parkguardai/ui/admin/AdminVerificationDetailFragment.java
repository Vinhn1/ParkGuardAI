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
import com.example.parkguardai.data.model.VehicleVerificationItem;
import com.example.parkguardai.data.repository.AdminRepository;
import com.example.parkguardai.databinding.FragmentAdminVerificationDetailBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Màn hình Chi tiết hồ sơ xác minh phương tiện của sinh viên trong Cổng Quản trị viên.
 * Tải trực tiếp từ Cloud Firestore và duyệt/từ chối nguyên tử trên DB.
 */
public class AdminVerificationDetailFragment extends Fragment {
    public static final String ARG_REQUEST_ID = "arg_request_id";

    private FragmentAdminVerificationDetailBinding binding;
    private AdminRepository adminRepository;
    private final ExecutorService imageIo = Executors.newSingleThreadExecutor();
    private final CloudinaryImageDataSource imageLoader = new CloudinaryImageDataSource();
    private String currentRequestId = "";
    private VehicleVerificationItem currentRequest;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminVerificationDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adminRepository = AdminRepository.getInstance(requireContext());

        if (getArguments() != null && getArguments().containsKey(ARG_REQUEST_ID)) {
            currentRequestId = getArguments().getString(ARG_REQUEST_ID, "");
        }

        binding.backButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigateUp());

        if (TextUtils.isEmpty(currentRequestId)) {
            binding.textDetailInfo.setText("Không xác định được mã hồ sơ xác minh.");
            binding.approveButton.setEnabled(false);
            binding.rejectButton.setEnabled(false);
            return;
        }

        loadRequestDetails();

        binding.approveButton.setOnClickListener(v -> {
            binding.approveButton.setEnabled(false);
            adminRepository.reviewVehicleRequest(currentRequestId, true, null)
                    .addOnSuccessListener(success -> {
                        if (isAdded()) {
                            NavHostFragment.findNavController(this).navigate(R.id.adminVerificationSuccessFragment);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded() && binding != null) {
                            binding.approveButton.setEnabled(true);
                            Snackbar.make(binding.getRoot(),
                                    "Không thể phê duyệt: " + e.getMessage(),
                                    Snackbar.LENGTH_LONG).show();
                        }
                    });
        });

        binding.rejectButton.setOnClickListener(v -> {
            binding.rejectButton.setEnabled(false);
            adminRepository.reviewVehicleRequest(currentRequestId, false, "Thông tin chưa trùng khớp")
                    .addOnSuccessListener(success -> {
                        if (isAdded() && binding != null) {
                            Snackbar.make(binding.getRoot(),
                                    "Đã ghi nhận từ chối yêu cầu đăng ký xe "
                                            + (currentRequest != null ? currentRequest.plateNumber : currentRequestId),
                                    Snackbar.LENGTH_LONG).show();
                            binding.getRoot().postDelayed(() -> {
                                if (isAdded()) NavHostFragment.findNavController(this).navigateUp();
                            }, 600);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded() && binding != null) {
                            binding.rejectButton.setEnabled(true);
                            Snackbar.make(binding.getRoot(),
                                    "Không thể từ chối hồ sơ: " + e.getMessage(),
                                    Snackbar.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void loadRequestDetails() {
        adminRepository.getVerificationRequests(null, null).addOnSuccessListener(requests -> {
            if (binding == null) return;
            for (VehicleVerificationItem req : requests) {
                if (req.id.equals(currentRequestId)) {
                    currentRequest = req;
                    break;
                }
            }
            if (currentRequest == null) {
                binding.textDetailInfo.setText("Không tìm thấy hồ sơ xác minh " + currentRequestId);
                binding.approveButton.setEnabled(false);
                binding.rejectButton.setEnabled(false);
                return;
            }
            renderRequest(currentRequest);
        });
    }

    private void renderRequest(VehicleVerificationItem req) {
        if (binding == null || getContext() == null) return;
        binding.textDetailPlate.setText(req.plateNumber);
        if (req.isBlocked()) {
            binding.textDetailStatus.setText("BỊ TỪ CHỐI / KHÓA");
            binding.textDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red));
            binding.approveButton.setEnabled(true);
            binding.approveButton.setText("MỞ KHÓA & DUYỆT");
        } else if (req.isApproved()) {
            binding.textDetailStatus.setText("ĐÃ DUYỆT");
            binding.textDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green));
            binding.approveButton.setEnabled(false);
            binding.approveButton.setText("ĐÃ PHÊ DUYỆT");
        } else {
            binding.textDetailStatus.setText("CHỜ DUYỆT");
            binding.textDetailStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.warn_yellow));
            binding.approveButton.setEnabled(true);
            binding.approveButton.setText("PHÊ DUYỆT XE");
        }

        String detailText = "• Chủ sở hữu: " + req.studentName + "\n"
                + "• MSSV / Mã định danh: " + (!req.studentCode.isEmpty() ? req.studentCode : req.requesterUid) + "\n"
                + "• Dòng xe: " + req.vehicleModel + "\n"
                + "• Màu sơn: " + (!req.color.isEmpty() ? req.color : "Chưa cập nhật") + "\n"
                + "• Mã phương tiện: " + req.vehicleId + "\n"
                + "• Thời điểm gửi: " + (!req.submissionDate.isEmpty() ? req.submissionDate : "Chưa rõ")
                + (!req.reason.isEmpty() ? "\n• Lý do / Ghi chú: " + req.reason : "");
        binding.textDetailInfo.setText(detailText);

        if (!TextUtils.isEmpty(req.registrationCardUrl)) {
            binding.textDetailCardOverlay.setText("Ảnh cà vẹt xe • Biển số " + req.plateNumber);
            String url = req.registrationCardUrl;
            imageIo.execute(() -> {
                try {
                    Bitmap bmp = imageLoader.loadPreview(url);
                    Activity act = getActivity();
                    if (act != null) {
                        act.runOnUiThread(() -> {
                            if (binding != null) {
                                binding.imgRegistrationCard.setImageBitmap(bmp);
                            }
                        });
                    }
                } catch (Exception ignored) { }
            });
        } else {
            binding.textDetailCardOverlay.setText("Hồ sơ đăng ký tự khai (chưa đính kèm ảnh cà vẹt)\nBiển số " + req.plateNumber);
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
