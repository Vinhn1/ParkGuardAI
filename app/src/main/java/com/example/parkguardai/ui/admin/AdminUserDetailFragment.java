package com.example.parkguardai.ui.admin;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.example.parkguardai.data.datasource.CloudinaryImageDataSource;
import com.example.parkguardai.data.model.AdminUserItem;
import com.example.parkguardai.data.model.VehicleItem;
import com.example.parkguardai.data.repository.AdminRepository;
import com.example.parkguardai.databinding.FragmentAdminUserDetailBinding;
import com.example.parkguardai.ui.common.AvatarLoader;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Màn hình Chi tiết nhân sự / hồ sơ sinh viên trong Cổng Quản trị viên.
 * Hỗ trợ:
 * 1. Sinh viên: Xem đầy đủ thông tin cá nhân, phương tiện đăng ký kèm ảnh cà vẹt & ảnh biển số/xe
 *    sinh viên cung cấp (chạm để phóng to), phê duyệt hồ sơ + xe, từ chối kèm lý do, khóa/xóa tài khoản.
 * 2. Bảo vệ: Chỉnh sửa thông tin, phân ca, khu vực, khóa, đặt lại mật khẩu và xóa tài khoản.
 */
public class AdminUserDetailFragment extends Fragment {
    public static final String ARG_USER_ID = "arg_user_id";

    private FragmentAdminUserDetailBinding binding;
    private AdminRepository adminRepository;
    private final ExecutorService imageIo = Executors.newFixedThreadPool(2);
    private final CloudinaryImageDataSource imageLoader = new CloudinaryImageDataSource();
    private String currentUserId = "";
    private AdminUserItem currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminUserDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adminRepository = AdminRepository.getInstance(requireContext());

        if (getArguments() != null && getArguments().containsKey(ARG_USER_ID)) {
            currentUserId = getArguments().getString(ARG_USER_ID, "");
        }

        binding.backButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigateUp());

        binding.resetPasswordButton.setOnClickListener(v -> handleResetPassword());
        binding.lockAccountButton.setOnClickListener(v -> handleToggleLockAccount());
        binding.btnDeleteAccount.setOnClickListener(v -> handleDeleteAccount());
        binding.btnEditGuard.setOnClickListener(v -> showEditGuardDialog());
        binding.btnApproveStudent.setOnClickListener(v -> handleApproveStudent());
        binding.btnRejectStudent.setOnClickListener(v -> handleRejectStudent());

        loadUserDetails();
    }

    private void loadUserDetails() {
        adminRepository.getUsersAndGuards(null, null).addOnSuccessListener(users -> {
            if (binding == null) return;
            if (users == null || users.isEmpty()) {
                Toast.makeText(requireContext(), "Không tìm thấy hồ sơ người dùng trên hệ thống", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).navigateUp();
                return;
            }
            currentUser = null;
            if (!TextUtils.isEmpty(currentUserId)) {
                for (AdminUserItem u : users) {
                    if (u.getUid().equals(currentUserId)) {
                        currentUser = u;
                        break;
                    }
                }
            }
            if (currentUser == null) {
                Toast.makeText(requireContext(), "Hồ sơ người dùng không còn tồn tại", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).navigateUp();
                return;
            }

            renderUser(currentUser);
            if (!currentUser.isGuard()) {
                loadStudentVehicles(currentUser.getUid());
            }
        }).addOnFailureListener(e -> {
            if (binding == null || !isAdded()) return;
            Toast.makeText(requireContext(), "Lỗi tải hồ sơ người dùng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadStudentVehicles(String uid) {
        binding.cardStudentVehicles.setVisibility(View.VISIBLE);
        binding.studentVehiclesContainer.removeAllViews();

        TextView loadingTv = new TextView(requireContext());
        loadingTv.setText("Đang tải thông tin phương tiện và hình ảnh xác minh...");
        loadingTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        loadingTv.setTextSize(12.5f);
        binding.studentVehiclesContainer.addView(loadingTv);

        adminRepository.getVehiclesForUser(uid).addOnSuccessListener(vehicles -> {
            if (binding == null || !isAdded()) return;
            renderStudentVehicles(vehicles);
        }).addOnFailureListener(e -> {
            if (binding == null || !isAdded()) return;
            binding.studentVehiclesContainer.removeAllViews();
            TextView errTv = new TextView(requireContext());
            errTv.setText("Không thể tải phương tiện của sinh viên: " + e.getMessage());
            errTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red));
            errTv.setTextSize(12.5f);
            binding.studentVehiclesContainer.addView(errTv);
        });
    }

    private void renderStudentVehicles(@Nullable List<VehicleItem> vehicles) {
        binding.studentVehiclesContainer.removeAllViews();
        int count = vehicles != null ? vehicles.size() : 0;
        binding.tvVehicleCountBadge.setText(count + " xe");
        float d = getResources().getDisplayMetrics().density;

        if (vehicles == null || vehicles.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("Sinh viên chưa gửi thông tin phương tiện hoặc hình ảnh giấy tờ.");
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            empty.setTextSize(13f);
            empty.setPadding(0, Math.round(8 * d), 0, Math.round(4 * d));
            binding.studentVehiclesContainer.addView(empty);
            return;
        }

        for (int i = 0; i < vehicles.size(); i++) {
            VehicleItem v = vehicles.get(i);

            LinearLayout vehicleBox = new LinearLayout(requireContext());
            vehicleBox.setOrientation(LinearLayout.VERTICAL);
            vehicleBox.setBackgroundResource(R.drawable.bg_card_surface);
            int pad = Math.round(14 * d);
            vehicleBox.setPadding(pad, pad, pad, pad);
            LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) boxParams.topMargin = Math.round(12 * d);
            vehicleBox.setLayoutParams(boxParams);

            // Header: Biển số + Trạng thái duyệt xe
            LinearLayout header = new LinearLayout(requireContext());
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvPlate = new TextView(requireContext());
            tvPlate.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            tvPlate.setText(v.plateNumber);
            tvPlate.setTextSize(17f);
            tvPlate.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            tvPlate.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            header.addView(tvPlate);

            TextView badge = new TextView(requireContext());
            badge.setTextSize(10.5f);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setPadding(Math.round(10 * d), Math.round(3 * d), Math.round(10 * d), Math.round(3 * d));
            if (v.isApproved()) {
                badge.setText("ĐÃ DUYỆT");
                badge.setBackgroundResource(R.drawable.bg_badge_processed);
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_success));
            } else if (v.isRejectedOrBlocked()) {
                badge.setText("TỪ CHỐI");
                badge.setBackgroundResource(R.drawable.bg_badge_unprocessed);
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red));
            } else {
                badge.setText("CHỜ DUYỆT");
                badge.setBackgroundResource(R.drawable.bg_badge_reviewing);
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.warn_yellow));
            }
            header.addView(badge);
            vehicleBox.addView(header);

            // Thông số xe: Hãng / Loại / Màu sơn
            String typeLabel = "CAR".equalsIgnoreCase(v.vehicleType) ? "Ô tô"
                    : "ELECTRIC_BIKE".equalsIgnoreCase(v.vehicleType) ? "Xe đạp điện" : "Xe máy";
            StringBuilder meta = new StringBuilder();
            meta.append(typeLabel);
            if (!TextUtils.isEmpty(v.model) && !v.model.equalsIgnoreCase(typeLabel)) {
                meta.append(" • ").append(v.model);
            }
            if (!TextUtils.isEmpty(v.color)) {
                meta.append(" • Màu ").append(v.color);
            }
            TextView tvMeta = new TextView(requireContext());
            tvMeta.setText(meta.toString());
            tvMeta.setTextSize(12.5f);
            tvMeta.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            tvMeta.setPadding(0, Math.round(4 * d), 0, 0);
            vehicleBox.addView(tvMeta);

            if (!TextUtils.isEmpty(v.submissionDate)) {
                TextView tvDate = new TextView(requireContext());
                tvDate.setText("Ngày gửi xe: " + v.submissionDate);
                tvDate.setTextSize(11.5f);
                tvDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
                tvDate.setPadding(0, Math.round(2 * d), 0, 0);
                vehicleBox.addView(tvDate);
            }

            // Hàng 2 khung hình ảnh: Ảnh cà vẹt & Ảnh xe/biển số
            LinearLayout photosRow = new LinearLayout(requireContext());
            photosRow.setOrientation(LinearLayout.HORIZONTAL);
            photosRow.setBaselineAligned(false);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.topMargin = Math.round(12 * d);
            photosRow.setLayoutParams(rowParams);

            photosRow.addView(buildPhotoCard("Ảnh giấy tờ (Cà vẹt)", v.registrationCardUrl, true));
            photosRow.addView(buildPhotoCard("Ảnh xe & Biển số", v.vehiclePhotoUrl, false));

            vehicleBox.addView(photosRow);
            binding.studentVehiclesContainer.addView(vehicleBox);
        }
    }

    private View buildPhotoCard(String title, @Nullable String imageUrl, boolean isLeft) {
        float d = getResources().getDisplayMetrics().density;

        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (isLeft) lp.setMarginEnd(Math.round(5 * d));
        else lp.setMarginStart(Math.round(5 * d));
        card.setLayoutParams(lp);
        card.setRadius(14 * d);
        card.setCardElevation(0f);
        card.setStrokeWidth(Math.round(1 * d));
        card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.outline));
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_900));

        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.VERTICAL);

        FrameLayout frame = new FrameLayout(requireContext());
        frame.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(128 * d)));
        frame.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_800));

        ImageView imageView = new ImageView(requireContext());
        imageView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setContentDescription(title);
        frame.addView(imageView);

        TextView placeholder = new TextView(requireContext());
        FrameLayout.LayoutParams phParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        placeholder.setLayoutParams(phParams);
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setPadding(Math.round(8 * d), Math.round(8 * d), Math.round(8 * d), Math.round(8 * d));
        placeholder.setTextSize(11.5f);
        placeholder.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        frame.addView(placeholder);

        boolean hasRemoteUrl = imageUrl != null && imageUrl.startsWith("https://");
        if (hasRemoteUrl) {
            placeholder.setText("Đang tải ảnh...");
            imageIo.execute(() -> {
                try {
                    Bitmap bmp = imageLoader.loadPreview(imageUrl);
                    Activity act = getActivity();
                    if (act != null) {
                        act.runOnUiThread(() -> {
                            if (binding == null || !isAdded()) return;
                            imageView.setImageBitmap(bmp);
                            placeholder.setVisibility(View.GONE);
                            card.setOnClickListener(v -> showFullImageDialog(title, bmp));
                        });
                    }
                } catch (Exception ex) {
                    Activity act = getActivity();
                    if (act != null) {
                        act.runOnUiThread(() -> {
                            if (binding == null || !isAdded()) return;
                            placeholder.setText("Không tải được ảnh");
                        });
                    }
                }
            });
        } else if (imageUrl != null && !imageUrl.isEmpty()) {
            placeholder.setText("Hồ sơ cũ chưa có tệp ảnh trên máy chủ");
        } else {
            placeholder.setText("Sinh viên chưa tải ảnh");
        }

        inner.addView(frame);

        TextView tvCaption = new TextView(requireContext());
        tvCaption.setText(title);
        tvCaption.setTextSize(11.5f);
        tvCaption.setTypeface(null, Typeface.BOLD);
        tvCaption.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        tvCaption.setGravity(Gravity.CENTER);
        tvCaption.setPadding(Math.round(6 * d), Math.round(8 * d), Math.round(6 * d), Math.round(8 * d));
        inner.addView(tvCaption);

        card.addView(inner);
        return card;
    }

    private void showFullImageDialog(String title, Bitmap bitmap) {
        if (!isAdded()) return;
        float d = getResources().getDisplayMetrics().density;
        ImageView zoomView = new ImageView(requireContext());
        zoomView.setImageBitmap(bitmap);
        zoomView.setAdjustViewBounds(true);
        zoomView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int pad = Math.round(12 * d);
        zoomView.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(zoomView)
                .setPositiveButton("Đóng", null)
                .show();
    }

    private void addInfoRow(String label, @Nullable String value) {
        float d = getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Math.round(7 * d), 0, Math.round(7 * d));

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.42f));
        tvLabel.setText(label);
        tvLabel.setTextSize(13f);
        tvLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        row.addView(tvLabel);

        boolean empty = value == null || value.trim().isEmpty();
        TextView tvValue = new TextView(requireContext());
        tvValue.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.58f));
        tvValue.setGravity(Gravity.END);
        tvValue.setText(empty ? "Chưa cập nhật" : value.trim());
        tvValue.setTextSize(13.5f);
        tvValue.setTypeface(null, empty ? Typeface.ITALIC : Typeface.BOLD);
        tvValue.setTextColor(ContextCompat.getColor(requireContext(),
                empty ? R.color.text_muted : R.color.text_primary));
        row.addView(tvValue);

        if (binding.infoRowsContainer.getChildCount() > 0) {
            View divider = new View(requireContext());
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.outline));
            binding.infoRowsContainer.addView(divider);
        }
        binding.infoRowsContainer.addView(row);
    }

    private void renderUser(AdminUserItem u) {
        binding.userNameText.setText(!TextUtils.isEmpty(u.getName()) ? u.getName() : "Chưa cập nhật tên");
        String subtitle = !TextUtils.isEmpty(u.getCode()) ? u.getCode()
                : (!TextUtils.isEmpty(u.getEmail()) ? u.getEmail() : u.getUid());
        binding.userCodeText.setText(subtitle);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String dateStr = u.getCreatedAt() > 0 ? sdf.format(new Date(u.getCreatedAt())) : "Chưa rõ";

        binding.infoRowsContainer.removeAllViews();

        // Đồng bộ ảnh đại diện người dùng (cho phép chạm phóng to để đối chiếu)
        AvatarLoader.loadCircular(getActivity(), null, u.getAvatarUrl(),
                binding.avatarPhoto, binding.avatarIcon);
        if (!TextUtils.isEmpty(u.getAvatarUrl()) && u.getAvatarUrl().startsWith("https://")) {
            String avatarUrl = u.getAvatarUrl();
            String avatarTitle = "Ảnh đại diện • " + (!TextUtils.isEmpty(u.getName()) ? u.getName() : "Người dùng");
            binding.avatarFrame.setClickable(true);
            binding.avatarFrame.setFocusable(true);
            binding.avatarFrame.setOnClickListener(v -> imageIo.execute(() -> {
                try {
                    Bitmap fullBmp = imageLoader.loadPreview(avatarUrl);
                    Activity act = getActivity();
                    if (act != null) {
                        act.runOnUiThread(() -> showFullImageDialog(avatarTitle, fullBmp));
                    }
                } catch (Exception ignored) {
                }
            }));
        } else {
            binding.avatarFrame.setOnClickListener(null);
            binding.avatarFrame.setClickable(false);
        }

        if (u.isGuard()) {
            binding.tvDetailTitle.setText("Chi tiết nhân sự bảo vệ");
            binding.userRoleBadge.setText("BẢO VỆ");
            binding.userRoleBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_cyan));
            binding.avatarFrame.setBackgroundResource(R.drawable.bg_kpi_icon_cyan);
            binding.avatarIcon.setImageResource(R.drawable.ic_guard_profile);
            binding.avatarIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.accent_cyan));

            binding.cardStudentVehicles.setVisibility(View.GONE);
            binding.cardPermissionsNote.setVisibility(View.VISIBLE);
            binding.tvPermissionsTitle.setText("Nhiệm vụ & Phân quyền bảo vệ");
            binding.tvPermissionsDesc.setText("Quét camera AI nhận diện biển số, lập hồ sơ vi phạm kèm GPS và kiểm soát bãi xe trong phạm vi được cấp: "
                    + (u.getScope() != null ? u.getScope() : "Toàn bộ khuôn viên") + ".");

            binding.btnEditGuard.setVisibility(View.VISIBLE);
            binding.btnApproveStudent.setVisibility(View.GONE);
            binding.btnRejectStudent.setVisibility(View.GONE);
            binding.lockAccountButton.setVisibility(View.VISIBLE);
            binding.btnDeleteAccount.setText("Xóa vĩnh viễn tài khoản bảo vệ");

            if ("ACTIVE".equalsIgnoreCase(u.getStatus())) {
                binding.userStatusBadge.setText("ĐANG TRỰC");
                binding.userStatusBadge.setBackgroundResource(R.drawable.bg_badge_processed);
                binding.userStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_success));
                binding.lockAccountButton.setText("Đình chỉ ca");
                binding.lockAccountButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.warn_yellow));
                binding.lockAccountButton.setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.warn_yellow));
            } else {
                binding.userStatusBadge.setText("TẠM KHÓA");
                binding.userStatusBadge.setBackgroundResource(R.drawable.bg_badge_unprocessed);
                binding.userStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red));
                binding.lockAccountButton.setText("Mở khóa");
                binding.lockAccountButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_cyan));
                binding.lockAccountButton.setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.accent_cyan));
            }

            addInfoRow("Mã nhân viên", u.getCode());
            addInfoRow("Email nội bộ", u.getEmail());
            addInfoRow("Số điện thoại", u.getPhone());
            addInfoRow("Ca trực phân công", u.getShift());
            addInfoRow("Khu vực kiểm soát", u.getScope());
            addInfoRow("Ngày cấp tài khoản", dateStr);
            addInfoRow("Trạng thái", "ACTIVE".equalsIgnoreCase(u.getStatus()) ? "Đang trong ca trực" : "Tạm đình chỉ");

        } else {
            binding.tvDetailTitle.setText("Chi tiết hồ sơ sinh viên");
            binding.userRoleBadge.setText("SINH VIÊN");
            binding.userRoleBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.warn_yellow));
            binding.avatarFrame.setBackgroundResource(R.drawable.bg_kpi_icon_orange);
            binding.avatarIcon.setImageResource(R.drawable.ic_guard_profile);
            binding.avatarIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.warn_yellow));

            binding.cardStudentVehicles.setVisibility(View.VISIBLE);
            binding.cardPermissionsNote.setVisibility(View.VISIBLE);
            binding.tvPermissionsTitle.setText("Hướng dẫn thẩm định hồ sơ");
            binding.tvPermissionsDesc.setText("Vui lòng đối chiếu họ tên, MSSV, biển số và ảnh giấy tờ sinh viên cung cấp ở trên. Khi phê duyệt, tài khoản sinh viên cùng phương tiện đang chờ duyệt sẽ được kích hoạt đồng thời.");

            binding.btnEditGuard.setVisibility(View.GONE);
            binding.btnDeleteAccount.setText("Xóa vĩnh viễn hồ sơ sinh viên");

            if (u.isPending()) {
                binding.userStatusBadge.setText("CHỜ DUYỆT");
                binding.userStatusBadge.setBackgroundResource(R.drawable.bg_badge_reviewing);
                binding.userStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.warn_yellow));

                binding.btnApproveStudent.setVisibility(View.VISIBLE);
                binding.btnApproveStudent.setText("Phê duyệt hồ sơ & Phương tiện");
                binding.btnRejectStudent.setVisibility(View.VISIBLE);
                binding.lockAccountButton.setVisibility(View.GONE);
            } else if (u.isActive()) {
                binding.userStatusBadge.setText("ĐÃ DUYỆT");
                binding.userStatusBadge.setBackgroundResource(R.drawable.bg_badge_processed);
                binding.userStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_success));

                binding.btnApproveStudent.setVisibility(View.GONE);
                binding.btnRejectStudent.setVisibility(View.GONE);
                binding.lockAccountButton.setVisibility(View.VISIBLE);
                binding.lockAccountButton.setText("Tạm khóa");
                binding.lockAccountButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.warn_yellow));
                binding.lockAccountButton.setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.warn_yellow));
            } else if (u.isRejected()) {
                binding.userStatusBadge.setText("ĐÃ TỪ CHỐI");
                binding.userStatusBadge.setBackgroundResource(R.drawable.bg_badge_unprocessed);
                binding.userStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red));

                binding.btnApproveStudent.setVisibility(View.VISIBLE);
                binding.btnApproveStudent.setText("Xem xét phê duyệt lại");
                binding.btnRejectStudent.setVisibility(View.GONE);
                binding.lockAccountButton.setVisibility(View.GONE);
            } else {
                binding.userStatusBadge.setText("TẠM KHÓA");
                binding.userStatusBadge.setBackgroundResource(R.drawable.bg_badge_unprocessed);
                binding.userStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red));

                binding.btnApproveStudent.setVisibility(View.GONE);
                binding.btnRejectStudent.setVisibility(View.GONE);
                binding.lockAccountButton.setVisibility(View.VISIBLE);
                binding.lockAccountButton.setText("Mở khóa");
                binding.lockAccountButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_cyan));
                binding.lockAccountButton.setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.accent_cyan));
            }

            String statusLabel = u.isPending() ? "Chờ quản trị viên phê duyệt"
                    : u.isActive() ? "Đã phê duyệt (Được phép đỗ xe)"
                    : u.isRejected() ? "Đã từ chối hồ sơ" : "Đang tạm khóa";

            addInfoRow("Mã số sinh viên", u.getCode());
            addInfoRow("Email sinh viên", u.getEmail());
            addInfoRow("Số điện thoại", u.getPhone());
            addInfoRow("Khoa / Lớp", "Sinh viên".equals(u.getShift()) ? "" : u.getShift());
            addInfoRow("Biển số chính", u.getScope());
            addInfoRow("Thời gian gửi", dateStr);
            addInfoRow("Trạng thái duyệt", statusLabel);
        }
    }

    private void handleApproveStudent() {
        if (currentUser == null) return;
        binding.btnApproveStudent.setEnabled(false);
        adminRepository.approveStudentAccount(currentUser.getUid()).addOnSuccessListener(ok -> {
            if (binding == null || !isAdded()) return;
            binding.btnApproveStudent.setEnabled(true);
            currentUser.setStatus("ACTIVE");
            renderUser(currentUser);
            loadStudentVehicles(currentUser.getUid());
            Snackbar.make(binding.getRoot(),
                    "Đã phê duyệt tài khoản và phương tiện của " + currentUser.getName(),
                    Snackbar.LENGTH_LONG).show();
        }).addOnFailureListener(e -> {
            if (!isAdded() || binding == null) return;
            binding.btnApproveStudent.setEnabled(true);
            Toast.makeText(requireContext(), "Không thể phê duyệt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void handleRejectStudent() {
        if (currentUser == null) return;
        float d = getResources().getDisplayMetrics().density;
        EditText reasonInput = new EditText(requireContext());
        reasonInput.setHint("Nhập lý do từ chối (vd: ảnh cà vẹt mờ, sai biển số...)");
        reasonInput.setTextSize(14f);
        int pad = Math.round(18 * d);
        FrameLayout wrapper = new FrameLayout(requireContext());
        wrapper.setPadding(pad, Math.round(8 * d), pad, 0);
        wrapper.addView(reasonInput);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Từ chối hồ sơ đăng ký")
                .setMessage("Xác nhận từ chối hồ sơ của " + currentUser.getName() + "?")
                .setView(wrapper)
                .setPositiveButton("Từ chối", (dialog, which) -> {
                    String reason = reasonInput.getText() != null && !reasonInput.getText().toString().trim().isEmpty()
                            ? reasonInput.getText().toString().trim()
                            : "Thông tin hồ sơ hoặc hình ảnh giấy tờ chưa trùng khớp";
                    adminRepository.rejectStudentAccount(currentUser.getUid(), reason).addOnSuccessListener(ok -> {
                        if (binding == null || !isAdded()) return;
                        currentUser.setStatus("REJECTED");
                        renderUser(currentUser);
                        loadStudentVehicles(currentUser.getUid());
                        Snackbar.make(binding.getRoot(), "Đã từ chối hồ sơ đăng ký.", Snackbar.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Không thể từ chối hồ sơ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void handleToggleLockAccount() {
        if (currentUser == null) return;
        boolean currentlyActive = "ACTIVE".equalsIgnoreCase(currentUser.getStatus());
        String newStatus = currentlyActive ? "LOCKED" : "ACTIVE";
        String actionTitle = currentlyActive ? "Xác nhận tạm khóa" : "Xác nhận mở khóa";
        String actionMsg = currentlyActive
                ? "Tạm ngưng quyền truy cập của " + currentUser.getName() + "?"
                : "Mở lại quyền truy cập cho " + currentUser.getName() + "?";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(actionTitle)
                .setMessage(actionMsg)
                .setPositiveButton(currentlyActive ? "Tạm khóa" : "Mở khóa", (dialog, which) -> {
                    adminRepository.setUserStatus(currentUser.getUid(), newStatus).addOnSuccessListener(ok -> {
                        if (binding == null || !isAdded()) return;
                        currentUser.setStatus(newStatus);
                        renderUser(currentUser);
                        Snackbar.make(binding.getRoot(), "Đã cập nhật trạng thái tài khoản thành công", Snackbar.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Lỗi cập nhật trạng thái: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void handleDeleteAccount() {
        if (currentUser == null) return;
        String roleText = currentUser.isGuard() ? "bảo vệ" : "sinh viên";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xác nhận xóa tài khoản")
                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn tài khoản " + roleText + " " + currentUser.getName() + "?\n\nLưu ý: Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa vĩnh viễn", (dialog, which) -> {
                    adminRepository.deleteUserAccount(currentUser.getUid()).addOnSuccessListener(ok -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Đã xóa tài khoản " + currentUser.getName() + " thành công!", Toast.LENGTH_SHORT).show();
                        NavHostFragment.findNavController(this).navigateUp();
                    }).addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Lỗi xóa tài khoản: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void handleResetPassword() {
        if (currentUser == null) return;
        String email = currentUser.getEmail();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(requireContext(), "Tài khoản này chưa có địa chỉ email để đặt lại mật khẩu.", Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Gửi email đặt lại mật khẩu")
                .setMessage("Hệ thống sẽ gửi liên kết đặt lại mật khẩu qua Firebase Authentication đến email:\n\n" + email)
                .setPositiveButton("Gửi email", (dialog, which) -> {
                    adminRepository.sendPasswordResetEmail(email)
                            .addOnSuccessListener(unused -> {
                                if (!isAdded() || binding == null) return;
                                Snackbar.make(binding.getRoot(),
                                        "Đã gửi email đặt lại mật khẩu tới " + email,
                                        Snackbar.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(),
                                        "Không thể gửi email đặt lại mật khẩu: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditGuardDialog() {
        if (currentUser == null) return;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_guard, null);
        EditText etName = dialogView.findViewById(R.id.etEditGuardName);
        EditText etCode = dialogView.findViewById(R.id.etEditGuardCode);
        EditText etPhone = dialogView.findViewById(R.id.etEditGuardPhone);
        EditText etEmail = dialogView.findViewById(R.id.etEditGuardEmail);
        EditText etScope = dialogView.findViewById(R.id.etEditGuardScope);

        TextView btnMorning = dialogView.findViewById(R.id.btnEditShiftMorning);
        TextView btnAfternoon = dialogView.findViewById(R.id.btnEditShiftAfternoon);
        TextView btnNight = dialogView.findViewById(R.id.btnEditShiftNight);

        etName.setText(currentUser.getName());
        etCode.setText(currentUser.getCode());
        etPhone.setText(currentUser.getPhone());
        etEmail.setText(currentUser.getEmail());
        etScope.setText(currentUser.getScope() != null ? currentUser.getScope() : "Cổng chính");

        final int[] selectedShift = new int[]{0};
        if (currentUser.getShift() != null) {
            if (currentUser.getShift().toLowerCase().contains("chiều")) selectedShift[0] = 1;
            else if (currentUser.getShift().toLowerCase().contains("đêm")) selectedShift[0] = 2;
        }

        Runnable updateShiftUI = () -> {
            int activeBg = R.drawable.bg_segmented_active;
            int inactiveBg = R.drawable.bg_segmented_inactive;
            int activeColor = ContextCompat.getColor(requireContext(), R.color.white);
            int inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_muted);

            btnMorning.setBackgroundResource(selectedShift[0] == 0 ? activeBg : inactiveBg);
            btnMorning.setTextColor(selectedShift[0] == 0 ? activeColor : inactiveColor);

            btnAfternoon.setBackgroundResource(selectedShift[0] == 1 ? activeBg : inactiveBg);
            btnAfternoon.setTextColor(selectedShift[0] == 1 ? activeColor : inactiveColor);

            btnNight.setBackgroundResource(selectedShift[0] == 2 ? activeBg : inactiveBg);
            btnNight.setTextColor(selectedShift[0] == 2 ? activeColor : inactiveColor);
        };
        updateShiftUI.run();

        btnMorning.setOnClickListener(v -> { selectedShift[0] = 0; updateShiftUI.run(); });
        btnAfternoon.setOnClickListener(v -> { selectedShift[0] = 1; updateShiftUI.run(); });
        btnNight.setOnClickListener(v -> { selectedShift[0] = 2; updateShiftUI.run(); });

        new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Lưu thay đổi", (dialog, which) -> {
                    String inputName = etName.getText().toString().trim();
                    String inputCode = etCode.getText().toString().trim();
                    final String newPhone = etPhone.getText().toString().trim();
                    final String newEmail = etEmail.getText().toString().trim();
                    final String newScope = etScope.getText().toString().trim();

                    final String finalName = TextUtils.isEmpty(inputName) ? currentUser.getName() : inputName;
                    final String finalCode = TextUtils.isEmpty(inputCode) ? currentUser.getCode() : inputCode;

                    final String newShift = selectedShift[0] == 0 ? "Ca sáng (06:00 - 14:00)"
                            : selectedShift[0] == 1 ? "Ca chiều (14:00 - 22:00)" : "Ca đêm (22:00 - 06:00)";

                    adminRepository.updateGuardAccount(currentUser.getUid(), finalName, finalCode,
                            newPhone, newEmail, newShift, newScope, currentUser.getStatus()).addOnSuccessListener(ok -> {
                        if (binding == null || !isAdded()) return;
                        currentUser.setName(finalName);
                        currentUser.setCode(finalCode);
                        currentUser.setPhone(newPhone);
                        currentUser.setEmail(newEmail);
                        currentUser.setShift(newShift);
                        currentUser.setScope(newScope);
                        renderUser(currentUser);
                        Snackbar.make(binding.getRoot(), "Đã cập nhật thông tin bảo vệ thành công!", Snackbar.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Lỗi cập nhật thông tin bảo vệ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        imageIo.shutdownNow();
        super.onDestroy();
    }
}
