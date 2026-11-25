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
import com.example.parkguardai.data.model.AdminUserItem;
import com.example.parkguardai.data.repository.AdminRepository;
import com.example.parkguardai.databinding.FragmentAdminUsersBinding;
import com.example.parkguardai.ui.common.AvatarLoader;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình Quản lý người dùng & Nhân sự bảo vệ (Cổng Quản trị viên).
 * Hỗ trợ nghiệp vụ:
 * 1. Phân quyền và cấp tài khoản bảo vệ: Tạo mới, cập nhật ca trực/khu vực, khóa và xóa tài khoản.
 * 2. Xét duyệt tài khoản sinh viên: Tiếp nhận hồ sơ đăng ký gửi lên hệ thống, lọc sinh viên chờ duyệt,
 *    phê duyệt kích hoạt hoặc từ chối hồ sơ.
 * 3. Duy trì trạng thái tab đã chọn, tránh hiện tượng nhảy lại tab Tất cả khi reload.
 * 4. Đồng bộ thời gian thực (Realtime).
 */
public class AdminUsersFragment extends Fragment {
    private FragmentAdminUsersBinding binding;
    private AdminRepository adminRepository;

    // Giữ trạng thái tab đã chọn xuyên suốt phiên làm việc (1: Bảo vệ, 2: Sinh viên, 0: Tất cả)
    private static int sSelectedTab = 1;
    private String currentSearchQuery = "";
    private String studentStatusFilter = null; // null: Tất cả sinh viên, "PENDING": Chỉ xem chờ duyệt

    // Lắng nghe cập nhật dữ liệu Realtime từ Cloud Firestore
    private final AdminRepository.AdminDataListener realtimeListener = () -> {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            loadUsers();
            updateSummaryCounters();
        });
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adminRepository = AdminRepository.getInstance(requireContext());

        // Đăng ký nhận sự kiện cập nhật Realtime
        adminRepository.addDashboardListener(realtimeListener);

        // Liên kết thanh điều hướng đáy (Tab Người dùng)
        AdminNavigation.bind(this, binding.bottomNav, R.id.adminUsersFragment);

        // Nút quay lại
        binding.btnBack.setOnClickListener(v -> {
            if (!NavHostFragment.findNavController(this).navigateUp()) {
                requireActivity().onBackPressed();
            }
        });

        // Xử lý chuyển tab bộ lọc (Bảo vệ / Sinh viên / Tất cả)
        setupFilterTabs();

        // Xử lý ô tìm kiếm
        binding.etSearchUsers.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim() : "";
                loadUsers();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        // Bộ lọc chip phụ cho Sinh viên
        setupStudentFilterChips();

        // Nút hành động phía dưới
        setupActionButtons();

        // Khôi phục tab người dùng đang chọn trước đó (mặc định là Bảo vệ, không bao giờ ép về Tất cả)
        selectTab(sSelectedTab);
    }

    private void setupFilterTabs() {
        binding.tabGuards.setOnClickListener(v -> {
            sSelectedTab = 1;
            selectTab(1);
        });
        binding.tabStudents.setOnClickListener(v -> {
            sSelectedTab = 2;
            selectTab(2);
        });
        binding.tabAllUsers.setOnClickListener(v -> {
            sSelectedTab = 0;
            selectTab(0);
        });
    }

    private void setupStudentFilterChips() {
        binding.chipStudentAll.setOnClickListener(v -> {
            studentStatusFilter = null;
            updateStudentChipsUI();
            loadUsers();
        });

        binding.chipStudentPending.setOnClickListener(v -> {
            studentStatusFilter = "PENDING";
            updateStudentChipsUI();
            loadUsers();
        });
    }

    private void updateStudentChipsUI() {
        boolean isPendingOnly = "PENDING".equalsIgnoreCase(studentStatusFilter);
        if (isPendingOnly) {
            binding.chipStudentAll.setBackgroundResource(R.drawable.bg_segmented_inactive);
            binding.chipStudentAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));

            binding.chipStudentPending.setBackgroundResource(R.drawable.bg_segmented_active);
            binding.chipStudentPending.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        } else {
            binding.chipStudentAll.setBackgroundResource(R.drawable.bg_segmented_active);
            binding.chipStudentAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

            binding.chipStudentPending.setBackgroundResource(R.drawable.bg_segmented_inactive);
            binding.chipStudentPending.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        }
    }

    private void selectTab(int index) {
        sSelectedTab = index;

        int activeBg = R.drawable.bg_segmented_active;
        int inactiveBg = R.drawable.bg_segmented_inactive;
        int activeColor = ContextCompat.getColor(requireContext(), R.color.white);
        int inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_muted);

        // Cập nhật giao diện Segmented Tab Bar
        binding.tabGuards.setBackgroundResource(index == 1 ? activeBg : inactiveBg);
        binding.tabGuards.setTextColor(index == 1 ? activeColor : inactiveColor);

        binding.tabStudents.setBackgroundResource(index == 2 ? activeBg : inactiveBg);
        binding.tabStudents.setTextColor(index == 2 ? activeColor : inactiveColor);

        binding.tabAllUsers.setBackgroundResource(index == 0 ? activeBg : inactiveBg);
        binding.tabAllUsers.setTextColor(index == 0 ? activeColor : inactiveColor);

        // Hiển thị/ẩn Chip bộ lọc sinh viên
        if (index == 2) {
            binding.containerStudentFilterChips.setVisibility(View.VISIBLE);
            updateStudentChipsUI();
        } else {
            binding.containerStudentFilterChips.setVisibility(View.GONE);
            studentStatusFilter = null;
        }

        // Cập nhật nhãn thẻ thống kê và nút hành động theo ngữ cảnh
        if (index == 1) {
            // Tab Bảo vệ
            binding.tvSummaryLabel1.setText("Tổng nhân sự bảo vệ");
            binding.tvSummaryLabel2.setText("Đang trong ca trực");
            binding.btnAssignShift.setText("Phân ca trực");
            binding.btnCreateGuard.setText("Tạo tài khoản bảo vệ");
        } else if (index == 2) {
            // Tab Sinh viên
            binding.tvSummaryLabel1.setText("Tổng số sinh viên");
            binding.tvSummaryLabel2.setText("Chờ xét duyệt");
            binding.btnAssignShift.setText("Lọc chờ duyệt");
            binding.btnCreateGuard.setText("Duyệt tất cả");
        } else {
            // Tab Tất cả
            binding.tvSummaryLabel1.setText("Tổng người dùng");
            binding.tvSummaryLabel2.setText("Chờ xét duyệt");
            binding.btnAssignShift.setText("Phân ca trực");
            binding.btnCreateGuard.setText("Tạo tài khoản bảo vệ");
        }

        loadUsers();
        updateSummaryCounters();
    }

    private void setupActionButtons() {
        binding.btnAssignShift.setOnClickListener(v -> {
            if (sSelectedTab == 2) {
                // Tab Sinh viên -> Chuyển đổi lọc Chờ duyệt
                if ("PENDING".equalsIgnoreCase(studentStatusFilter)) {
                    studentStatusFilter = null;
                    Toast.makeText(requireContext(), "Hiển thị tất cả sinh viên", Toast.LENGTH_SHORT).show();
                } else {
                    studentStatusFilter = "PENDING";
                    Toast.makeText(requireContext(), "Đang lọc danh sách sinh viên chờ xét duyệt", Toast.LENGTH_SHORT).show();
                }
                updateStudentChipsUI();
                loadUsers();
            } else {
                Toast.makeText(requireContext(), "Bảng phân ca trực tuần đã được cập nhật tự động", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnCreateGuard.setOnClickListener(v -> {
            if (sSelectedTab == 2) {
                // Tab Sinh viên -> Duyệt nhanh toàn bộ sinh viên đang Chờ duyệt
                adminRepository.getUsersAndGuards("STUDENT", null, "PENDING").addOnSuccessListener(pendingList -> {
                    if (!isAdded()) return;
                    if (pendingList == null || pendingList.isEmpty()) {
                        Toast.makeText(requireContext(), "Hiện không có tài khoản sinh viên nào đang chờ duyệt!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<Task<Boolean>> tasks = new ArrayList<>();
                    for (AdminUserItem item : pendingList) {
                        tasks.add(adminRepository.approveStudentAccount(item.getUid()));
                    }
                    Tasks.whenAllComplete(tasks).addOnCompleteListener(all -> {
                        if (!isAdded()) return;
                        int okCount = 0;
                        for (Task<Boolean> t : tasks) {
                            if (t.isSuccessful()) okCount++;
                        }
                        Toast.makeText(requireContext(), "Đã phê duyệt " + okCount + "/" + pendingList.size() + " tài khoản sinh viên.", Toast.LENGTH_SHORT).show();
                        loadUsers();
                        updateSummaryCounters();
                    });
                }).addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Không thể tải danh sách chờ duyệt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } else {
                // Mở màn hình Tạo và cấp tài khoản bảo vệ mới
                NavHostFragment.findNavController(this).navigate(R.id.adminCreateGuardFragment);
            }
        });
    }

    private void loadUsers() {
        if (binding == null) return;

        String roleFilter = null;
        if (sSelectedTab == 1) roleFilter = "GUARD";
        else if (sSelectedTab == 2) roleFilter = "STUDENT";

        adminRepository.getUsersAndGuards(roleFilter, currentSearchQuery, studentStatusFilter).addOnSuccessListener(list -> {
            if (binding == null) return;
            renderUserList(list);
        });
    }

    private void updateSummaryCounters() {
        if (binding == null) return;

        if (sSelectedTab == 1) {
            // Thống kê Bảo vệ
            adminRepository.getUsersAndGuards("GUARD", null).addOnSuccessListener(guards -> {
                if (binding == null || guards == null) return;
                binding.tvTotalGuards.setText(String.valueOf(guards.size()));
                int active = 0;
                for (AdminUserItem g : guards) {
                    if ("ACTIVE".equalsIgnoreCase(g.getStatus())) active++;
                }
                binding.tvActiveGuards.setText(String.valueOf(active));
                binding.tvActiveGuards.setTextColor(0xFF10B981);
            });
        } else if (sSelectedTab == 2) {
            // Thống kê Sinh viên
            adminRepository.getUsersAndGuards("STUDENT", null).addOnSuccessListener(students -> {
                if (binding == null || students == null) return;
                binding.tvTotalGuards.setText(String.valueOf(students.size()));
                int pending = 0;
                for (AdminUserItem s : students) {
                    if (s.isPending()) pending++;
                }
                binding.tvActiveGuards.setText(String.valueOf(pending));
                binding.tvActiveGuards.setTextColor(pending > 0 ? 0xFFF59E0B : 0xFF10B981);

                // Cập nhật badge số lượng chờ duyệt trên tab Sinh viên nếu có
                if (pending > 0) {
                    binding.tabStudents.setText("Sinh viên (" + pending + ")");
                } else {
                    binding.tabStudents.setText("Sinh viên");
                }
            });
        } else {
            // Thống kê Tất cả
            adminRepository.getUsersAndGuards(null, null).addOnSuccessListener(allUsers -> {
                if (binding == null || allUsers == null) return;
                binding.tvTotalGuards.setText(String.valueOf(allUsers.size()));
                int pending = 0;
                for (AdminUserItem u : allUsers) {
                    if (u.isPending()) pending++;
                }
                binding.tvActiveGuards.setText(String.valueOf(pending));
                binding.tvActiveGuards.setTextColor(pending > 0 ? 0xFFF59E0B : 0xFF10B981);
            });
        }
    }

    private void renderUserList(List<AdminUserItem> users) {
        binding.guardListContainer.removeAllViews();

        if (users == null || users.isEmpty()) {
            LinearLayout emptyBox = new LinearLayout(requireContext());
            emptyBox.setOrientation(LinearLayout.VERTICAL);
            emptyBox.setGravity(Gravity.CENTER);
            emptyBox.setPadding(0, 60, 0, 60);

            ImageView emptyIcon = new ImageView(requireContext());
            emptyIcon.setImageResource(R.drawable.ic_guard_profile);
            emptyIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_muted));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                    (int) (48 * getResources().getDisplayMetrics().density),
                    (int) (48 * getResources().getDisplayMetrics().density));
            emptyBox.addView(emptyIcon, iconParams);

            TextView emptyText = new TextView(requireContext());
            emptyText.setText("Không tìm thấy người dùng phù hợp với bộ lọc hiện tại");
            emptyText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            emptyText.setTextSize(13);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, 16, 0, 0);
            emptyBox.addView(emptyText);

            binding.guardListContainer.addView(emptyBox);
            return;
        }

        for (AdminUserItem item : users) {
            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = (int) (10 * getResources().getDisplayMetrics().density);
            card.setLayoutParams(cardParams);
            card.setRadius(16 * getResources().getDisplayMetrics().density);
            card.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.outline));
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_800));
            card.setClickable(true);
            card.setFocusable(true);

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int p14 = (int) (14 * getResources().getDisplayMetrics().density);
            row.setPadding(p14, p14, p14, p14);

            // Avatar Frame với Chấm trạng thái nhỏ
            FrameLayout avatarContainer = new FrameLayout(requireContext());
            int s44 = (int) (44 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(s44, s44);
            avatarContainer.setLayoutParams(frameParams);

            FrameLayout iconFrame = new FrameLayout(requireContext());
            FrameLayout.LayoutParams iconFrameParams = new FrameLayout.LayoutParams(s44, s44);
            iconFrame.setLayoutParams(iconFrameParams);
            if (item.isGuard()) {
                iconFrame.setBackgroundResource(R.drawable.bg_kpi_icon_cyan);
            } else {
                iconFrame.setBackgroundResource(R.drawable.bg_kpi_icon_orange);
            }

            ImageView icon = new ImageView(requireContext());
            int s22 = (int) (22 * getResources().getDisplayMetrics().density);
            FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(s22, s22);
            iconParams.gravity = Gravity.CENTER;
            icon.setLayoutParams(iconParams);
            icon.setImageResource(R.drawable.ic_guard_profile);
            icon.setColorFilter(item.isGuard() ? ContextCompat.getColor(requireContext(), R.color.accent_cyan)
                                              : ContextCompat.getColor(requireContext(), R.color.warn_yellow));
            iconFrame.addView(icon);
            avatarContainer.addView(iconFrame);

            // Ảnh đại diện thực tế bo tròn (đồng bộ từ users/{uid}.avatarUrl)
            ImageView ivAvatar = new ImageView(requireContext());
            ivAvatar.setLayoutParams(new FrameLayout.LayoutParams(s44, s44));
            ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ivAvatar.setContentDescription("Ảnh đại diện " + item.getName());
            ivAvatar.setVisibility(View.GONE);
            avatarContainer.addView(ivAvatar);
            AvatarLoader.loadCircular(getActivity(), null, item.getAvatarUrl(), ivAvatar, iconFrame);

            // Chấm chỉ báo trạng thái góc Avatar
            View statusDot = new View(requireContext());
            int dotSize = (int) (11 * getResources().getDisplayMetrics().density);
            FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dotSize, dotSize);
            dotParams.gravity = Gravity.BOTTOM | Gravity.END;
            dotParams.setMargins(0, 0, 1, 1);
            statusDot.setLayoutParams(dotParams);
            if (item.isGuard()) {
                statusDot.setBackgroundResource("ACTIVE".equalsIgnoreCase(item.getStatus())
                        ? R.drawable.bg_status_dot_green : R.drawable.bg_status_dot_red);
            } else {
                statusDot.setBackgroundResource(item.isPending() ? R.drawable.bg_status_dot_amber
                        : item.isActive() ? R.drawable.bg_status_dot_green : R.drawable.bg_status_dot_red);
            }
            avatarContainer.addView(statusDot);
            row.addView(avatarContainer);

            // Cột Thông tin chi tiết
            LinearLayout infoCol = new LinearLayout(requireContext());
            infoCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            infoParams.setMarginStart((int) (12 * getResources().getDisplayMetrics().density));
            infoCol.setLayoutParams(infoParams);

            TextView tvName = new TextView(requireContext());
            tvName.setText(item.getName());
            tvName.setTextSize(15);
            tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            infoCol.addView(tvName);

            TextView tvSub = new TextView(requireContext());
            String subInfo;
            if (item.isGuard()) {
                subInfo = item.getCode() + " • " + (item.getShift() != null ? item.getShift() : "Bảo vệ");
            } else {
                subInfo = item.getCode() + " • " + (item.getShift() != null ? item.getShift() : "Sinh viên");
            }
            tvSub.setText(subInfo);
            tvSub.setTextSize(12);
            tvSub.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            infoCol.addView(tvSub);

            // Dòng SĐT hoặc phạm vi
            if (item.getPhone() != null && !item.getPhone().isEmpty()) {
                TextView tvPhone = new TextView(requireContext());
                tvPhone.setText(item.getPhone() + (item.getScope() != null ? " • " + item.getScope() : ""));
                tvPhone.setTextSize(11);
                tvPhone.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_cyan));
                tvPhone.setPadding(0, 2, 0, 0);
                infoCol.addView(tvPhone);
            }

            row.addView(infoCol);

            // Nút duyệt nhanh nếu là Sinh viên CHỜ DUYỆT
            if (item.isStudent() && item.isPending()) {
                MaterialButton btnQuickApprove = new MaterialButton(requireContext());
                btnQuickApprove.setText("Duyệt");
                btnQuickApprove.setTextSize(11);
                btnQuickApprove.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.accent_cyan));
                btnQuickApprove.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                btnQuickApprove.setCornerRadius((int) (14 * getResources().getDisplayMetrics().density));
                btnQuickApprove.setPadding(0, 0, 0, 0);
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                        (int) (64 * getResources().getDisplayMetrics().density),
                        (int) (32 * getResources().getDisplayMetrics().density));
                btnParams.setMarginEnd((int) (6 * getResources().getDisplayMetrics().density));
                btnQuickApprove.setLayoutParams(btnParams);

                btnQuickApprove.setOnClickListener(v -> {
                    btnQuickApprove.setEnabled(false);
                    adminRepository.approveStudentAccount(item.getUid()).addOnSuccessListener(ok -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Đã duyệt tài khoản cho " + item.getName(), Toast.LENGTH_SHORT).show();
                        loadUsers();
                        updateSummaryCounters();
                    }).addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        btnQuickApprove.setEnabled(true);
                        Toast.makeText(requireContext(), "Lỗi duyệt tài khoản: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                });
                row.addView(btnQuickApprove);
            }

            // Badge trạng thái
            TextView badge = new TextView(requireContext());
            badge.setTextSize(11);
            badge.setTypeface(null, android.graphics.Typeface.BOLD);
            int px = (int) (10 * getResources().getDisplayMetrics().density);
            int py = (int) (4 * getResources().getDisplayMetrics().density);
            badge.setPadding(px, py, px, py);

            if (item.isGuard()) {
                if ("ACTIVE".equalsIgnoreCase(item.getStatus())) {
                    badge.setBackgroundResource(R.drawable.bg_badge_processed);
                    badge.setTextColor(0xFF10B981);
                    badge.setText("ĐANG TRỰC");
                } else {
                    badge.setBackgroundResource(R.drawable.bg_badge_unprocessed);
                    badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red));
                    badge.setText("TẠM KHÓA");
                }
            } else {
                // Sinh viên
                if (item.isPending()) {
                    badge.setBackgroundResource(R.drawable.bg_badge_reviewing);
                    badge.setTextColor(0xFFF59E0B);
                    badge.setText("CHỜ DUYỆT");
                } else if (item.isActive()) {
                    badge.setBackgroundResource(R.drawable.bg_badge_processed);
                    badge.setTextColor(0xFF10B981);
                    badge.setText("HOẠT ĐỘNG");
                } else if (item.isRejected()) {
                    badge.setBackgroundResource(R.drawable.bg_button_dark_card);
                    badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
                    badge.setText("TỪ CHỐI");
                } else {
                    badge.setBackgroundResource(R.drawable.bg_badge_unprocessed);
                    badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red));
                    badge.setText("TẠM KHÓA");
                }
            }

            row.addView(badge);
            card.addView(row);

            card.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString(AdminUserDetailFragment.ARG_USER_ID, item.getUid());
                NavHostFragment.findNavController(this)
                        .navigate(R.id.adminUserDetailFragment, args);
            });

            binding.guardListContainer.addView(card);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Giữ nguyên tab hiện tại, chỉ làm mới danh sách và chỉ số
        loadUsers();
        updateSummaryCounters();
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
