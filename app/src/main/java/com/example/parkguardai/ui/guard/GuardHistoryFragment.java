package com.example.parkguardai.ui.guard;

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
import com.example.parkguardai.data.model.ViolationRecord;
import com.example.parkguardai.data.repository.GuardRepository;
import com.example.parkguardai.databinding.FragmentGuardHistoryBinding;
import com.example.parkguardai.databinding.ItemGuardHistoryRecordBinding;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Màn hình Lịch sử hồ sơ vi phạm phương tiện phân hệ Bảo vệ (Tab Lịch sử).
 * Dữ liệu cập nhật REALTIME từ hệ thống và Firestore, loại bỏ hoàn toàn dữ liệu mock mẫu.
 * Hỗ trợ lọc theo Hôm nay / Hôm qua / 7 ngày, xem chi tiết hồ sơ và chuyển nhanh sang quét camera.
 */
public class GuardHistoryFragment extends Fragment {
    private static final int TAB_TODAY = 0;
    private static final int TAB_YESTERDAY = 1;
    private static final int TAB_7DAYS = 2;

    private FragmentGuardHistoryBinding binding;
    private GuardRepository repository;
    private final List<ViolationRecord> allRecords = new ArrayList<>();
    private int currentTabIndex = TAB_TODAY;

    // Listener nhận dữ liệu vi phạm cập nhật Realtime
    private final GuardRepository.ViolationListener realtimeListener = records -> {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            allRecords.clear();
            if (records != null) {
                allRecords.addAll(records);
            }
            if (binding != null) {
                binding.layoutHistoryLoading.setVisibility(View.GONE);
                filterAndRenderCurrentTab();
            }
        });
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGuardHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = GuardRepository.getInstance(requireContext());

        // Liên kết Bottom Navigation Bar (Tab Lịch sử)
        GuardNavigation.bind(this, binding.guardBottomNav, R.id.guardHistoryFragment);

        // Nút quay lại
        binding.btnBack.setOnClickListener(v -> {
            if (!NavHostFragment.findNavController(this).navigateUp()) {
                requireActivity().onBackPressed();
            }
        });

        // Nút làm mới dữ liệu thủ công
        binding.btnRefreshHistory.setOnClickListener(v -> {
            binding.layoutHistoryLoading.setVisibility(View.VISIBLE);
            repository.notifyViolationsUpdated();
            Toast.makeText(requireContext(), "Đã đồng bộ dữ liệu hồ sơ mới nhất", Toast.LENGTH_SHORT).show();
        });

        // Nút hành động khi danh sách rỗng -> Mở camera quét vi phạm ngay
        binding.btnEmptyActionScan.setOnClickListener(v -> {
            try {
                NavHostFragment.findNavController(this).navigate(R.id.guardCameraFragment);
            } catch (Exception ignored) { }
        });

        // Chuyển đổi các tab lọc thời gian
        binding.tabToday.setOnClickListener(v -> selectTab(TAB_TODAY));
        binding.tabYesterday.setOnClickListener(v -> selectTab(TAB_YESTERDAY));
        binding.tab7Days.setOnClickListener(v -> selectTab(TAB_7DAYS));

        // Khởi tạo trạng thái ban đầu
        selectTab(TAB_TODAY);

        // Hiển thị loading và đăng ký nhận dữ liệu Realtime
        binding.layoutHistoryLoading.setVisibility(View.VISIBLE);
        repository.addViolationListener(realtimeListener);
    }

    private void selectTab(int tabIndex) {
        currentTabIndex = tabIndex;

        int activeBg = R.drawable.bg_segmented_active;
        int inactiveBg = R.drawable.bg_segmented_inactive;
        int activeColor = ContextCompat.getColor(requireContext(), R.color.white);
        int inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_muted);

        binding.tabToday.setBackgroundResource(tabIndex == TAB_TODAY ? activeBg : inactiveBg);
        binding.tabToday.setTextColor(tabIndex == TAB_TODAY ? activeColor : inactiveColor);

        binding.tabYesterday.setBackgroundResource(tabIndex == TAB_YESTERDAY ? activeBg : inactiveBg);
        binding.tabYesterday.setTextColor(tabIndex == TAB_YESTERDAY ? activeColor : inactiveColor);

        binding.tab7Days.setBackgroundResource(tabIndex == TAB_7DAYS ? activeBg : inactiveBg);
        binding.tab7Days.setTextColor(tabIndex == TAB_7DAYS ? activeColor : inactiveColor);

        filterAndRenderCurrentTab();
    }

    /**
     * Lọc danh sách hồ sơ vi phạm theo mốc thời gian thực tế và render động ra màn hình.
     */
    private void filterAndRenderCurrentTab() {
        if (binding == null) return;

        // Tính toán các mốc thời gian
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfToday = cal.getTimeInMillis();

        cal.add(Calendar.DAY_OF_YEAR, -1);
        long startOfYesterday = cal.getTimeInMillis();
        long endOfYesterday = startOfToday - 1;

        cal.add(Calendar.DAY_OF_YEAR, -6);
        long startOf7Days = cal.getTimeInMillis();

        List<ViolationRecord> filtered = new ArrayList<>();
        int countToday = 0;
        int countYesterday = 0;
        int count7Days = 0;

        for (ViolationRecord rec : allRecords) {
            long time = rec.getCapturedAt() != null ? rec.getCapturedAt().getTime() : System.currentTimeMillis();
            if (time >= startOfToday) {
                countToday++;
            } else if (time >= startOfYesterday && time <= endOfYesterday) {
                countYesterday++;
            }
            if (time >= startOf7Days) {
                count7Days++;
            }

            // Lọc theo tab hiện tại
            if (currentTabIndex == TAB_TODAY && time >= startOfToday) {
                filtered.add(rec);
            } else if (currentTabIndex == TAB_YESTERDAY && time >= startOfYesterday && time <= endOfYesterday) {
                filtered.add(rec);
            } else if (currentTabIndex == TAB_7DAYS && time >= startOf7Days) {
                filtered.add(rec);
            }
        }

        // Cập nhật số lượng trên các nhãn Tab
        binding.tabToday.setText(countToday > 0 ? "Hôm nay (" + countToday + ")" : getString(R.string.guard_filter_today));
        binding.tabYesterday.setText(countYesterday > 0 ? "Hôm qua (" + countYesterday + ")" : getString(R.string.guard_filter_yesterday));
        binding.tab7Days.setText(count7Days > 0 ? "7 ngày (" + count7Days + ")" : getString(R.string.guard_filter_7days));

        // Render giao diện danh sách
        binding.layoutHistoryItems.removeAllViews();

        if (filtered.isEmpty()) {
            binding.layoutHistoryEmpty.setVisibility(View.VISIBLE);
            binding.layoutHistoryItems.setVisibility(View.GONE);

            if (currentTabIndex == TAB_TODAY) {
                binding.tvEmptyTitle.setText("Chưa có hồ sơ vi phạm hôm nay");
                binding.tvEmptyDesc.setText("Hồ sơ sau khi chụp và xác nhận sẽ tự động đồng bộ realtime tại đây.");
            } else if (currentTabIndex == TAB_YESTERDAY) {
                binding.tvEmptyTitle.setText("Không có hồ sơ vi phạm hôm qua");
                binding.tvEmptyDesc.setText("Không ghi nhận phương tiện vi phạm trong ngày hôm qua.");
            } else {
                binding.tvEmptyTitle.setText("Không có hồ sơ trong 7 ngày qua");
                binding.tvEmptyDesc.setText("Toàn bộ khuôn viên bãi xe tuân thủ tốt, không có vi phạm.");
            }
        } else {
            binding.layoutHistoryEmpty.setVisibility(View.GONE);
            binding.layoutHistoryItems.setVisibility(View.VISIBLE);

            LayoutInflater inflater = LayoutInflater.from(requireContext());
            for (ViolationRecord rec : filtered) {
                ItemGuardHistoryRecordBinding itemBinding = ItemGuardHistoryRecordBinding.inflate(
                        inflater, binding.layoutHistoryItems, false);

                // Mã hồ sơ
                itemBinding.tvHistoryRecordId.setText("MÃ: " + (rec.getId() != null ? rec.getId() : "CHƯA_RÕ"));

                // Biển số xe
                String plate = rec.getPlateFinal() != null ? rec.getPlateFinal() : "Chưa rõ biển số";
                itemBinding.tvHistoryPlate.setText(plate);

                // Thông tin vi phạm & Vị trí
                String typeStr = formatViolationType(rec.getTypeCode());
                String scopeStr = rec.getScopeId() != null && !rec.getScopeId().isEmpty() ? rec.getScopeId() : "Cổng chính";
                itemBinding.tvHistoryViolationInfo.setText(typeStr + " • " + scopeStr);

                // Thời gian ghi nhận
                Date capDate = rec.getCapturedAt() != null ? rec.getCapturedAt() : new Date();
                String timeStr = new SimpleDateFormat("HH:mm - dd/MM/yyyy", new Locale("vi", "VN")).format(capDate);
                itemBinding.tvHistoryTime.setText(timeStr);

                // Trạng thái hồ sơ thật từ Firestore
                if (rec.getStatus() == com.example.parkguardai.data.model.ViolationStatus.RESOLVED) {
                    itemBinding.badgeHistoryStatus.setText("Đã xử lý");
                    itemBinding.badgeHistoryStatus.setBackgroundResource(R.drawable.bg_badge_processed);
                    itemBinding.badgeHistoryStatus.setTextColor(0xFF16A34A);
                } else if (rec.getStatus() == com.example.parkguardai.data.model.ViolationStatus.CONFIRMED) {
                    itemBinding.badgeHistoryStatus.setText("Đã xác nhận");
                    itemBinding.badgeHistoryStatus.setBackgroundResource(R.drawable.bg_badge_warning_amber);
                    itemBinding.badgeHistoryStatus.setTextColor(0xFFD97706);
                } else {
                    itemBinding.badgeHistoryStatus.setText("Chờ xử lý");
                    itemBinding.badgeHistoryStatus.setBackgroundResource(R.drawable.bg_badge_unprocessed);
                    itemBinding.badgeHistoryStatus.setTextColor(0xFFDC2626);
                }

                // Click vào thẻ -> Chuyển sang màn hình Chi tiết hồ sơ (GuardDetailFragment)
                final String recordId = rec.getId();
                itemBinding.cardHistoryRecord.setOnClickListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("recordId", recordId);
                    try {
                        NavHostFragment.findNavController(GuardHistoryFragment.this)
                                .navigate(R.id.guardDetailFragment, bundle);
                    } catch (Exception e) {
                        try {
                            NavHostFragment.findNavController(GuardHistoryFragment.this)
                                    .navigate(R.id.guardViolationFormFragment, bundle);
                        } catch (Exception ignored) { }
                    }
                });

                binding.layoutHistoryItems.addView(itemBinding.getRoot());
            }
        }
    }

    private static String formatViolationType(String code) {
        if (code == null) return "Vi phạm quy định bãi xe";
        if (code.contains("PARKING_WRONG_AREA") || code.contains("sai khu vực")) return "Đỗ xe sai khu vực quy định";
        if (code.contains("BLOCKING_ENTRANCE") || code.contains("cứu hỏa") || code.contains("lối đi")) return "Chắn lối thoát hiểm & cứu hỏa";
        if (code.contains("NO_PARKING_ZONE") || code.contains("cấm")) return "Đỗ tại khu vực cấm đỗ";
        if (code.contains("Hàng loạt")) return code;
        return code;
    }

    @Override
    public void onDestroyView() {
        if (repository != null) {
            repository.removeViolationListener(realtimeListener);
        }
        binding = null;
        super.onDestroyView();
    }
}
