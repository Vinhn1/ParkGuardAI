package com.example.parkguardai.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.data.model.ViolationItem;
import com.example.parkguardai.data.repository.AdminRepository;
import com.example.parkguardai.data.repository.UserProfileRepository;
import com.example.parkguardai.databinding.FragmentAdminDashboardBinding;
import com.example.parkguardai.ui.common.AvatarLoader;
import com.example.parkguardai.ui.common.ViolationNotificationHelper;

import java.util.Calendar;

/**
 * Màn hình Tổng quan Cổng Quản trị viên (Admin Portal).
 * Đồng bộ 100% số liệu KPI, lưu lượng 7 ngày và vi phạm mới nhất trực tiếp từ Cloud Firestore.
 */
public class AdminDashboardFragment extends Fragment {

    private FragmentAdminDashboardBinding binding;
    private AdminRepository adminRepository;

    private final AdminRepository.AdminDataListener realtimeListener = () -> {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(this::loadDashboardData);
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adminRepository = AdminRepository.getInstance(requireContext());
        adminRepository.addDashboardListener(realtimeListener);

        AdminNavigation.bind(this, binding.adminBottomNav, R.id.adminDashboardFragment);

        if (ViolationNotificationHelper.isPushEnabled(requireContext())
                && ViolationNotificationHelper.hasNotificationPermission(requireContext())) {
            ViolationNotificationHelper.startSecurityAlertListener(requireContext());
        }

        binding.cardKpiViolationsToday.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.adminViolationsFragment));

        View.OnClickListener openVehiclesListener = v ->
                NavHostFragment.findNavController(this).navigate(R.id.adminVerificationQueueFragment);
        binding.cardKpiVehiclesInLot.setOnClickListener(openVehiclesListener);
        binding.cardKpiUnregisteredVehicles.setOnClickListener(openVehiclesListener);

        binding.btnViewAllViolations.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.adminViolationsFragment));
        binding.cardLatestViolationItem.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.adminViolationsFragment));
        binding.adminDashboardAvatarFrame.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.adminMoreFragment));

        loadDashboardData();
    }

    private void loadDashboardData() {
        if (binding == null || getContext() == null) return;

        UserProfileRepository.getInstance().getMyProfile().addOnSuccessListener(profile -> {
            if (binding == null || !isAdded() || profile == null) return;
            String displayName = (profile.getFullName() != null && !profile.getFullName().trim().isEmpty())
                    ? profile.getFullName().trim()
                    : (profile.getEmailCanonical() != null && !profile.getEmailCanonical().isEmpty()
                            ? profile.getEmailCanonical() : "Quản trị viên");
            binding.textAdminWelcome.setText("Xin chào, " + displayName);
            AvatarLoader.loadCircular(getActivity(), null, profile.getAvatarUrl(),
                    binding.ivAdminDashboardAvatar, binding.ivAdminDashboardFallbackIcon);
        });

        adminRepository.getDashboardKPIs().addOnSuccessListener(kpis -> {
            if (binding == null) return;
            binding.textKpiVehiclesCount.setText(String.valueOf(kpis.getTotalVehiclesInLot()));
            binding.textKpiViolationsCount.setText(String.valueOf(kpis.getViolationsToday()));
            binding.textKpiCamerasCount.setText(kpis.getCamerasOnline());
            binding.textKpiUnregisteredCount.setText(String.valueOf(kpis.getUnregisteredVehicles()));

            String aiInsight = adminRepository.getAiSecurityInsight(
                    kpis.getViolationsToday(), kpis.getUnregisteredVehicles());
            binding.textAiPredictionContent.setText(aiInsight);
        });

        adminRepository.getWeeklyTraffic().addOnSuccessListener(this::renderWeeklyTrafficChart);

        adminRepository.getAllViolations(null, null).addOnSuccessListener(violations -> {
            if (binding == null || getContext() == null) return;

            if (violations == null || violations.isEmpty()) {
                binding.cardLatestViolationItem.setVisibility(View.GONE);
                binding.layoutEmptyViolation.setVisibility(View.VISIBLE);
            } else {
                binding.layoutEmptyViolation.setVisibility(View.GONE);
                binding.cardLatestViolationItem.setVisibility(View.VISIBLE);

                ViolationItem latest = violations.get(0);
                binding.textLatestViolationPlate.setText(latest.plateNumber);
                binding.textLatestViolationDetails.setText(
                        latest.violationName + (latest.timestamp.isEmpty() ? "" : " • " + latest.timestamp));

                if (!latest.penalty.isEmpty()) {
                    binding.textLatestViolationPenalty.setText("Mức phạt: " + latest.penalty);
                    binding.textLatestViolationPenalty.setVisibility(View.VISIBLE);
                } else {
                    binding.textLatestViolationPenalty.setVisibility(View.GONE);
                }

                if (latest.isResolved()) {
                    binding.badgeLatestViolationStatus.setText("Đã xử lý");
                    binding.badgeLatestViolationStatus.setBackgroundResource(R.drawable.bg_badge_processed);
                    binding.badgeLatestViolationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_success));
                } else if (latest.isCancelled()) {
                    binding.badgeLatestViolationStatus.setText("Đã hủy");
                    binding.badgeLatestViolationStatus.setBackgroundResource(R.drawable.bg_button_dark_card);
                    binding.badgeLatestViolationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
                } else if (latest.isAppealed()) {
                    binding.badgeLatestViolationStatus.setText("Xem xét");
                    binding.badgeLatestViolationStatus.setBackgroundResource(R.drawable.bg_badge_warning_amber);
                    binding.badgeLatestViolationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.warn_yellow));
                } else {
                    binding.badgeLatestViolationStatus.setText("Chưa xử lý");
                    binding.badgeLatestViolationStatus.setBackgroundResource(R.drawable.bg_badge_unprocessed);
                    binding.badgeLatestViolationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_error));
                }
            }
        });
    }

    private void renderWeeklyTrafficChart(int[] traffic) {
        if (binding == null || getContext() == null || traffic == null || traffic.length < 7) return;

        int max = 1;
        for (int val : traffic) {
            if (val > max) max = val;
        }

        View[] bars = new View[]{
                binding.barMon, binding.barTue, binding.barWed,
                binding.barThu, binding.barFri, binding.barSat, binding.barSun
        };

        TextView[] labels = new TextView[]{
                binding.labelMon, binding.labelTue, binding.labelWed,
                binding.labelThu, binding.labelFri, binding.labelSat, binding.labelSun
        };

        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int currentDayIndex = (dayOfWeek == Calendar.SUNDAY) ? 6 : (dayOfWeek - 2);

        float density = getResources().getDisplayMetrics().density;
        int minHeightPx = (int) (10 * density);
        int maxHeightPx = (int) (70 * density);

        int activeColor = ContextCompat.getColor(requireContext(), R.color.accent_cyan);
        int normalBarColor = ContextCompat.getColor(requireContext(), R.color.surface_700);
        int normalTextColor = ContextCompat.getColor(requireContext(), R.color.text_muted);

        for (int i = 0; i < 7; i++) {
            View bar = bars[i];
            TextView label = labels[i];
            if (bar == null || label == null) continue;

            int heightPx = traffic[i] == 0
                    ? minHeightPx
                    : minHeightPx + (int) ((traffic[i] / (float) max) * (maxHeightPx - minHeightPx));
            ViewGroup.LayoutParams lp = bar.getLayoutParams();
            if (lp != null) {
                lp.height = heightPx;
                bar.setLayoutParams(lp);
            }

            if (i == currentDayIndex) {
                bar.setBackgroundColor(activeColor);
                label.setTextColor(activeColor);
                label.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                bar.setBackgroundColor(normalBarColor);
                label.setTextColor(normalTextColor);
                label.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboardData();
    }

    @Override
    public void onDestroyView() {
        if (adminRepository != null) {
            adminRepository.removeDashboardListener(realtimeListener);
        }
        binding = null;
        super.onDestroyView();
    }
}
