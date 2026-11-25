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
import com.example.parkguardai.data.model.ViolationRecord;
import com.example.parkguardai.data.model.ViolationStatus;
import com.example.parkguardai.data.repository.GuardRepository;
import com.example.parkguardai.databinding.FragmentGuardMapBinding;
import com.example.parkguardai.ui.common.CampusMapHelper;
import com.google.android.material.button.MaterialButton;

import org.maplibre.android.annotations.Icon;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tab Bản đồ khuôn viên Trường Đại học Cửu Long dành cho Bảo vệ.
 * Lắng nghe realtime từ Cloud Firestore: khi bảo vệ chụp xe và lưu tọa độ,
 * biểu tượng cảnh báo vi phạm + banner thông báo sẽ hiển thị ngay trên bản đồ.
 */
public final class GuardMapFragment extends Fragment {
    private FragmentGuardMapBinding binding;
    private GuardRepository repository;
    private MapLibreMap map;
    private final List<ViolationRecord> allRecords = new ArrayList<>();
    private final Map<Long, String> markerRecordIds = new HashMap<>();
    private boolean styleReady;
    private boolean tileError;
    private boolean satelliteMode = false;
    @Nullable
    private String latestRecordId;

    private final GuardRepository.ViolationListener realtimeListener = records -> {
        if (!isAdded() || getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            allRecords.clear();
            if (records != null) {
                allRecords.addAll(records);
            }
            render();
        });
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle state) {
        CampusMapHelper.ensureInitialized(requireContext());
        binding = FragmentGuardMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);

        repository = GuardRepository.getInstance(requireContext());
        GuardNavigation.bind(this, binding.guardBottomNav, R.id.guardMapFragment);

        binding.mapView.onCreate(state);
        binding.mapView.addOnDidFailLoadingMapListener(message -> {
            if (binding == null) return;
            tileError = true;
            binding.statusText.setText(R.string.guard_map_tiles_error);
        });
        binding.mapView.getMapAsync(readyMap -> {
            if (binding == null) return;
            map = readyMap;
            map.setLatLngBoundsForCameraTarget(CampusMapHelper.CAMPUS_BOUNDS);
            map.setMinZoomPreference(CampusMapHelper.MIN_ZOOM);
            map.setMaxZoomPreference(CampusMapHelper.MAX_ZOOM);
            map.setOnMarkerClickListener(marker -> {
                String recordId = markerRecordIds.get(marker.getId());
                if (recordId != null) openRecord(recordId);
                return true;
            });
            map.setStyle(CampusMapHelper.buildCampusStyle(), style -> {
                if (binding == null) return;
                styleReady = true;
                tileError = false;
                render();
            });
        });

        binding.refreshButton.setOnClickListener(v -> {
            repository.notifyViolationsUpdated();
            Toast.makeText(requireContext(), "Đã cập nhật bản đồ khuôn viên", Toast.LENGTH_SHORT).show();
        });
        binding.btnToggleMapType.setOnClickListener(v -> toggleMapStyle());
        binding.cardLiveViolationBanner.setOnClickListener(v -> {
            if (latestRecordId != null) {
                openRecord(latestRecordId);
            }
        });

        repository.addViolationListener(realtimeListener);
    }

    private void toggleMapStyle() {
        if (map == null || binding == null) return;
        satelliteMode = !satelliteMode;
        styleReady = false;
        binding.btnToggleMapType.setText(satelliteMode ? "Bản đồ" : "Vệ tinh");
        map.setStyle(satelliteMode ? CampusMapHelper.buildSatelliteStyle()
                : CampusMapHelper.buildCampusStyle(), style -> {
            if (binding == null) return;
            styleReady = true;
            tileError = false;
            render();
        });
    }

    private void render() {
        if (binding == null || !isAdded()) return;

        binding.recordsContainer.removeAllViews();
        markerRecordIds.clear();
        if (map != null && styleReady) {
            map.clear();
            CampusMapHelper.drawCampusPerimeter(map);
        }

        int visible = 0;
        LatLng first = null;
        ViolationRecord latest = null;
        int dp8 = Math.round(8 * getResources().getDisplayMetrics().density);

        for (ViolationRecord record : allRecords) {
            Double latitude = record.getLatitude();
            Double longitude = record.getLongitude();
            Double accuracy = record.getLocationAccuracy();
            if (!CampusMapHelper.isValidCoordinate(latitude, longitude)) continue;

            visible++;
            LatLng position = new LatLng(latitude, longitude);
            if (first == null) {
                first = position;
                latest = record;
                latestRecordId = record.getId();
            }

            String plate = record.getPlateFinal();
            String displayPlate = (plate == null || plate.isEmpty())
                    ? getString(R.string.guard_map_unknown_plate) : plate;
            Date captured = record.getCapturedAt();
            String timeStr = captured == null ? "" : DateFormat.getTimeInstance(
                    DateFormat.SHORT, new Locale("vi", "VN")).format(captured);
            double accVal = accuracy != null ? accuracy : 0d;

            // Thẻ nút cuộn ngang bên dưới bản đồ
            MaterialButton chipBtn = new MaterialButton(requireContext());
            chipBtn.setText(String.format(Locale.US, "%s · %s (%.0fm)", displayPlate, timeStr, accVal));
            chipBtn.setTextSize(12f);
            chipBtn.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.surface_900)));
            chipBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            chipBtn.setStrokeColor(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.outline)));
            chipBtn.setStrokeWidth(Math.round(getResources().getDisplayMetrics().density));
            chipBtn.setCornerRadius(Math.round(18 * getResources().getDisplayMetrics().density));
            chipBtn.setIconResource(R.drawable.ic_alert_triangle);
            chipBtn.setIconTint(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.violation_red)));
            chipBtn.setIconSize(Math.round(16 * getResources().getDisplayMetrics().density));
            chipBtn.setOnClickListener(v -> openRecord(record.getId()));
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp8);
            chipBtn.setLayoutParams(lp);
            binding.recordsContainer.addView(chipBtn);

            // Biểu tượng cảnh báo vi phạm ngay trên bản đồ ĐH Cửu Long
            if (map != null && styleReady) {
                boolean isResolved = record.getStatus() == ViolationStatus.RESOLVED;
                Icon alertIcon = CampusMapHelper.createViolationAlertIcon(
                        requireContext(), displayPlate, isResolved);
                Marker marker = map.addMarker(new MarkerOptions()
                        .position(position)
                        .icon(alertIcon)
                        .title(displayPlate));
                markerRecordIds.put(marker.getId(), record.getId());
            }
        }

        binding.tvMapViolationCount.setText(visible + " điểm vi phạm");
        if (latest != null) {
            binding.cardLiveViolationBanner.setVisibility(View.VISIBLE);
            String latestPlate = latest.getPlateFinal() != null && !latest.getPlateFinal().isEmpty()
                    ? latest.getPlateFinal() : getString(R.string.guard_map_unknown_plate);
            String scope = latest.getScopeId() != null && !latest.getScopeId().isEmpty()
                    ? latest.getScopeId() : CampusMapHelper.CAMPUS_NAME;
            binding.tvLiveViolationTitle.setText("Cảnh báo vi phạm: " + latestPlate);
            binding.tvLiveViolationSubtitle.setText(scope + " · Chạm biểu tượng trên bản đồ để xem chi tiết");
        } else {
            binding.cardLiveViolationBanner.setVisibility(View.GONE);
            latestRecordId = null;
        }

        binding.statusText.setText(tileError
                ? getString(R.string.guard_map_tiles_error)
                : (visible == 0
                ? "Khuôn viên ĐH Cửu Long · Chưa có vi phạm"
                : "Khuôn viên ĐH Cửu Long · " + visible + " vi phạm"));

        if (map != null && styleReady) {
            LatLng cameraTarget = (first != null && CampusMapHelper.CAMPUS_BOUNDS.contains(first))
                    ? first : CampusMapHelper.CAMPUS_CENTER;
            map.setCameraPosition(new CameraPosition.Builder()
                    .target(cameraTarget)
                    .zoom(CampusMapHelper.DEFAULT_ZOOM)
                    .build());
        }
    }

    private void openRecord(@Nullable String id) {
        if (id == null || id.isEmpty() || binding == null) return;
        Bundle args = new Bundle();
        args.putString("recordId", id);
        try {
            NavHostFragment.findNavController(this).navigate(R.id.guardDetailFragment, args);
        } catch (Exception ignored) { }
    }

    @Override public void onStart() {
        super.onStart();
        if (binding != null) binding.mapView.onStart();
    }

    @Override public void onResume() {
        super.onResume();
        if (binding != null) binding.mapView.onResume();
    }

    @Override public void onPause() {
        if (binding != null) binding.mapView.onPause();
        super.onPause();
    }

    @Override public void onStop() {
        if (binding != null) binding.mapView.onStop();
        super.onStop();
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) binding.mapView.onSaveInstanceState(outState);
    }

    @Override public void onLowMemory() {
        super.onLowMemory();
        if (binding != null) binding.mapView.onLowMemory();
    }

    @Override public void onDestroyView() {
        if (repository != null) {
            repository.removeViolationListener(realtimeListener);
        }
        if (binding != null) binding.mapView.onDestroy();
        map = null;
        styleReady = false;
        markerRecordIds.clear();
        binding = null;
        super.onDestroyView();
    }
}
