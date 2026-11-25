package com.example.parkguardai.ui.student;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.data.datasource.CloudinaryImageDataSource;
import com.example.parkguardai.data.repository.StudentRepository;
import com.example.parkguardai.databinding.FragmentStudentEvidenceViewerBinding;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Hiển thị ảnh bằng chứng thật của vi phạm từ Cloudinary & Cloud Firestore. */
public class StudentEvidenceViewerFragment extends Fragment {
    public static final String ARG_VIOLATION_ID = "arg_violation_id";
    public static final String ARG_EVIDENCE_URL = "arg_evidence_url";
    public static final String ARG_PLATE = "arg_plate";

    private FragmentStudentEvidenceViewerBinding binding;
    private final ExecutorService imageIo = Executors.newSingleThreadExecutor();
    private final CloudinaryImageDataSource imageLoader = new CloudinaryImageDataSource();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentEvidenceViewerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.backButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigateUp());

        String violationId = getArguments() != null ? getArguments().getString(ARG_VIOLATION_ID, "") : "";
        String directUrl = getArguments() != null ? getArguments().getString(ARG_EVIDENCE_URL, "") : "";
        String directPlate = getArguments() != null ? getArguments().getString(ARG_PLATE, "") : "";

        if (!TextUtils.isEmpty(directPlate)) {
            binding.tvEvidencePlate.setText(directPlate);
        }
        if (!TextUtils.isEmpty(directUrl)) {
            loadRemoteImage(directUrl);
        }
        if (!TextUtils.isEmpty(violationId)) {
            StudentRepository.getInstance().getViolationById(violationId)
                    .addOnSuccessListener(vio -> {
                        if (binding == null || vio == null) return;
                        binding.tvEvidencePlate.setText(vio.plateNumber);
                        if (!TextUtils.isEmpty(vio.primaryEvidenceUrl)) {
                            loadRemoteImage(vio.primaryEvidenceUrl);
                        }
                        StringBuilder wm = new StringBuilder();
                        if (vio.latitude != null && vio.longitude != null) {
                            wm.append(String.format(Locale.US, "%.5f, %.5f", vio.latitude, vio.longitude));
                        }
                        if (!TextUtils.isEmpty(vio.timestamp)) {
                            if (wm.length() > 0) wm.append(" • ");
                            wm.append(vio.timestamp);
                        }
                        binding.tvEvidenceWatermark.setText(wm.toString());
                    });
        }
    }

    private void loadRemoteImage(String secureUrl) {
        imageIo.execute(() -> {
            try {
                Bitmap bmp = imageLoader.loadPreview(secureUrl);
                Activity act = getActivity();
                if (act != null) {
                    act.runOnUiThread(() -> {
                        if (binding != null) {
                            binding.evidenceImageView.setImageBitmap(bmp);
                        }
                    });
                }
            } catch (Exception ignored) { }
        });
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
