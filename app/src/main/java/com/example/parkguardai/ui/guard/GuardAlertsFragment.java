package com.example.parkguardai.ui.guard;

import android.content.res.ColorStateList;
import android.os.Bundle;
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
import com.example.parkguardai.data.model.GuardSecurityAlert;
import com.example.parkguardai.data.repository.GuardRepository;
import com.example.parkguardai.databinding.FragmentGuardAlertsBinding;
import com.example.parkguardai.databinding.ItemGuardSecurityAlertBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình Cảnh báo an ninh bãi xe dành cho Bảo vệ (Tab 3 Bottom Navigation).
 * Dữ liệu cập nhật REALTIME từ quá trình quét hiện trường và hệ thống cảnh báo.
 * Loại bỏ hoàn toàn dữ liệu mock mẫu, hỗ trợ lọc theo 3 tab (Tất cả, Chưa xử lý, Đã xử lý),
 * xác minh/xử lý cảnh báo tức thì và liên kết xử lý vi phạm.
 */
public final class GuardAlertsFragment extends Fragment {
    private static final int TAB_ALL = 0;
    private static final int TAB_UNHANDLED = 1;
    private static final int TAB_RESOLVED = 2;

    private FragmentGuardAlertsBinding binding;
    private GuardRepository repository;
    private final List<GuardSecurityAlert> allAlerts = new ArrayList<>();
    private int currentTab = TAB_ALL;

    // Listener nhận dữ liệu cảnh báo an ninh Realtime
    private final GuardRepository.AlertListener realtimeAlertListener = alerts -> {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            allAlerts.clear();
            if (alerts != null) {
                allAlerts.addAll(alerts);
            }
            if (binding != null) {
                binding.layoutAlertsLoading.setVisibility(View.GONE);
                filterAndRenderCurrentTab();
            }
        });
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGuardAlertsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = GuardRepository.getInstance(requireContext());

        // Liên kết Bottom Navigation Bar (Tab Cảnh báo)
        GuardNavigation.bind(this, binding.guardBottomNav, R.id.guardAlertsFragment);

        // Nút Làm mới dữ liệu
        binding.btnRefreshAlerts.setOnClickListener(v -> {
            binding.layoutAlertsLoading.setVisibility(View.VISIBLE);
            repository.notifyAlertsUpdated();
            Toast.makeText(requireContext(), "Đã đồng bộ cảnh báo an ninh mới nhất", Toast.LENGTH_SHORT).show();
        });

        // Nút Quét khi danh sách rỗng
        binding.btnAlertsEmptyScan.setOnClickListener(v -> {
            try {
                NavHostFragment.findNavController(this).navigate(R.id.guardCameraFragment);
            } catch (Exception ignored) { }
        });

        // Chuyển đổi 3 Tabs lọc
        binding.tabAll.setOnClickListener(v -> selectTab(TAB_ALL));
        binding.tabUnhandled.setOnClickListener(v -> selectTab(TAB_UNHANDLED));
        binding.tabResolved.setOnClickListener(v -> selectTab(TAB_RESOLVED));

        // Khởi tạo trạng thái ban đầu
        selectTab(TAB_ALL);

        // Đăng ký nhận dữ liệu cảnh báo Realtime
        binding.layoutAlertsLoading.setVisibility(View.VISIBLE);
        repository.addAlertListener(realtimeAlertListener);
    }

    private void selectTab(int tabIndex) {
        currentTab = tabIndex;

        int activeBg = R.drawable.bg_segmented_active;
        int inactiveBg = R.drawable.bg_segmented_inactive;
        int activeColor = ContextCompat.getColor(requireContext(), R.color.white);
        int inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);

        binding.tabAll.setBackgroundResource(tabIndex == TAB_ALL ? activeBg : inactiveBg);
        binding.tabAll.setTextColor(tabIndex == TAB_ALL ? activeColor : inactiveColor);

        binding.tabUnhandled.setBackgroundResource(tabIndex == TAB_UNHANDLED ? activeBg : inactiveBg);
        binding.tabUnhandled.setTextColor(tabIndex == TAB_UNHANDLED ? activeColor : inactiveColor);

        binding.tabResolved.setBackgroundResource(tabIndex == TAB_RESOLVED ? activeBg : inactiveBg);
        binding.tabResolved.setTextColor(tabIndex == TAB_RESOLVED ? activeColor : inactiveColor);

        filterAndRenderCurrentTab();
    }

    private void filterAndRenderCurrentTab() {
        if (binding == null) return;

        int countAll = allAlerts.size();
        int countUnhandled = 0;
        int countResolved = 0;

        List<GuardSecurityAlert> filtered = new ArrayList<>();

        for (GuardSecurityAlert alert : allAlerts) {
            if (alert.status == GuardSecurityAlert.Status.UNHANDLED) {
                countUnhandled++;
            } else {
                countResolved++;
            }

            if (currentTab == TAB_ALL) {
                filtered.add(alert);
            } else if (currentTab == TAB_UNHANDLED && alert.status == GuardSecurityAlert.Status.UNHANDLED) {
                filtered.add(alert);
            } else if (currentTab == TAB_RESOLVED && alert.status == GuardSecurityAlert.Status.RESOLVED) {
                filtered.add(alert);
            }
        }

        // Cập nhật số đếm trên các Tabs
        binding.tabAll.setText(countAll > 0 ? "Tất cả (" + countAll + ")" : getString(R.string.guard_alerts_filter_all));
        binding.tabUnhandled.setText(countUnhandled > 0 ? "Chưa xử lý (" + countUnhandled + ")" : getString(R.string.guard_alerts_filter_unhandled));
        binding.tabResolved.setText(countResolved > 0 ? "Đã xử lý (" + countResolved + ")" : getString(R.string.guard_alerts_filter_resolved));

        // Cập nhật Badge trên thanh Top Bar
        if (countUnhandled > 0) {
            binding.tvAlertsBadge.setText(countUnhandled + " mới");
            binding.tvAlertsBadge.setBackgroundResource(R.drawable.bg_badge_warning_amber);
            binding.tvAlertsBadge.setTextColor(0xFFFFB525);
        } else {
            binding.tvAlertsBadge.setText("An toàn");
            binding.tvAlertsBadge.setBackgroundResource(R.drawable.bg_badge_processed);
            binding.tvAlertsBadge.setTextColor(0xFF10B981);
        }

        // Render giao diện danh sách
        binding.layoutAlertsContainer.removeAllViews();

        if (filtered.isEmpty()) {
            binding.layoutAlertsEmpty.setVisibility(View.VISIBLE);
            binding.layoutAlertsContainer.setVisibility(View.GONE);

            if (currentTab == TAB_UNHANDLED) {
                binding.tvAlertsEmptyTitle.setText("Không có cảnh báo tồn đọng");
                binding.tvAlertsEmptyDesc.setText("Toàn bộ cảnh báo đã được xác minh và xử lý an toàn.");
            } else if (currentTab == TAB_RESOLVED) {
                binding.tvAlertsEmptyTitle.setText("Chưa có cảnh báo đã xử lý");
                binding.tvAlertsEmptyDesc.setText("Các cảnh báo sau khi bạn bấm xác minh sẽ được lưu trữ tại đây.");
            } else {
                binding.tvAlertsEmptyTitle.setText("Khuôn viên an toàn");
                binding.tvAlertsEmptyDesc.setText("Hiện tại không có cảnh báo an ninh nào trong ca trực. Toàn bộ phương tiện tuân thủ đúng quy định.");
            }
        } else {
            binding.layoutAlertsEmpty.setVisibility(View.GONE);
            binding.layoutAlertsContainer.setVisibility(View.VISIBLE);

            LayoutInflater inflater = LayoutInflater.from(requireContext());
            for (GuardSecurityAlert alert : filtered) {
                ItemGuardSecurityAlertBinding itemBinding = ItemGuardSecurityAlertBinding.inflate(
                        inflater, binding.layoutAlertsContainer, false);

                itemBinding.tvAlertTitle.setText(alert.title);
                itemBinding.tvAlertPlate.setText(alert.plateNumber != null && !alert.plateNumber.isEmpty() ? alert.plateNumber : "CHƯA RÕ");
                itemBinding.tvAlertLocation.setText(alert.location != null ? alert.location : "Bãi xe chính");
                itemBinding.tvAlertDescription.setText(alert.description);
                itemBinding.tvAlertTime.setText(alert.timestamp != null ? alert.timestamp : "Vừa xong");

                if (alert.status == GuardSecurityAlert.Status.RESOLVED) {
                    // Trạng thái đã xử lý
                    itemBinding.cardAlertRoot.setCardBackgroundColor(0xFFFFFFFF);
                    itemBinding.cardAlertRoot.setStrokeColor(0xFFEAE0D5);

                    itemBinding.badgeAlertSeverity.setText("Đã xử lý");
                    itemBinding.badgeAlertSeverity.setBackgroundResource(R.drawable.bg_badge_processed);
                    itemBinding.badgeAlertSeverity.setTextColor(0xFF16A34A);

                    itemBinding.ivAlertIcon.setImageResource(R.drawable.ic_shield_check);
                    itemBinding.ivAlertIcon.setColorFilter(0xFF16A34A);
                    itemBinding.tvAlertTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));

                    itemBinding.layoutAlertActions.setVisibility(View.GONE);
                    itemBinding.layoutAlertResolvedInfo.setVisibility(View.VISIBLE);
                    itemBinding.tvAlertResolvedNote.setText(alert.resolutionNote != null && !alert.resolutionNote.isEmpty()
                            ? alert.resolutionNote : "Đã xử lý an toàn");
                } else {
                    // Trạng thái chưa xử lý
                    itemBinding.layoutAlertActions.setVisibility(View.VISIBLE);
                    itemBinding.layoutAlertResolvedInfo.setVisibility(View.GONE);

                    if (alert.severity == GuardSecurityAlert.Severity.HIGH) {
                        itemBinding.cardAlertRoot.setCardBackgroundColor(0xFFFFFFFF);
                        itemBinding.cardAlertRoot.setStrokeColor(0xFFFCA5A5);
                        itemBinding.badgeAlertSeverity.setText("Khẩn cấp");
                        itemBinding.badgeAlertSeverity.setBackgroundResource(R.drawable.bg_badge_violation_pill);
                        itemBinding.badgeAlertSeverity.setTextColor(0xFFDC2626);
                        itemBinding.ivAlertIcon.setImageResource(R.drawable.ic_alert_triangle);
                        itemBinding.ivAlertIcon.setColorFilter(0xFFDC2626);
                        itemBinding.tvAlertTitle.setTextColor(0xFFDC2626);
                        itemBinding.btnAlertHandle.setBackgroundTintList(ColorStateList.valueOf(0xFFDC2626));
                        itemBinding.btnAlertHandle.setText("Kiểm tra ngay");
                    } else if (alert.severity == GuardSecurityAlert.Severity.MEDIUM) {
                        itemBinding.cardAlertRoot.setCardBackgroundColor(0xFFFFFFFF);
                        itemBinding.cardAlertRoot.setStrokeColor(0xFFFCD34D);
                        itemBinding.badgeAlertSeverity.setText("Cần chú ý");
                        itemBinding.badgeAlertSeverity.setBackgroundResource(R.drawable.bg_badge_warning_amber);
                        itemBinding.badgeAlertSeverity.setTextColor(0xFFD97706);
                        itemBinding.ivAlertIcon.setImageResource(R.drawable.ic_alert_triangle);
                        itemBinding.ivAlertIcon.setColorFilter(0xFFD97706);
                        itemBinding.tvAlertTitle.setTextColor(0xFFB45309);
                        itemBinding.btnAlertHandle.setBackgroundTintList(ColorStateList.valueOf(0xFFE86A24));
                        itemBinding.btnAlertHandle.setText("Lập hồ sơ");
                    } else {
                        itemBinding.cardAlertRoot.setCardBackgroundColor(0xFFFFFFFF);
                        itemBinding.cardAlertRoot.setStrokeColor(0xFFEAE0D5);
                        itemBinding.badgeAlertSeverity.setText("Thông tin");
                        itemBinding.badgeAlertSeverity.setBackgroundResource(R.drawable.bg_badge_record_id);
                        itemBinding.badgeAlertSeverity.setTextColor(0xFFE86A24);
                        itemBinding.ivAlertIcon.setImageResource(R.drawable.ic_guard_alert);
                        itemBinding.ivAlertIcon.setColorFilter(0xFFE86A24);
                        itemBinding.tvAlertTitle.setTextColor(0xFF1C1917);
                        itemBinding.btnAlertHandle.setBackgroundTintList(ColorStateList.valueOf(0xFFE86A24));
                        itemBinding.btnAlertHandle.setText("Chi tiết");
                    }

                    // Nút Đã xử lý / Xác minh
                    itemBinding.btnAlertResolve.setOnClickListener(v -> {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Xác nhận xử lý cảnh báo")
                                .setMessage("Xác nhận phương tiện " + alert.plateNumber + " đã được kiểm tra an toàn hoặc giải quyết?")
                                .setPositiveButton("Xác nhận", (dialog, which) -> {
                                    repository.resolveAlert(alert.id, "Đã xác minh và kiểm tra an toàn", null);
                                    Toast.makeText(requireContext(), "Đã xử lý cảnh báo xe " + alert.plateNumber, Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("Hủy", null)
                                .show();
                    });

                    // Nút Xử lý vi phạm / Kiểm tra xe
                    itemBinding.btnAlertHandle.setOnClickListener(v -> {
                        Bundle bundle = new Bundle();
                        bundle.putString(GuardRescanAlertFragment.ARG_PLATE, alert.plateNumber);
                        try {
                            NavHostFragment.findNavController(GuardAlertsFragment.this)
                                    .navigate(R.id.guardRescanAlertFragment, bundle);
                        } catch (Exception e) {
                            try {
                                NavHostFragment.findNavController(GuardAlertsFragment.this)
                                        .navigate(R.id.guardCameraFragment);
                            } catch (Exception ignored) { }
                        }
                    });
                }

                binding.layoutAlertsContainer.addView(itemBinding.getRoot());
            }
        }
    }

    @Override
    public void onDestroyView() {
        if (repository != null) {
            repository.removeAlertListener(realtimeAlertListener);
        }
        binding = null;
        super.onDestroyView();
    }
}
