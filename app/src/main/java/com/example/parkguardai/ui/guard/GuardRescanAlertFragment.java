package com.example.parkguardai.ui.guard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.data.model.DuplicateScanCheckResult;
import com.example.parkguardai.data.model.ScanTimelineItem;
import com.example.parkguardai.data.model.VehicleLookupResult;
import com.example.parkguardai.data.repository.GuardRepository;
import com.example.parkguardai.databinding.FragmentGuardRescanAlertBinding;
import com.example.parkguardai.databinding.ItemGuardRescanTimelineBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Màn hình Xe quét lại trong ngày dành cho Bảo vệ.
 * Hiển thị lịch sử quét/vi phạm thực tế từ Firestore, ghi nhận lượt ra thật và chuyển chụp ảnh vi phạm chuẩn GPS.
 */
public final class GuardRescanAlertFragment extends Fragment {
    public static final String ARG_PLATE = "plate";

    private FragmentGuardRescanAlertBinding binding;
    private GuardRepository repository;
    private String plate = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGuardRescanAlertBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = GuardRepository.getInstance(requireContext());

        if (getArguments() != null && getArguments().getString(ARG_PLATE) != null) {
            plate = getArguments().getString(ARG_PLATE);
        } else if (getArguments() != null && getArguments().getString("plate") != null) {
            plate = getArguments().getString("plate");
        }

        if (plate == null || plate.trim().isEmpty()) {
            plate = "CHƯA RÕ";
        }

        binding.tvRescanPlate.setText(plate);

        repository.lookupVehicle(plate).addOnCompleteListener(task -> {
            if (binding == null) return;
            if (task.isSuccessful() && task.getResult() != null) {
                VehicleLookupResult res = task.getResult();
                if (res.status == VehicleLookupResult.RegistrationStatus.VERIFIED
                        || res.status == VehicleLookupResult.RegistrationStatus.PENDING) {
                    binding.tvRescanOwner.setText(res.ownerName + " • " + res.vehicleModel);
                } else {
                    binding.tvRescanOwner.setText("Chưa đăng ký • Phương tiện lạ");
                }
            } else {
                binding.tvRescanOwner.setText("Chưa đăng ký • Phương tiện lạ");
            }
        });

        loadDuplicateScanData();

        GuardNavigation.bind(this, binding.guardBottomNav, R.id.guardHistoryFragment);

        binding.btnBack.setOnClickListener(v -> {
            if (!NavHostFragment.findNavController(this).popBackStack()) {
                requireActivity().onBackPressed();
            }
        });

        // Bổ sung lượt ra hợp lệ vào Cloud Firestore parkingHistory
        binding.btnSupplementExit.setOnClickListener(v -> {
            binding.btnSupplementExit.setEnabled(false);
            repository.recordParkingEvent(plate, "EXIT", "Cổng bãi xe")
                    .addOnSuccessListener(unused -> {
                        if (!isAdded()) return;
                        String timeNow = new SimpleDateFormat("HH:mm", new Locale("vi", "VN")).format(new Date());
                        Toast.makeText(requireContext(),
                                "Đã ghi nhận lượt ra cho xe " + plate + " lúc " + timeNow,
                                Toast.LENGTH_SHORT).show();
                        if (!NavHostFragment.findNavController(this).popBackStack()) {
                            requireActivity().onBackPressed();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        binding.btnSupplementExit.setEnabled(true);
                        Toast.makeText(requireContext(),
                                "Không thể lưu lượt ra: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        });

        // Ghi nhận xác minh lượt đọc trùng (không tạo vi phạm)
        binding.btnMergeScan.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Đã xác nhận gộp lượt đọc trùng của xe " + plate,
                    Toast.LENGTH_SHORT).show();
            if (!NavHostFragment.findNavController(this).popBackStack()) {
                requireActivity().onBackPressed();
            }
        });

        // Lập biên bản vi phạm -> Mở camera chụp ảnh hiện trường thật kèm GPS chuẩn DEC-008A
        binding.btnSuspiciousPlate.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Vui lòng chụp ảnh hiện trường kèm định vị GPS để lập biên bản vi phạm",
                    Toast.LENGTH_LONG).show();
            try {
                NavHostFragment.findNavController(this).navigate(R.id.guardCameraFragment);
            } catch (Exception ignored) { }
        });
    }

    private void loadDuplicateScanData() {
        if (binding == null) return;

        repository.checkDuplicateScan(plate).addOnCompleteListener(task -> {
            if (binding == null) return;
            if (!task.isSuccessful() || task.getResult() == null) {
                renderTimeline(null);
                return;
            }

            DuplicateScanCheckResult result = task.getResult();
            binding.tvRescanCount.setText(result.scanCountToday + " lần quét");

            if (result.isDuplicate && result.scanCountToday > 1) {
                binding.cardRescanAlertBanner.setVisibility(View.VISIBLE);
                binding.tvRescanAlertTitle.setText(result.scanCountToday + " lượt ghi nhận trong ngày");
                binding.tvRescanAlertDesc.setText("Phương tiện đã được ghi nhận " + result.scanCountToday +
                        " lần trong ngày hôm nay. Lần gần nhất lúc " +
                        (result.lastScanTime != null && !result.lastScanTime.isEmpty() ? result.lastScanTime : "trước đó") +
                        " tại " + (result.lastLocation != null ? result.lastLocation : "hiện trường") +
                        ". Vui lòng đối chiếu với lịch sử bên dưới.");
            } else {
                binding.cardRescanAlertBanner.setVisibility(View.GONE);
            }

            renderTimeline(result.todayScans);
        });
    }

    private void renderTimeline(@Nullable List<ScanTimelineItem> items) {
        if (binding == null) return;
        binding.layoutRescanTimeline.removeAllViews();

        if (items == null || items.isEmpty()) {
            binding.layoutRescanEmpty.setVisibility(View.VISIBLE);
            binding.layoutRescanTimeline.setVisibility(View.GONE);
            return;
        }

        binding.layoutRescanEmpty.setVisibility(View.GONE);
        binding.layoutRescanTimeline.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (ScanTimelineItem item : items) {
            ItemGuardRescanTimelineBinding itemBinding = ItemGuardRescanTimelineBinding.inflate(
                    inflater, binding.layoutRescanTimeline, false);

            itemBinding.tvTimelineTimeAction.setText(item.time + " • " + item.action);
            itemBinding.tvTimelineLocation.setText(item.gate != null && !item.gate.isEmpty() ? item.gate : "Hiện trường");
            itemBinding.tvTimelineBadge.setText(item.status);

            String status = item.status != null ? item.status : "";
            if (status.contains("Vi phạm")) {
                itemBinding.cardTimelineItem.setCardBackgroundColor(0xFFFFF1F2);
                itemBinding.cardTimelineItem.setStrokeColor(0xFFFCA5A5);
                itemBinding.viewTimelineIndicator.setBackgroundResource(R.drawable.bg_kpi_icon_red);
                itemBinding.tvTimelineBadge.setBackgroundResource(R.drawable.bg_badge_violation_pill);
                itemBinding.tvTimelineBadge.setTextColor(0xFFDC2626);
            } else if (status.contains("xác nhận") || status.contains("chú ý")) {
                itemBinding.cardTimelineItem.setCardBackgroundColor(0xFFFFFBEB);
                itemBinding.cardTimelineItem.setStrokeColor(0xFFFCD34D);
                itemBinding.viewTimelineIndicator.setBackgroundResource(R.drawable.bg_kpi_icon_orange);
                itemBinding.tvTimelineBadge.setBackgroundResource(R.drawable.bg_badge_warning_amber);
                itemBinding.tvTimelineBadge.setTextColor(0xFFD97706);
            } else {
                itemBinding.cardTimelineItem.setCardBackgroundColor(0xFFFFFFFF);
                itemBinding.cardTimelineItem.setStrokeColor(0xFFEAE0D5);
                itemBinding.viewTimelineIndicator.setBackgroundResource(R.drawable.bg_kpi_icon_green);
                itemBinding.tvTimelineBadge.setBackgroundResource(R.drawable.bg_badge_processed);
                itemBinding.tvTimelineBadge.setTextColor(0xFF16A34A);
            }

            binding.layoutRescanTimeline.addView(itemBinding.getRoot());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
