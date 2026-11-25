package com.example.parkguardai.ui.student;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.data.model.StudentNotificationItem;
import com.example.parkguardai.databinding.FragmentStudentNotificationsBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * Màn hình Thông báo dành cho Sinh viên (Tab 3 Bottom Navigation).
 * Đọc và hiển thị danh sách thông báo thực tế từ Cloud Firestore.
 */
public final class StudentNotificationsFragment extends Fragment {
    private FragmentStudentNotificationsBinding binding;
    private StudentViewModel viewModel;
    private String currentFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StudentViewModel.class);

        StudentNavigation.bind(this, binding.studentBottomNav, R.id.studentNotificationsFragment);
        setupFilterTabs();

        viewModel.getNotifications().observe(getViewLifecycleOwner(), this::renderNotifications);
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty() && getContext() != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.loadNotifications(requireContext(), currentFilter);
    }

    private void renderNotifications(List<StudentNotificationItem> items) {
        if (binding == null || getContext() == null) return;

        int unreadCount = 0;
        if (items != null) {
            for (StudentNotificationItem item : items) {
                if (!item.isRead()) unreadCount++;
            }
        }
        binding.tvUnreadBadge.setText(unreadCount + " chưa đọc");

        binding.notificationsListContainer.removeAllViews();
        float density = getResources().getDisplayMetrics().density;

        if (items == null || items.isEmpty()) {
            MaterialCardView emptyCard = new MaterialCardView(requireContext());
            emptyCard.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            emptyCard.setRadius(16 * density);
            emptyCard.setStrokeWidth(Math.round(1 * density));
            emptyCard.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.outline));
            emptyCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_900));

            TextView emptyText = new TextView(requireContext());
            emptyText.setText("Hiện chưa có thông báo nào.");
            emptyText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            emptyText.setGravity(Gravity.CENTER);
            int p24 = Math.round(24 * density);
            emptyText.setPadding(p24, p24, p24, p24);
            emptyCard.addView(emptyText);
            binding.notificationsListContainer.addView(emptyCard);
            return;
        }

        for (StudentNotificationItem item : items) {
            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = Math.round(10 * density);
            card.setLayoutParams(cardLp);
            card.setRadius(16 * density);
            card.setStrokeWidth(Math.round(1 * density));
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.outline));
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_900));

            LinearLayout body = new LinearLayout(requireContext());
            body.setOrientation(LinearLayout.VERTICAL);
            int p14 = Math.round(14 * density);
            body.setPadding(p14, p14, p14, p14);

            LinearLayout header = new LinearLayout(requireContext());
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            ImageView icon = new ImageView(requireContext());
            int s18 = Math.round(18 * density);
            icon.setLayoutParams(new LinearLayout.LayoutParams(s18, s18));
            if ("VIOLATION".equalsIgnoreCase(item.getType())) {
                icon.setImageResource(R.drawable.ic_alert_triangle);
                icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.violation_red));
            } else if ("SECURITY_ALERT".equalsIgnoreCase(item.getType())) {
                icon.setImageResource(R.drawable.ic_alert_triangle);
                icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.warn_yellow));
            } else if ("VEHICLE".equalsIgnoreCase(item.getType())) {
                icon.setImageResource(R.drawable.ic_student_vehicle);
                icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.pg_success));
            } else {
                icon.setImageResource(R.drawable.ic_guard_alert);
                icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.accent_cyan));
            }
            header.addView(icon);

            TextView tvTitle = new TextView(requireContext());
            tvTitle.setText(item.getTitle());
            tvTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvTitle.setTextSize(14f);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            titleLp.setMarginStart(Math.round(8 * density));
            tvTitle.setLayoutParams(titleLp);
            header.addView(tvTitle);

            TextView tvTime = new TextView(requireContext());
            tvTime.setText(item.getTimeFormatted());
            tvTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            tvTime.setTextSize(11f);
            header.addView(tvTime);

            body.addView(header);

            TextView tvMsg = new TextView(requireContext());
            tvMsg.setText(item.getMessage());
            tvMsg.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvMsg.setTextSize(13f);
            tvMsg.setPadding(0, Math.round(6 * density), 0, 0);
            body.addView(tvMsg);

            if (item.getRelatedId() != null && !item.getRelatedId().isEmpty()) {
                LinearLayout actions = new LinearLayout(requireContext());
                actions.setOrientation(LinearLayout.HORIZONTAL);
                actions.setPadding(0, Math.round(10 * density), 0, 0);

                MaterialButton btnDetail = new MaterialButton(requireContext());
                btnDetail.setText(R.string.student_notif_btn_view_detail);
                btnDetail.setTextSize(12f);
                btnDetail.setCornerRadius(Math.round(18 * density));
                btnDetail.setOnClickListener(v -> {
                    Bundle args = new Bundle();
                    args.putString("violation_id", item.getRelatedId());
                    NavHostFragment.findNavController(this).navigate(R.id.studentViolationDetailFragment, args);
                });
                actions.addView(btnDetail);
                body.addView(actions);
            } else if (item.isRequiresAction()) {
                LinearLayout actions = new LinearLayout(requireContext());
                actions.setOrientation(LinearLayout.HORIZONTAL);
                actions.setPadding(0, Math.round(10 * density), 0, 0);

                MaterialButton btnIsMe = new MaterialButton(requireContext());
                btnIsMe.setText(R.string.student_notif_btn_is_me);
                btnIsMe.setTextSize(12f);
                btnIsMe.setCornerRadius(Math.round(18 * density));
                btnIsMe.setOnClickListener(v -> viewModel.respondNotification(requireContext(), item.getId(), true));
                actions.addView(btnIsMe);

                MaterialButton btnNotMe = new MaterialButton(requireContext(), null,
                        com.google.android.material.R.attr.materialButtonOutlinedStyle);
                btnNotMe.setText(R.string.student_notif_btn_not_me);
                btnNotMe.setTextSize(12f);
                btnNotMe.setCornerRadius(Math.round(18 * density));
                LinearLayout.LayoutParams notMeLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                notMeLp.setMarginStart(Math.round(8 * density));
                btnNotMe.setLayoutParams(notMeLp);
                btnNotMe.setOnClickListener(v -> viewModel.respondNotification(requireContext(), item.getId(), false));
                actions.addView(btnNotMe);

                body.addView(actions);
            }

            card.addView(body);
            binding.notificationsListContainer.addView(card);
        }
    }

    private void setupFilterTabs() {
        binding.tabAll.setOnClickListener(v -> {
            selectTab(binding.tabAll);
            currentFilter = "ALL";
            viewModel.loadNotifications(requireContext(), currentFilter);
        });
        binding.tabViolation.setOnClickListener(v -> {
            selectTab(binding.tabViolation);
            currentFilter = "VIOLATION";
            viewModel.loadNotifications(requireContext(), currentFilter);
        });
        binding.tabVehicle.setOnClickListener(v -> {
            selectTab(binding.tabVehicle);
            currentFilter = "VEHICLE";
            viewModel.loadNotifications(requireContext(), currentFilter);
        });
        binding.tabSystem.setOnClickListener(v -> {
            selectTab(binding.tabSystem);
            currentFilter = "SYSTEM";
            viewModel.loadNotifications(requireContext(), currentFilter);
        });
    }

    private void selectTab(TextView selected) {
        resetTab(binding.tabAll);
        resetTab(binding.tabViolation);
        resetTab(binding.tabVehicle);
        resetTab(binding.tabSystem);

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
            viewModel.loadNotifications(requireContext(), currentFilter);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
