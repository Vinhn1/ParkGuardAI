package com.example.parkguardai.ui.guard;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.data.datasource.CloudinaryImageDataSource;
import com.example.parkguardai.databinding.FragmentGuardDetailBinding;
import com.example.parkguardai.ui.common.CampusMapHelper;
import com.google.firebase.firestore.DocumentSnapshot;

import org.maplibre.android.maps.MapLibreMap;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Hiển thị hồ sơ và bằng chứng ảnh thật từ Cloud Firestore & Cloudinary. */
public final class GuardDetailFragment extends Fragment {
    private FragmentGuardDetailBinding binding;
    private GuardDetailViewModel viewModel;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final CloudinaryImageDataSource images = new CloudinaryImageDataSource();
    private MapLibreMap detailMap;
    private boolean detailStyleReady;
    private Double currentLat;
    private Double currentLng;
    private String currentPlateLabel;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                             @Nullable Bundle state) {
        CampusMapHelper.ensureInitialized(requireContext());
        binding = FragmentGuardDetailBinding.inflate(inflater, parent, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        viewModel = new ViewModelProvider(this).get(GuardDetailViewModel.class);
        binding.backButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        binding.detailMapView.onCreate(state);
        CampusMapHelper.enableScrollViewTouchSupport(binding.detailMapView);
        binding.detailMapView.getMapAsync(readyMap -> {
            if (binding == null) return;
            detailMap = readyMap;
            detailMap.setStyle(CampusMapHelper.buildCampusStyle(), style -> {
                if (binding == null) return;
                detailStyleReady = true;
                updateDetailMapMarker();
            });
        });
        binding.btnOpenExternalMap.setOnClickListener(v ->
                CampusMapHelper.openInExternalMaps(requireContext(), currentLat, currentLng, currentPlateLabel));

        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        String id = getArguments() == null ? null : getArguments().getString("recordId");
        if (id == null || id.isEmpty()) {
            binding.statusText.setText(R.string.guard_detail_load_error);
            return;
        }
        viewModel.load(id);
    }

    private String[] splitPlates(String raw) {
        if (raw == null || raw.trim().isEmpty()) return new String[0];
        String cleaned = raw.replace("•", "").trim();
        String[] parts = cleaned.split("[,\\n]+");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) list.add(t);
        }
        return list.toArray(new String[0]);
    }

    private void populateBatchPlates(String[] parts) {
        if (binding == null) return;
        binding.plateText.setVisibility(View.GONE);
        binding.layoutDetailBatchPlates.removeAllViews();
        binding.layoutDetailBatchPlates.setVisibility(View.VISIBLE);
        int dp8 = Math.round(8 * getResources().getDisplayMetrics().density);
        int dp10 = Math.round(10 * getResources().getDisplayMetrics().density);
        int dp12 = Math.round(12 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < parts.length; i++) {
            android.widget.LinearLayout row = new android.widget.LinearLayout(requireContext());
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_button_dark_card);
            row.setPadding(dp12, dp10, dp12, dp10);
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.topMargin = dp8;
            row.setLayoutParams(lp);

            android.widget.TextView tvNum = new android.widget.TextView(requireContext());
            tvNum.setText(String.format(Locale.US, "#%02d", i + 1));
            tvNum.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_cyan));
            tvNum.setTextSize(12f);
            tvNum.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(tvNum);

            android.widget.TextView tvPlate = new android.widget.TextView(requireContext());
            tvPlate.setText(parts[i]);
            tvPlate.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tvPlate.setTextSize(16f);
            tvPlate.setTypeface(null, android.graphics.Typeface.BOLD);
            tvPlate.setLetterSpacing(0.06f);
            android.widget.LinearLayout.LayoutParams plateLp = new android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            plateLp.leftMargin = dp12;
            tvPlate.setLayoutParams(plateLp);
            row.addView(tvPlate);

            binding.layoutDetailBatchPlates.addView(row);
        }
    }

    private void render(GuardDetailViewModel.State state) {
        if (binding == null) return;
        if (state.loading) {
            binding.statusText.setText(R.string.guard_detail_loading);
            return;
        }
        if (state.error || state.record == null) {
            binding.statusText.setText(R.string.guard_detail_load_error);
            return;
        }

        DocumentSnapshot record = state.record;
        String status = record.getString("status");
        if ("RESOLVED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status)) {
            binding.statusText.setText("Đã xử lý");
        } else if ("CANCELLED".equalsIgnoreCase(status)) {
            binding.statusText.setText("Đã hủy");
        } else if ("APPEALED".equalsIgnoreCase(status)) {
            binding.statusText.setText("Đang khiếu nại");
        } else {
            binding.statusText.setText("Chờ xử lý");
        }

        binding.tvDetailRecordId.setText("MÃ HỒ SƠ: " + record.getId());

        String rawPlate = record.getString("plateFinal");
        String plate = (rawPlate == null || rawPlate.isEmpty())
                ? getString(R.string.guard_notice_plate_unknown) : rawPlate;
        String[] parts = splitPlates(plate);
        if (parts.length > 1) {
            binding.tvDetailVehicleCountBadge.setText(parts.length + " xe vi phạm");
            binding.plateText.setVisibility(View.GONE);
            populateBatchPlates(parts);
        } else {
            binding.tvDetailVehicleCountBadge.setText("1 xe vi phạm");
            binding.plateText.setVisibility(View.VISIBLE);
            binding.layoutDetailBatchPlates.setVisibility(View.GONE);
            binding.plateText.setText(parts.length == 1 ? parts[0] : plate);
        }

        binding.tvDetailViolationType.setText(typeLabel(record.getString("typeCode")));
        Date captured = record.getDate("capturedAt");
        if (captured != null) {
            binding.tvDetailTime.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                    new Locale("vi", "VN")).format(captured));
        }

        String scope = record.getString("scopeId");
        binding.tvDetailScope.setText(scope != null && !scope.isEmpty() ? scope : "Chưa xác định");

        Double lat = record.getDouble("latitude");
        Double lng = record.getDouble("longitude");
        Double acc = record.getDouble("locationAccuracy");
        currentLat = lat;
        currentLng = lng;
        currentPlateLabel = plate;
        updateDetailMapMarker();
        if (lat != null && lng != null) {
            String accStr = acc != null ? String.format(Locale.US, " (sai số %.0fm)", acc) : "";
            binding.tvDetailLocation.setText(String.format(Locale.US, "%.5f, %.5f%s", lat, lng, accStr));
        } else {
            binding.tvDetailLocation.setText("Không có dữ liệu GPS");
        }

        String guardName = record.getString("guardName");
        String guardUid = record.getString("guardUid");
        binding.tvDetailGuard.setText(guardName != null && !guardName.isEmpty()
                ? guardName : (guardUid != null ? guardUid : "Bảo vệ ca trực"));

        String penalty = record.getString("penaltyAmount");
        binding.tvDetailPenalty.setText(penalty != null && !penalty.isEmpty() ? penalty : "Theo quy định");
        binding.noticeText.setText(formatNotice(record));

        if (state.primary != null) {
            loadImage(state.primary.getString("secureUrl"), true);
        } else {
            loadImage(record.getString("primaryEvidenceUrl"), true);
        }
        if (state.crop != null) {
            loadImage(state.crop.getString("secureUrl"), false);
        }
    }

    private void loadImage(String url, boolean primary) {
        if (url == null || url.isEmpty()) return;
        Activity activity = getActivity();
        if (activity == null) return;
        io.execute(() -> {
            try {
                Bitmap bitmap = images.loadPreview(url);
                activity.runOnUiThread(() -> {
                    if (binding == null) return;
                    if (primary) {
                        binding.primaryImage.setImageBitmap(bitmap);
                    } else {
                        binding.cropImage.setImageBitmap(bitmap);
                        binding.cropImage.setVisibility(View.VISIBLE);
                    }
                });
            } catch (IOException failure) {
                activity.runOnUiThread(() -> {
                    if (binding != null && primary) {
                        binding.statusText.setText(R.string.guard_detail_image_error);
                    }
                });
            }
        });
    }

    private String formatNotice(DocumentSnapshot record) {
        StringBuilder notice = new StringBuilder();
        appendLine(notice, getString(R.string.guard_notice_id, record.getId()));
        appendLine(notice, getString(R.string.guard_notice_type,
                typeLabel(record.getString("typeCode"))));
        Date captured = record.getDate("capturedAt");
        if (captured != null) appendLine(notice, getString(R.string.guard_notice_time,
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                        new Locale("vi", "VN")).format(captured)));
        String scope = record.getString("scopeId");
        if (scope != null) appendLine(notice, getString(R.string.guard_notice_scope, scope));
        Double latitude = record.getDouble("latitude");
        Double longitude = record.getDouble("longitude");
        Double accuracy = record.getDouble("locationAccuracy");
        if (latitude != null && longitude != null && accuracy != null) {
            appendLine(notice, getString(R.string.guard_notice_location,
                    latitude, longitude, accuracy));
        }
        String note = record.getString("note");
        if (note != null && !note.isEmpty()) {
            appendLine(notice, getString(R.string.guard_notice_note, note));
        }
        return notice.toString();
    }

    private void appendLine(StringBuilder notice, String line) {
        if (notice.length() > 0) notice.append("\n\n");
        notice.append(line);
    }

    private String typeLabel(String code) {
        if ("PARKING_WRONG_AREA".equals(code)) return getString(R.string.guard_type_wrong_area);
        if ("BLOCKING_ENTRANCE".equals(code)) return getString(R.string.guard_type_blocking);
        if ("NO_PARKING_ZONE".equals(code)) return getString(R.string.guard_type_no_parking);
        return getString(R.string.guard_type_other);
    }

    private void updateDetailMapMarker() {
        if (detailMap == null || !detailStyleReady) return;
        CampusMapHelper.configureCampusFrame(detailMap, currentLat, currentLng, currentPlateLabel);
    }

    @Override public void onStart() {
        super.onStart();
        if (binding != null) binding.detailMapView.onStart();
    }

    @Override public void onResume() {
        super.onResume();
        if (binding != null) binding.detailMapView.onResume();
    }

    @Override public void onPause() {
        if (binding != null) binding.detailMapView.onPause();
        super.onPause();
    }

    @Override public void onStop() {
        if (binding != null) binding.detailMapView.onStop();
        super.onStop();
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) binding.detailMapView.onSaveInstanceState(outState);
    }

    @Override public void onLowMemory() {
        super.onLowMemory();
        if (binding != null) binding.detailMapView.onLowMemory();
    }

    @Override public void onDestroyView() {
        if (binding != null) binding.detailMapView.onDestroy();
        detailMap = null;
        detailStyleReady = false;
        binding = null;
        super.onDestroyView();
    }

    @Override public void onDestroy() {
        io.shutdown();
        super.onDestroy();
    }
}
