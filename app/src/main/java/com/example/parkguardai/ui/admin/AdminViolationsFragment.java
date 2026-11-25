package com.example.parkguardai.ui.admin;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.data.model.ViolationItem;
import com.example.parkguardai.data.repository.AdminRepository;
import com.example.parkguardai.databinding.FragmentAdminViolationsBinding;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * Màn hình Quản lý vi phạm trong cổng Quản trị viên (Admin Portal).
 * Hiển thị danh sách vi phạm thật và bộ đếm thực tế từ Cloud Firestore.
 */
public class AdminViolationsFragment extends Fragment {
    private FragmentAdminViolationsBinding binding;
    private AdminRepository adminRepository;
    private int currentTab = 0; // 0: Tất cả, 1: Chưa xử lý, 2: Xem xét, 3: Đã xử lý

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminViolationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adminRepository = AdminRepository.getInstance(requireContext());

        AdminNavigation.bind(this, binding.bottomNav, R.id.adminViolationsFragment);

        binding.btnBack.setOnClickListener(v -> {
            if (!NavHostFragment.findNavController(this).navigateUp()) {
                requireActivity().onBackPressed();
            }
        });

        setupFilterTabs();
        loadViolations();
    }

    private void setupFilterTabs() {
        binding.tabAll.setOnClickListener(v -> selectTab(0));
        binding.tabUnprocessed.setOnClickListener(v -> selectTab(1));
        binding.tabReviewing.setOnClickListener(v -> selectTab(2));
        binding.tabProcessed.setOnClickListener(v -> selectTab(3));
    }

    private void selectTab(int index) {
        currentTab = index;
        int activeBg = R.drawable.bg_segmented_active;
        int inactiveBg = R.drawable.bg_segmented_inactive;
        int activeColor = ContextCompat.getColor(requireContext(), R.color.white);
        int inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_muted);

        binding.tabAll.setBackgroundResource(index == 0 ? activeBg : inactiveBg);
        binding.tabAll.setTextColor(index == 0 ? activeColor : inactiveColor);

        binding.tabUnprocessed.setBackgroundResource(index == 1 ? activeBg : inactiveBg);
        binding.tabUnprocessed.setTextColor(index == 1 ? activeColor : inactiveColor);

        binding.tabReviewing.setBackgroundResource(index == 2 ? activeBg : inactiveBg);
        binding.tabReviewing.setTextColor(index == 2 ? activeColor : inactiveColor);

        binding.tabProcessed.setBackgroundResource(index == 3 ? activeBg : inactiveBg);
        binding.tabProcessed.setTextColor(index == 3 ? activeColor : inactiveColor);

        loadViolations();
    }

    private void loadViolations() {
        if (binding == null) return;

        String filterStatus = null;
        if (currentTab == 1) filterStatus = "UNPROCESSED";
        else if (currentTab == 2) filterStatus = "REVIEWING";
        else if (currentTab == 3) filterStatus = "PROCESSED";

        adminRepository.getAllViolations(filterStatus, null).addOnSuccessListener(list -> {
            if (binding == null) return;
            renderViolationsList(list);
            updateSummaryCounters();
        });
    }

    private void updateSummaryCounters() {
        adminRepository.getAllViolations(null, null).addOnSuccessListener(all -> {
            if (binding == null || all == null) return;
            long sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000L;
            int thisWeek = 0;
            int unprocessed = 0;
            for (ViolationItem v : all) {
                if (v.capturedAtMillis >= sevenDaysAgo) thisWeek++;
                if (v.isUnprocessed()) unprocessed++;
            }
            binding.tvStatThisWeek.setText(String.valueOf(thisWeek));
            binding.tvStatUnprocessed.setText(String.valueOf(unprocessed));
        });
    }

    private void renderViolationsList(List<ViolationItem> violations) {
        if (binding == null || getContext() == null) return;
        binding.violationsListContainer.removeAllViews();

        if (violations == null || violations.isEmpty()) {
            TextView emptyText = new TextView(requireContext());
            emptyText.setText("Không có biên bản vi phạm nào trong danh mục này");
            emptyText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, 48, 0, 48);
            binding.violationsListContainer.addView(emptyText);
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        for (ViolationItem vio : violations) {
            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = (int) (10 * density);
            card.setLayoutParams(cardParams);
            card.setRadius(16 * density);
            card.setStrokeWidth((int) (1 * density));
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.outline));
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_800));
            card.setClickable(true);
            card.setFocusable(true);

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int p14 = (int) (14 * density);
            row.setPadding(p14, p14, p14, p14);

            FrameLayout iconFrame = new FrameLayout(requireContext());
            int s38 = (int) (38 * density);
            iconFrame.setLayoutParams(new LinearLayout.LayoutParams(s38, s38));

            ImageView icon = new ImageView(requireContext());
            int s20 = (int) (20 * density);
            FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(s20, s20);
            iconParams.gravity = Gravity.CENTER;
            icon.setLayoutParams(iconParams);
            icon.setImageResource(R.drawable.ic_student_violation);

            if (vio.isProcessed()) {
                iconFrame.setBackgroundResource(R.drawable.bg_badge_processed);
                icon.setColorFilter(0xFF10B981);
            } else if (vio.isAppealed()) {
                iconFrame.setBackgroundResource(R.drawable.bg_kpi_icon_orange);
                icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.warn_yellow));
            } else {
                iconFrame.setBackgroundResource(R.drawable.bg_kpi_icon_red);
                icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error_red));
            }
            iconFrame.addView(icon);
            row.addView(iconFrame);

            LinearLayout infoCol = new LinearLayout(requireContext());
            infoCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            infoParams.setMarginStart((int) (12 * density));
            infoCol.setLayoutParams(infoParams);

            TextView tvPlate = new TextView(requireContext());
            tvPlate.setText(vio.plateNumber);
            tvPlate.setTextSize(16);
            tvPlate.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvPlate.setTypeface(null, android.graphics.Typeface.BOLD);
            infoCol.addView(tvPlate);

            TextView tvSub = new TextView(requireContext());
            tvSub.setText(vio.violationName + (vio.timestamp.isEmpty() ? "" : " • " + vio.timestamp));
            tvSub.setTextSize(12);
            tvSub.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            infoCol.addView(tvSub);

            row.addView(infoCol);

            TextView badge = new TextView(requireContext());
            badge.setTextSize(11);
            badge.setTypeface(null, android.graphics.Typeface.BOLD);
            int px = (int) (10 * density);
            int py = (int) (4 * density);
            badge.setPadding(px, py, px, py);

            if (vio.isProcessed()) {
                badge.setBackgroundResource(R.drawable.bg_badge_processed);
                badge.setTextColor(0xFF10B981);
                badge.setText(vio.isCancelled() ? "Đã hủy" : getString(R.string.admin_violations_filter_processed));
            } else if (vio.isAppealed()) {
                badge.setBackgroundResource(R.drawable.bg_badge_warning_amber);
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.warn_yellow));
                badge.setText(R.string.admin_violations_filter_reviewing);
            } else {
                badge.setBackgroundResource(R.drawable.bg_badge_unprocessed);
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red));
                badge.setText(R.string.admin_violations_filter_unprocessed);
            }

            row.addView(badge);
            card.addView(row);

            card.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString(AdminViolationDetailFragment.ARG_VIOLATION_ID, vio.id);
                NavHostFragment.findNavController(this)
                        .navigate(R.id.adminViolationDetailFragment, args);
            });

            binding.violationsListContainer.addView(card);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadViolations();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
