package com.example.parkguardai.ui.student;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.data.datasource.CloudinaryImageDataSource;
import com.example.parkguardai.data.model.ViolationItem;
import com.example.parkguardai.databinding.FragmentStudentViolationDetailBinding;
import com.example.parkguardai.ui.common.CampusMapHelper;

import org.maplibre.android.maps.MapLibreMap;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Màn hình Chi tiết vi phạm dành cho Sinh viên.
 * Tải biên bản vi phạm, ảnh bằng chứng Cloudinary và gửi khiếu nại trực tiếp lên Firestore.
 */
public final class StudentViolationDetailFragment extends Fragment {
    private FragmentStudentViolationDetailBinding binding;
    private StudentViewModel viewModel;
    private String violationId = "";
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final CloudinaryImageDataSource imageDataSource = new CloudinaryImageDataSource();
    private MapLibreMap campusMap;
    private boolean campusStyleReady;
    private Double currentLat;
    private Double currentLng;
    private String currentPlateLabel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        CampusMapHelper.ensureInitialized(requireContext());
        binding = FragmentStudentViolationDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StudentViewModel.class);

        binding.studentDetailMapView.onCreate(savedInstanceState);
        CampusMapHelper.enableScrollViewTouchSupport(binding.studentDetailMapView);
        binding.studentDetailMapView.getMapAsync(readyMap -> {
            if (binding == null) return;
            campusMap = readyMap;
            campusMap.setStyle(CampusMapHelper.buildCampusStyle(), style -> {
                if (binding == null) return;
                campusStyleReady = true;
                updateStudentMapMarker();
            });
        });
        binding.btnStudentOpenExternalMap.setOnClickListener(v ->
                CampusMapHelper.openInExternalMaps(requireContext(), currentLat, currentLng, currentPlateLabel));

        if (getArguments() != null && getArguments().getString("violation_id") != null) {
            violationId = getArguments().getString("violation_id");
        }

        binding.btnBack.setOnClickListener(v -> {
            if (!NavHostFragment.findNavController(this).popBackStack()) {
                requireActivity().onBackPressed();
            }
        });

        viewModel.getViolationDetail().observe(getViewLifecycleOwner(), this::bindViolationDetail);

        binding.btnSubmitComplaint.setOnClickListener(v -> {
            if (TextUtils.isEmpty(violationId)) {
                Toast.makeText(requireContext(), "Không xác định được mã biên bản vi phạm", Toast.LENGTH_SHORT).show();
                return;
            }
            String complaint = binding.etComplaint.getText() != null
                    ? binding.etComplaint.getText().toString().trim()
                    : "";

            if (TextUtils.isEmpty(complaint)) {
                binding.tilComplaint.setError("Vui lòng nhập nội dung khiếu nại giải trình");
                return;
            }

            binding.tilComplaint.setError(null);
            binding.btnSubmitComplaint.setEnabled(false);

            viewModel.submitComplaint(
                    requireContext(),
                    violationId,
                    complaint,
                    () -> {
                        if (isAdded()) {
                            binding.btnSubmitComplaint.setEnabled(true);
                            if (!NavHostFragment.findNavController(this).popBackStack()) {
                                requireActivity().onBackPressed();
                            }
                        }
                    }
            );
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty() && getContext() != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding != null) {
                binding.btnSubmitComplaint.setEnabled(!loading);
            }
        });

        if (!TextUtils.isEmpty(violationId)) {
            viewModel.loadViolationDetail(violationId);
        } else {
            binding.tvViolationId.setText("Không tìm thấy mã biên bản");
            binding.btnSubmitComplaint.setEnabled(false);
        }
    }

    private void bindViolationDetail(ViolationItem item) {
        if (binding == null || item == null || getContext() == null) return;

        binding.tvViolationId.setText("#" + item.id);
        binding.tvPlateNumber.setText(item.plateNumber);
        binding.tvViolationReason.setText(item.violationName);
        binding.tvViolationTime.setText(!item.timestamp.isEmpty() ? item.timestamp : "Chưa có thời gian");
        binding.tvViolationLocation.setText(item.location);

        currentLat = item.latitude;
        currentLng = item.longitude;
        currentPlateLabel = item.plateNumber;
        updateStudentMapMarker();

        if (item.latitude != null && item.longitude != null) {
            String accStr = item.locationAccuracy != null
                    ? String.format(Locale.US, " • Sai số %.0fm", item.locationAccuracy) : "";
            binding.tvGpsSubtitle.setText(String.format(Locale.US,
                    "Tọa độ GPS: %.5f, %.5f%s", item.latitude, item.longitude, accStr));
        } else {
            binding.tvGpsSubtitle.setText("Không có tọa độ GPS đính kèm");
        }

        if (item.isProcessed()) {
            binding.tvViolationStatus.setText(item.isCancelled() ? "Đã hủy" : "Đã xử lý");
            binding.tvViolationStatus.setBackgroundResource(R.drawable.bg_badge_processed);
            binding.tvViolationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_success));
            binding.btnSubmitComplaint.setEnabled(false);
        } else if (item.isAppealed()) {
            binding.tvViolationStatus.setText("Đang khiếu nại");
            binding.tvViolationStatus.setBackgroundResource(R.drawable.bg_badge_warning_amber);
            binding.tvViolationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.warn_yellow));
            if (!item.complaintReason.isEmpty()) {
                binding.etComplaint.setText(item.complaintReason);
            }
        } else {
            binding.tvViolationStatus.setText(R.string.student_status_unprocessed);
            binding.tvViolationStatus.setBackgroundResource(R.drawable.bg_badge_unprocessed);
            binding.tvViolationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.violation_red));
        }

        if (!TextUtils.isEmpty(item.primaryEvidenceUrl)) {
            loadEvidencePhoto(item.primaryEvidenceUrl);
        }
    }

    private void loadEvidencePhoto(String url) {
        Activity activity = getActivity();
        if (activity == null) return;
        io.execute(() -> {
            try {
                Bitmap bm = imageDataSource.loadPreview(url);
                activity.runOnUiThread(() -> {
                    if (binding != null && bm != null) {
                        binding.ivEvidencePhoto.setImageBitmap(bm);
                        binding.ivEvidencePhoto.setVisibility(View.VISIBLE);
                        binding.ivEvidencePlaceholder.setVisibility(View.GONE);
                    }
                });
            } catch (IOException ignored) { }
        });
    }

    private void updateStudentMapMarker() {
        if (campusMap == null || !campusStyleReady) return;
        CampusMapHelper.configureCampusFrame(campusMap, currentLat, currentLng, currentPlateLabel);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (binding != null) binding.studentDetailMapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) binding.studentDetailMapView.onResume();
    }

    @Override
    public void onPause() {
        if (binding != null) binding.studentDetailMapView.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {
        if (binding != null) binding.studentDetailMapView.onStop();
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) binding.studentDetailMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (binding != null) binding.studentDetailMapView.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        if (binding != null) binding.studentDetailMapView.onDestroy();
        campusMap = null;
        campusStyleReady = false;
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        io.shutdown();
        super.onDestroy();
    }
}
