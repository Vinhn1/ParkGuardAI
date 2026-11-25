package com.example.parkguardai.ui.student;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.data.model.ViolationItem;
import com.example.parkguardai.databinding.FragmentStudentViolationsBinding;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình Vi phạm của tôi (Tab 2 Bottom Navigation).
 * Hiển thị danh sách các hồ sơ vi phạm thật của sinh viên từ Cloud Firestore.
 */
public class StudentViolationsFragment extends Fragment {
    private FragmentStudentViolationsBinding binding;
    private StudentViewModel viewModel;
    private int currentFilterIndex = 0; // 0: Tất cả, 1: Chưa xử lý, 2: Khiếu nại, 3: Đã xử lý
    private final List<ViolationItem> allViolations = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentViolationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StudentViewModel.class);

        StudentNavigation.bind(this, binding.studentBottomNav, R.id.studentViolationsFragment);
        setupFilterTabs();

        viewModel.getViolations().observe(getViewLifecycleOwner(), this::updateViolationsList);
        viewModel.loadViolations(requireContext());
    }

    private void navigateToDetail(String violationId) {
        Bundle args = new Bundle();
        args.putString("violation_id", violationId);
        NavHostFragment.findNavController(this).navigate(R.id.studentViolationDetailFragment, args);
    }

    private void updateViolationsList(List<ViolationItem> list) {
        allViolations.clear();
        if (list != null) {
            allViolations.addAll(list);
        }
        renderFilteredViolations();
    }

    private void renderFilteredViolations() {
        if (binding == null || getContext() == null) return;

        int unprocessedCount = 0;
        List<ViolationItem> filtered = new ArrayList<>();
        for (ViolationItem item : allViolations) {
            if (item.isUnprocessed()) unprocessedCount++;

            if (currentFilterIndex == 0) {
                filtered.add(item);
            } else if (currentFilterIndex == 1 && item.isUnprocessed()) {
                filtered.add(item);
            } else if (currentFilterIndex == 2 && item.isAppealed()) {
                filtered.add(item);
            } else if (currentFilterIndex == 3 && item.isProcessed()) {
                filtered.add(item);
            }
        }

        if (unprocessedCount > 0) {
            binding.tvUnprocessedBadge.setText(unprocessedCount + " chưa xử lý");
            binding.tvUnprocessedBadge.setBackgroundResource(R.drawable.bg_badge_unprocessed);
            binding.tvUnprocessedBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.violation_red));
        } else {
            binding.tvUnprocessedBadge.setText("0 chưa xử lý");
            binding.tvUnprocessedBadge.setBackgroundResource(R.drawable.bg_badge_processed);
            binding.tvUnprocessedBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_success));
        }

        binding.violationsListContainer.removeAllViews();
        float density = getResources().getDisplayMetrics().density;

        if (filtered.isEmpty()) {
            MaterialCardView emptyCard = new MaterialCardView(requireContext());
            emptyCard.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            emptyCard.setRadius(16 * density);
            emptyCard.setStrokeWidth(Math.round(1 * density));
            emptyCard.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.outline));
            emptyCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_900));

            TextView emptyText = new TextView(requireContext());
            emptyText.setText("Không có biên bản vi phạm nào trong danh mục này.");
            emptyText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            emptyText.setGravity(Gravity.CENTER);
            int p24 = Math.round(24 * density);
            emptyText.setPadding(p24, p24, p24, p24);
            emptyCard.addView(emptyText);
            binding.violationsListContainer.addView(emptyCard);
            return;
        }

        for (ViolationItem vio : filtered) {
            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = Math.round(10 * density);
            card.setLayoutParams(cardLp);
            card.setRadius(16 * density);
            card.setStrokeWidth(Math.round(1 * density));
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.outline));
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_900));
            card.setClickable(true);
            card.setFocusable(true);

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int p14 = Math.round(14 * density);
            row.setPadding(p14, p14, p14, p14);

            FrameLayout iconFrame = new FrameLayout(requireContext());
            int s38 = Math.round(38 * density);
            iconFrame.setLayoutParams(new LinearLayout.LayoutParams(s38, s38));

            ImageView icon = new ImageView(requireContext());
            int s18 = Math.round(18 * density);
            FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(s18, s18);
            iconLp.gravity = Gravity.CENTER;
            icon.setLayoutParams(iconLp);
            icon.setImageResource(R.drawable.ic_alert_triangle);

            TextView badge = new TextView(requireContext());
            badge.setTextSize(11f);
            badge.setTypeface(null, android.graphics.Typeface.BOLD);
            int px = Math.round(10 * density);
            int py = Math.round(4 * density);
            badge.setPadding(px, py, px, py);

            if (vio.isProcessed()) {
                iconFrame.setBackgroundResource(R.drawable.bg_kpi_icon_green);
                icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.pg_success));
                badge.setBackgroundResource(R.drawable.bg_badge_processed);
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_success));
                badge.setText(vio.isCancelled() ? "Đã hủy" : "Đã xử lý");
            } else if (vio.isAppealed()) {
                iconFrame.setBackgroundResource(R.drawable.bg_kpi_icon_orange);
                icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.warn_yellow));
                badge.setBackgroundResource(R.drawable.bg_badge_warning_amber);
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.warn_yellow));
                badge.setText("Khiếu nại");
            } else {
                iconFrame.setBackgroundResource(R.drawable.bg_kpi_icon_red);
                icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.violation_red));
                badge.setBackgroundResource(R.drawable.bg_badge_unprocessed);
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.violation_red));
                badge.setText("Chưa xử lý");
            }

            iconFrame.addView(icon);
            row.addView(iconFrame);

            LinearLayout infoCol = new LinearLayout(requireContext());
            infoCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            infoLp.setMarginStart(Math.round(12 * density));
            infoCol.setLayoutParams(infoLp);

            TextView tvPlate = new TextView(requireContext());
            tvPlate.setText(vio.plateNumber);
            tvPlate.setTextSize(15f);
            tvPlate.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvPlate.setTypeface(null, android.graphics.Typeface.BOLD);
            infoCol.addView(tvPlate);

            TextView tvSub = new TextView(requireContext());
            String sub = vio.violationName + (vio.timestamp.isEmpty() ? "" : " • " + vio.timestamp);
            tvSub.setText(sub);
            tvSub.setTextSize(12f);
            tvSub.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            infoCol.addView(tvSub);

            row.addView(infoCol);
            row.addView(badge);
            card.addView(row);

            card.setOnClickListener(v -> navigateToDetail(vio.id));
            binding.violationsListContainer.addView(card);
        }
    }

    private void setupFilterTabs() {
        binding.tabAll.setOnClickListener(v -> {
            selectTab(binding.tabAll);
            currentFilterIndex = 0;
            renderFilteredViolations();
        });
        binding.tabUnprocessed.setOnClickListener(v -> {
            selectTab(binding.tabUnprocessed);
            currentFilterIndex = 1;
            renderFilteredViolations();
        });
        binding.tabAppealed.setOnClickListener(v -> {
            selectTab(binding.tabAppealed);
            currentFilterIndex = 2;
            renderFilteredViolations();
        });
        binding.tabResolved.setOnClickListener(v -> {
            selectTab(binding.tabResolved);
            currentFilterIndex = 3;
            renderFilteredViolations();
        });
    }

    private void selectTab(TextView selected) {
        resetTab(binding.tabAll);
        resetTab(binding.tabUnprocessed);
        resetTab(binding.tabAppealed);
        resetTab(binding.tabResolved);

        selected.setBackgroundResource(R.drawable.bg_segmented_active);
        selected.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        selected.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void resetTab(TextView tab) {
        tab.setBackground(null);
        tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        tab.setTypeface(null, android.graphics.Typeface.NORMAL);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null && getContext() != null) {
            viewModel.loadViolations(requireContext());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
