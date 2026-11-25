package com.example.parkguardai.ui.student;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.data.model.StudentDashboardData;
import com.example.parkguardai.data.model.StudentParkingRecord;
import com.example.parkguardai.databinding.FragmentStudentHomeBinding;
import com.example.parkguardai.ui.common.ViolationNotificationHelper;

import java.util.List;

/**
 * Màn hình Xe của tôi dành cho Sinh viên (Tab 1 Bottom Navigation).
 * Hiển thị trạng thái phương tiện, xe trong bãi, cảnh báo vi phạm và lịch sử gần đây từ Cloud Firestore.
 */
public final class StudentHomeFragment extends Fragment {
    private FragmentStudentHomeBinding binding;
    private StudentViewModel viewModel;
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (isAdded() && granted) {
                    ViolationNotificationHelper.startRealtimeListener(requireContext());
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StudentViewModel.class);

        StudentNavigation.bind(this, binding.studentBottomNav, R.id.studentHomeFragment);
        viewModel.getDashboardData().observe(getViewLifecycleOwner(), this::bindDashboardData);

        View.OnClickListener openAddVehicle = v ->
                NavHostFragment.findNavController(this).navigate(R.id.studentAddVehicleFragment);
        binding.btnRegisterNewVehicle.setOnClickListener(openAddVehicle);
        binding.quickActionAddVehicle.setOnClickListener(openAddVehicle);

        View.OnClickListener openViolations = v ->
                NavHostFragment.findNavController(this).navigate(R.id.studentViolationsFragment);
        binding.cardStatusViolations.setOnClickListener(openViolations);
        binding.quickActionViolations.setOnClickListener(openViolations);

        View.OnClickListener openHistory = v ->
                NavHostFragment.findNavController(this).navigate(R.id.studentParkingHistoryFragment);
        binding.cardRecentHistory.setOnClickListener(openHistory);
        binding.quickActionHistory.setOnClickListener(openHistory);

        binding.quickActionNotifications.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.studentNotificationsFragment));

        if (ViolationNotificationHelper.isPushEnabled(requireContext())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && !ViolationNotificationHelper.hasNotificationPermission(requireContext())) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                ViolationNotificationHelper.startRealtimeListener(requireContext());
            }
        }

        viewModel.loadDashboard(requireContext());
    }

    private void bindDashboardData(StudentDashboardData data) {
        if (binding == null || data == null || getContext() == null) return;

        binding.tvStudentName.setText(
                data.getStudentName() != null && !data.getStudentName().isEmpty()
                        ? data.getStudentName() : "Sinh viên");

        binding.tvMainPlateNumber.setText(
                data.getMainPlate() != null && !data.getMainPlate().isEmpty()
                        ? data.getMainPlate() : "Chưa đăng ký xe");

        binding.tvVehicleDetails.setText(
                data.getVehicleDetails() != null && !data.getVehicleDetails().isEmpty()
                        ? data.getVehicleDetails() : "");

        String vStatus = data.getVehicleStatus() != null ? data.getVehicleStatus() : "NONE";
        if ("APPROVED".equalsIgnoreCase(vStatus) || "VERIFIED".equalsIgnoreCase(vStatus)) {
            binding.tvVehicleStatusBadge.setText("Đã duyệt");
            binding.tvVehicleStatusBadge.setBackgroundResource(R.drawable.bg_badge_processed);
            binding.tvVehicleStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_success));
        } else if ("PENDING".equalsIgnoreCase(vStatus)) {
            binding.tvVehicleStatusBadge.setText("Chờ duyệt");
            binding.tvVehicleStatusBadge.setBackgroundResource(R.drawable.bg_badge_warning_amber);
            binding.tvVehicleStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.warn_yellow));
        } else if ("REJECTED".equalsIgnoreCase(vStatus) || "BLOCKED".equalsIgnoreCase(vStatus)) {
            binding.tvVehicleStatusBadge.setText("Từ chối");
            binding.tvVehicleStatusBadge.setBackgroundResource(R.drawable.bg_badge_unprocessed);
            binding.tvVehicleStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.violation_red));
        } else {
            binding.tvVehicleStatusBadge.setText("Chưa có xe");
            binding.tvVehicleStatusBadge.setBackgroundResource(R.drawable.bg_segmented_inactive);
            binding.tvVehicleStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        }

        // Trạng thái trong bãi
        if (data.isInLot()) {
            binding.tvInLotStatus.setText(R.string.student_status_in_lot);
            binding.tvInLotTime.setText(data.getInLotTime() != null ? data.getInLotTime() : "");
            binding.tvInLotTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_success));
            binding.cardStatusInLot.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.pg_success));
        } else {
            binding.tvInLotStatus.setText("Ngoài bãi");
            binding.tvInLotTime.setText(data.getInLotTime() != null ? data.getInLotTime() : "Chưa ghi nhận lượt gửi");
            binding.tvInLotTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            binding.cardStatusInLot.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.outline));
        }

        // Thẻ số vi phạm
        int vioCount = data.getUnprocessedViolationsCount();
        binding.tvViolationsCount.setText(vioCount + " vi phạm");
        if (vioCount > 0) {
            binding.tvViolationsSubtitle.setText(R.string.student_status_unprocessed);
            binding.tvViolationsSubtitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.violation_red));
        } else {
            binding.tvViolationsSubtitle.setText("Không có vi phạm tồn đọng");
            binding.tvViolationsSubtitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_success));
        }

        // Render timeline lịch sử vào/ra thật
        renderRecentHistory(data.getRecentHistory());
    }

    private void renderRecentHistory(List<StudentParkingRecord> records) {
        if (binding == null || getContext() == null) return;
        binding.recentHistoryContainer.removeAllViews();

        if (records == null || records.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("Chưa có lượt xe vào/ra nào được ghi nhận.");
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            empty.setTextSize(13f);
            empty.setPadding(0, 12, 0, 8);
            binding.recentHistoryContainer.addView(empty);
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < records.size(); i++) {
            StudentParkingRecord rec = records.get(i);
            boolean isEntry = "ENTRY".equalsIgnoreCase(rec.getActionType());

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            View dot = new View(requireContext());
            int dotSize = Math.round(12 * density);
            dot.setLayoutParams(new LinearLayout.LayoutParams(dotSize, dotSize));
            dot.setBackgroundResource(i == 0 ? R.drawable.bg_stepper_circle_active : R.drawable.bg_stepper_circle_inactive);
            row.addView(dot);

            LinearLayout info = new LinearLayout(requireContext());
            info.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            infoLp.setMarginStart(Math.round(12 * density));
            info.setLayoutParams(infoLp);

            TextView tvTitle = new TextView(requireContext());
            tvTitle.setText(rec.getDateTitle() + (isEntry ? " • Xe vào bãi" : " • Xe rời bãi"));
            tvTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvTitle.setTextSize(14f);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            info.addView(tvTitle);

            TextView tvSub = new TextView(requireContext());
            tvSub.setText(rec.getLocationName() + (isEntry ? " • Đang gửi xe" : " • Phiên đỗ hoàn tất"));
            tvSub.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            tvSub.setTextSize(12f);
            info.addView(tvSub);

            row.addView(info);

            TextView tvTime = new TextView(requireContext());
            tvTime.setText(rec.getTimeFormatted());
            tvTime.setTextSize(12f);
            tvTime.setTypeface(null, android.graphics.Typeface.BOLD);
            if (i == 0) {
                tvTime.setBackgroundResource(R.drawable.bg_badge_kpi_cyan);
                tvTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_primary_orange_dark));
                tvTime.setPadding(Math.round(10 * density), Math.round(3 * density),
                        Math.round(10 * density), Math.round(3 * density));
            } else {
                tvTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            }
            row.addView(tvTime);

            binding.recentHistoryContainer.addView(row);

            if (i < records.size() - 1) {
                View connector = new View(requireContext());
                LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(
                        Math.round(2 * density), Math.round(14 * density));
                lineLp.setMarginStart(Math.round(5 * density));
                connector.setLayoutParams(lineLp);
                connector.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pg_primary_orange_soft));
                binding.recentHistoryContainer.addView(connector);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null && getContext() != null) {
            viewModel.loadDashboard(requireContext());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
