package com.example.parkguardai.ui.guard;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.ai.detection.PlateDetector;
import com.example.parkguardai.ai.ocr.PlateParser;
import com.example.parkguardai.ai.ocr.PlateTextReader;
import com.example.parkguardai.data.datasource.CaptureLocationSource;
import com.example.parkguardai.data.model.GuardDraft;
import com.example.parkguardai.data.model.VehicleLookupResult;
import com.example.parkguardai.data.repository.GuardDraftStore;
import com.example.parkguardai.data.repository.GuardRepository;
import com.example.parkguardai.databinding.FragmentGuardMultiVehicleScanBinding;
import com.example.parkguardai.databinding.ItemGuardMultiVehicleBinding;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Màn hình Quét nhiều xe thực tế (CameraX + YOLO11n + ML Kit OCR + tra cứu Firestore).
 */
public final class GuardMultiVehicleScanFragment extends Fragment {
    private FragmentGuardMultiVehicleScanBinding binding;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private GuardRepository repository;
    private GuardDraftStore draftStore;
    private CaptureLocationSource locationSource;
    private PlateDetector plateDetector;
    private PlateTextReader textReader;

    private String lastDraftId;
    private long lastCaptureMillis;
    private CaptureLocationSource.Result lastLocationResult;
    private boolean pendingScanAfterLocation = false;
    private final List<VehicleLookupResult> detectedVehicles = new ArrayList<>();

    private final ActivityResultLauncher<String[]> permissionsLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (!isAdded()) return;
                Boolean cameraGranted = result.get(Manifest.permission.CAMERA);
                if (Boolean.TRUE.equals(cameraGranted)) {
                    startCameraPreview();
                } else {
                    Toast.makeText(requireContext(),
                            R.string.guard_camera_permission, Toast.LENGTH_SHORT).show();
                }
                if (locationSource != null && locationSource.hasFineLocationPermission()) {
                    locationSource.startWarmUp();
                    if (pendingScanAfterLocation) {
                        ensureGpsAndScan();
                    }
                } else if (pendingScanAfterLocation) {
                    pendingScanAfterLocation = false;
                    showOpenAppLocationSettingsDialog();
                }
            });

    private final ActivityResultLauncher<IntentSenderRequest> gpsResolutionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (!isAdded() || locationSource == null) return;
                boolean shouldScan = pendingScanAfterLocation;
                pendingScanAfterLocation = false;
                if (result.getResultCode() == Activity.RESULT_OK) {
                    locationSource.startWarmUp();
                    if (shouldScan) {
                        updateHint("Đang khóa tín hiệu GPS...");
                        locationSource.awaitInitialWarmFix(3_500L).addOnCompleteListener(t -> {
                            if (isAdded() && binding != null) scanMultiVehicles();
                        });
                    }
                } else {
                    Toast.makeText(requireContext(),
                            R.string.guard_loc_gps_disabled_message, Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<Intent> settingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (!isAdded() || locationSource == null) return;
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED && cameraProvider == null) {
                    startCameraPreview();
                }
                if (locationSource.hasFineLocationPermission()) {
                    locationSource.startWarmUp();
                    if (pendingScanAfterLocation) {
                        ensureGpsAndScan();
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGuardMultiVehicleScanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = requireContext();
        repository = GuardRepository.getInstance(context);
        draftStore = new GuardDraftStore(context);
        locationSource = new CaptureLocationSource(context);
        textReader = new PlateTextReader();

        Context appContext = context.getApplicationContext();
        ioExecutor.execute(() -> {
            try {
                plateDetector = new PlateDetector(appContext);
            } catch (Exception ignored) { }
        });

        GuardNavigation.bind(this, binding.guardBottomNav, R.id.guardCameraFragment);

        boolean hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasLocation = locationSource.hasFineLocationPermission();

        if (hasCamera) {
            startCameraPreview();
        }
        if (hasLocation) {
            locationSource.startWarmUp();
        }
        if (!hasCamera || !hasLocation) {
            permissionsLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }

        binding.btnBack.setOnClickListener(v -> {
            if (!NavHostFragment.findNavController(this).popBackStack()) {
                requireActivity().onBackPressed();
            }
        });

        binding.cameraPreviewView.setOnClickListener(v -> requestScanWithLocationCheck());

        binding.btnAllowValidVehicles.setOnClickListener(v -> allowValidVehicles());
        binding.btnHandleUnknownVehicle.setOnClickListener(v -> handleFirstUnknownVehicle());

        renderDetectedVehicles();
    }

    private void requestScanWithLocationCheck() {
        if (!isAdded() || binding == null) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            pendingScanAfterLocation = true;
            permissionsLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }
        if (!locationSource.hasFineLocationPermission()) {
            pendingScanAfterLocation = true;
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.guard_loc_perm_dialog_title)
                    .setMessage(R.string.guard_loc_perm_dialog_message)
                    .setPositiveButton(R.string.guard_loc_perm_btn_grant, (d, w) -> {
                        permissionsLauncher.launch(new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        });
                    })
                    .setNegativeButton(R.string.action_cancel, (d, w) -> pendingScanAfterLocation = false)
                    .show();
            return;
        }
        ensureGpsAndScan();
    }

    private void ensureGpsAndScan() {
        if (!isAdded() || locationSource == null) return;
        pendingScanAfterLocation = true;
        locationSource.checkLocationSettings()
                .addOnSuccessListener(res -> {
                    if (!isAdded()) return;
                    pendingScanAfterLocation = false;
                    locationSource.startWarmUp();
                    if (locationSource.hasRecentWarmFix()) {
                        scanMultiVehicles();
                    } else {
                        updateHint("Đang khóa tín hiệu GPS...");
                        locationSource.awaitInitialWarmFix(3_500L).addOnCompleteListener(t -> {
                            if (isAdded() && binding != null) scanMultiVehicles();
                        });
                    }
                })
                .addOnFailureListener(ex -> {
                    if (!isAdded()) return;
                    if (ex instanceof ResolvableApiException) {
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) ex;
                            gpsResolutionLauncher.launch(
                                    new IntentSenderRequest.Builder(resolvable.getResolution()).build());
                            return;
                        } catch (Exception ignored) { }
                    }
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.guard_loc_gps_disabled_title)
                            .setMessage(R.string.guard_loc_gps_disabled_message)
                            .setPositiveButton(R.string.guard_loc_gps_btn_turn_on, (d, w) -> {
                                settingsLauncher.launch(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            })
                            .setNegativeButton(R.string.action_cancel, (d, w) -> pendingScanAfterLocation = false)
                            .show();
                });
    }

    private void showOpenAppLocationSettingsDialog() {
        if (!isAdded()) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.guard_loc_perm_dialog_title)
                .setMessage(R.string.guard_loc_perm_settings_message)
                .setPositiveButton(R.string.guard_loc_perm_btn_open_settings, (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
                    settingsLauncher.launch(intent);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void startCameraPreview() {
        if (binding == null) return;
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                if (binding == null) return;

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.cameraPreviewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException ignored) { }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void scanMultiVehicles() {
        if (imageCapture == null || binding == null) {
            Toast.makeText(requireContext(), "Camera chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.tvCameraScanHint.setText("Đang phân tích nhiều biển số...");
        long captureNanos = SystemClock.elapsedRealtimeNanos();
        lastCaptureMillis = System.currentTimeMillis();
        lastDraftId = draftStore.newId();
        lastLocationResult = null;

        locationSource.request(captureNanos).addOnSuccessListener(res -> lastLocationResult = res);

        File originalFile = draftStore.imageFile(lastDraftId, false);
        ImageCapture.OutputFileOptions opts = new ImageCapture.OutputFileOptions.Builder(originalFile).build();
        imageCapture.takePicture(opts, ioExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Bitmap full = BitmapFactory.decodeFile(originalFile.getAbsolutePath());
                if (full == null) {
                    updateHint("Không đọc được ảnh chụp. Chạm để thử lại.");
                    return;
                }
                List<String> plates = new ArrayList<>();
                List<PlateDetector.Detection> dets = plateDetector != null
                        ? plateDetector.detect(full) : new ArrayList<>();
                for (PlateDetector.Detection d : dets) {
                    Bitmap crop = cropBitmap(full, d.box);
                    if (crop != null) {
                        try {
                            com.google.mlkit.vision.text.Text txt = Tasks.await(textReader.read(crop));
                            PlateParser.Result parsed = PlateParser.parse(txt);
                            if (parsed != null && !parsed.formattedPlate.isEmpty()
                                    && !plates.contains(parsed.formattedPlate)) {
                                plates.add(parsed.formattedPlate);
                            }
                        } catch (Exception ignored) { }
                    }
                }
                if (plates.isEmpty()) {
                    try {
                        com.google.mlkit.vision.text.Text txt = Tasks.await(textReader.read(full));
                        PlateParser.Result parsed = PlateParser.parse(txt);
                        if (parsed != null && !parsed.formattedPlate.isEmpty()) {
                            plates.add(parsed.formattedPlate);
                        }
                    } catch (Exception ignored) { }
                }

                List<VehicleLookupResult> results = new ArrayList<>();
                for (String p : plates) {
                    try {
                        VehicleLookupResult lr = Tasks.await(repository.lookupVehicle(p));
                        if (lr != null) results.add(lr);
                    } catch (Exception ignored) { }
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        detectedVehicles.clear();
                        detectedVehicles.addAll(results);
                        renderDetectedVehicles();
                        if (results.isEmpty()) {
                            updateHint("Không tìm thấy biển số. Chạm khung hình để quét lại.");
                        } else {
                            updateHint("Đã nhận diện " + results.size() + " xe. Chạm để quét lại.");
                        }
                    });
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                updateHint("Lỗi chụp ảnh. Chạm để thử lại.");
            }
        });
    }

    private void updateHint(String text) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (binding != null) binding.tvCameraScanHint.setText(text);
        });
    }

    private void renderDetectedVehicles() {
        if (binding == null || getContext() == null) return;

        int validCount = 0;
        int unregCount = 0;
        int vioCount = 0;
        for (VehicleLookupResult v : detectedVehicles) {
            if (v.isValid()) validCount++;
            else unregCount++;
            if (v.violationCount > 0) vioCount++;
        }

        binding.tvDetectedCountBadge.setText(detectedVehicles.size() + " xe");
        binding.tvCountValid.setText(String.valueOf(validCount));
        binding.tvCountUnregistered.setText(String.valueOf(unregCount));
        binding.tvCountViolations.setText(String.valueOf(vioCount));

        binding.detectedVehiclesContainer.removeAllViews();
        if (detectedVehicles.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("Chưa có xe nào được quét. Chạm vào khung hình camera phía trên để bắt đầu quét.");
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(16, 36, 16, 36);
            binding.detectedVehiclesContainer.addView(empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < detectedVehicles.size(); i++) {
            final VehicleLookupResult v = detectedVehicles.get(i);
            ItemGuardMultiVehicleBinding item = ItemGuardMultiVehicleBinding.inflate(
                    inflater, binding.detectedVehiclesContainer, false);
            item.tvItemIndex.setText(String.format(Locale.US, "#%02d", i + 1));
            item.tvItemPlate.setText(v.plate);
            item.tvItemOwnerInfo.setText(v.ownerName + " • " + v.vehicleModel);

            if (v.isValid()) {
                item.badgeItemStatus.setText(R.string.guard_plate_valid);
                item.badgeItemStatus.setBackgroundResource(R.drawable.bg_badge_processed);
                item.badgeItemStatus.setTextColor(0xFF10B981);
            } else {
                item.badgeItemStatus.setText(R.string.guard_plate_unregistered);
                item.badgeItemStatus.setBackgroundResource(R.drawable.bg_badge_warning_amber);
                item.badgeItemStatus.setTextColor(0xFFFFB525);
            }

            item.getRoot().setOnClickListener(view -> {
                if (v.isValid()) {
                    Bundle args = new Bundle();
                    args.putString("plate", v.plate);
                    NavHostFragment.findNavController(this).navigate(R.id.guardRescanAlertFragment, args);
                } else {
                    handleVehicleViolation(v.plate, "Phương tiện chưa đăng ký vào bãi");
                }
            });
            item.btnItemRemove.setVisibility(View.GONE);
            binding.detectedVehiclesContainer.addView(item.getRoot());
        }
    }

    private void allowValidVehicles() {
        List<VehicleLookupResult> validList = new ArrayList<>();
        for (VehicleLookupResult v : detectedVehicles) {
            if (v.isValid()) validList.add(v);
        }
        if (validList.isEmpty()) {
            Toast.makeText(requireContext(), "Chưa có xe hợp lệ nào trong danh sách quét", Toast.LENGTH_SHORT).show();
            return;
        }
        for (VehicleLookupResult v : validList) {
            repository.recordParkingEvent(v.plate, "ENTRY", "Cổng bãi xe");
        }
        Toast.makeText(requireContext(),
                "Đã ghi nhận lượt vào cho " + validList.size() + " xe hợp lệ vào Firestore.",
                Toast.LENGTH_LONG).show();
    }

    private void handleFirstUnknownVehicle() {
        for (VehicleLookupResult v : detectedVehicles) {
            if (!v.isValid()) {
                handleVehicleViolation(v.plate, "Phương tiện chưa đăng ký vào bãi");
                return;
            }
        }
        Toast.makeText(requireContext(), "Không có phương tiện chưa đăng ký nào trong lần quét này", Toast.LENGTH_SHORT).show();
    }

    private void handleVehicleViolation(String plate, String reason) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Phiên bảo vệ hết hạn", Toast.LENGTH_SHORT).show();
            return;
        }
        if (lastDraftId == null || lastLocationResult == null || !lastLocationResult.accepted) {
            requestScanWithLocationCheck();
            return;
        }

        GuardDraft draft = new GuardDraft();
        draft.id = lastDraftId;
        draft.ownerUid = user.getUid();
        draft.capturedAtMillis = lastCaptureMillis;
        draft.plate = plate;
        draft.originalName = lastDraftId + "_original.jpg";
        draft.latitude = lastLocationResult.latitude;
        draft.longitude = lastLocationResult.longitude;
        draft.accuracyMeters = lastLocationResult.accuracyMeters;
        draft.locationAtMillis = lastLocationResult.locationAtMillis;
        draft.locationAccepted = true;
        draft.note = reason;

        try {
            draftStore.save(draft);
        } catch (IOException ignored) { }

        Bundle args = new Bundle();
        args.putString("draftId", lastDraftId);
        try {
            NavHostFragment.findNavController(this).navigate(R.id.action_multi_to_review, args);
        } catch (Exception e) {
            NavHostFragment.findNavController(this).navigate(R.id.guardReviewFragment, args);
        }
    }

    @Nullable
    private Bitmap cropBitmap(Bitmap source, RectF box) {
        int left = Math.max(0, Math.round(box.left));
        int top = Math.max(0, Math.round(box.top));
        int width = Math.min(source.getWidth() - left, Math.round(box.width()));
        int height = Math.min(source.getHeight() - top, Math.round(box.height()));
        if (width <= 0 || height <= 0) return null;
        return Bitmap.createBitmap(source, left, top, width, height);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (locationSource != null && locationSource.hasFineLocationPermission()) {
            locationSource.startWarmUp();
        }
    }

    @Override
    public void onPause() {
        if (locationSource != null) {
            locationSource.stopWarmUp();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
        if (locationSource != null) {
            locationSource.stopWarmUp();
            locationSource.cancel();
        }
        if (textReader != null) {
            textReader.close();
            textReader = null;
        }
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        ioExecutor.execute(() -> {
            if (plateDetector != null) {
                plateDetector.close();
                plateDetector = null;
            }
        });
        ioExecutor.shutdown();
        super.onDestroy();
    }
}
