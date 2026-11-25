package com.example.parkguardai.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.data.model.VehicleVerificationItem;
import com.example.parkguardai.data.repository.AdminRepository;
import com.example.parkguardai.databinding.FragmentAdminVerificationQueueBinding;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình Quản lý Phương tiện & Hàng đợi xét duyệt xe (Cổng Quản trị viên).
 * Đồng bộ 100% từ Cloud Firestore (vehicleVerificationRequests + vehicles + users).
 */
public class AdminVerificationQueueFragment extends Fragment {
    private FragmentAdminVerificationQueueBinding binding;
    private AdminRepository adminRepository;
    private int currentTab = 1; // 0: Đã duyệt, 1: Chờ duyệt, 2: Bị khoá
    private String currentSearchQuery = "";

    private final AdminRepository.AdminDataListener realtimeListener = () -> {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(this::loadVehicles);
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminVerificationQueueBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adminRepository = AdminRepository.getInstance(requireContext());
        adminRepository.addDashboardListener(realtimeListener);

        AdminNavigation.bind(this, binding.bottomNav, R.id.adminVerificationQueueFragment);

        binding.btnBack.setOnClickListener(v -> {
            if (!NavHostFragment.findNavController(this).navigateUp()) {
                requireActivity().onBackPressed();
            }
        });

        setupFilterTabs();

        binding.etSearchVehicle.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString() : "";
                loadVehicles();
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        binding.btnQuickApprove.setOnClickListener(v -> {
            binding.btnQuickApprove.setEnabled(false);
            adminRepository.getVerificationRequests("PENDING", null).addOnSuccessListener(pendingList -> {
                if (!isAdded() || binding == null) return;
                if (pendingList == null || pendingList.isEmpty()) {
                    binding.btnQuickApprove.setEnabled(true);
                    Toast.makeText(requireContext(), "Không có hồ sơ nào đang chờ duyệt!", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<Task<Boolean>> tasks = new ArrayList<>();
                for (VehicleVerificationItem req : pendingList) {
                    tasks.add(adminRepository.reviewVehicleRequest(req.id, true, null));
                }
                Tasks.whenAllComplete(tasks).addOnCompleteListener(all -> {
                    if (!isAdded() || binding == null) return;
                    binding.btnQuickApprove.setEnabled(true);
                    int ok = 0;
                    for (Task<Boolean> t : tasks) {
                        if (t.isSuccessful()) ok++;
                    }
                    Toast.makeText(requireContext(),
                            "Đã duyệt " + ok + "/" + pendingList.size() + " hồ sơ xe trên Firestore",
                            Toast.LENGTH_SHORT).show();
                    loadVehicles();
                });
            }).addOnFailureListener(e -> {
                if (isAdded() && binding != null) {
                    binding.btnQuickApprove.setEnabled(true);
                    Toast.makeText(requireContext(), "Lỗi tải danh sách: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        binding.btnAddVehicle.setOnClickListener(v ->
                Toast.makeText(requireContext(),
                        "Sinh viên tự đăng ký phương tiện tại Cổng Sinh viên và hồ sơ sẽ hiển thị tại đây để xét duyệt",
                        Toast.LENGTH_SHORT).show());

        selectTab(1);
    }

    private void setupFilterTabs() {
        binding.tabApproved.setOnClickListener(v -> selectTab(0));
        binding.tabPending.setOnClickListener(v -> selectTab(1));
        binding.tabBlocked.setOnClickListener(v -> selectTab(2));
    }

    private void selectTab(int index) {
        currentTab = index;
        int activeBg = R.drawable.bg_segmented_active;
        int inactiveBg = R.drawable.bg_segmented_inactive;
        int activeColor = ContextCompat.getColor(requireContext(), R.color.white);
        int inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_muted);

        binding.tabApproved.setBackgroundResource(index == 0 ? activeBg : inactiveBg);
        binding.tabApproved.setTextColor(index == 0 ? activeColor : inactiveColor);

        binding.tabPending.setBackgroundResource(index == 1 ? activeBg : inactiveBg);
        binding.tabPending.setTextColor(index == 1 ? activeColor : inactiveColor);

        binding.tabBlocked.setBackgroundResource(index == 2 ? activeBg : inactiveBg);
        binding.tabBlocked.setTextColor(index == 2 ? activeColor : inactiveColor);

        loadVehicles();
    }

    private void loadVehicles() {
        if (binding == null) return;

        String filterStatus = null;
        if (currentTab == 0) filterStatus = "APPROVED";
        else if (currentTab == 1) filterStatus = "PENDING";
        else if (currentTab == 2) filterStatus = "BLOCKED";

        adminRepository.getVerificationRequests(filterStatus, currentSearchQuery)
                .addOnSuccessListener(list -> {
                    if (binding == null) return;
                    renderVehicleList(list);
                });
    }

    private void renderVehicleList(List<VehicleVerificationItem> requests) {
        if (binding == null || getContext() == null) return;
        binding.vehicleListContainer.removeAllViews();

        if (requests == null || requests.isEmpty()) {
            TextView emptyText = new TextView(requireContext());
            emptyText.setText("Không có phương tiện nào trong danh sách lọc");
            emptyText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, 48, 0, 48);
            binding.vehicleListContainer.addView(emptyText);
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        for (VehicleVerificationItem req : requests) {
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
            iconFrame.setBackgroundResource(R.drawable.bg_kpi_icon_cyan);

            ImageView icon = new ImageView(requireContext());
            int s20 = (int) (20 * density);
            FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(s20, s20);
            iconParams.gravity = Gravity.CENTER;
            icon.setLayoutParams(iconParams);
            icon.setImageResource(R.drawable.ic_student_vehicle);
            icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.accent_cyan));
            iconFrame.addView(icon);
            row.addView(iconFrame);

            LinearLayout infoCol = new LinearLayout(requireContext());
            infoCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            infoParams.setMarginStart((int) (12 * density));
            infoCol.setLayoutParams(infoParams);

            TextView tvPlate = new TextView(requireContext());
            tvPlate.setText(req.plateNumber);
            tvPlate.setTextSize(16);
            tvPlate.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvPlate.setTypeface(null, android.graphics.Typeface.BOLD);
            infoCol.addView(tvPlate);

            TextView tvSub = new TextView(requireContext());
            tvSub.setText(req.studentName + " • " + req.vehicleModel);
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

            if (req.isBlocked()) {
                badge.setBackgroundResource(R.drawable.bg_badge_unprocessed);
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red));
                badge.setText(R.string.admin_filter_vehicle_blocked);
            } else if (req.isApproved()) {
                badge.setBackgroundResource(R.drawable.bg_badge_processed);
                badge.setTextColor(0xFF10B981);
                badge.setText(R.string.admin_filter_vehicle_approved);
            } else {
                badge.setBackgroundResource(R.drawable.bg_badge_warning_amber);
                badge.setTextColor(0xFFF59E0B);
                badge.setText(R.string.admin_filter_vehicle_pending);
            }

            row.addView(badge);
            card.addView(row);

            card.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString(AdminVerificationDetailFragment.ARG_REQUEST_ID, req.id);
                NavHostFragment.findNavController(this)
                        .navigate(R.id.adminVerificationDetailFragment, args);
            });

            binding.vehicleListContainer.addView(card);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadVehicles();
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
