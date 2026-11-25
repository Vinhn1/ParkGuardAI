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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.parkguardai.data.model.DuplicateScanCheckResult;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.parkguardai.data.model.GuardDraft;
import com.example.parkguardai.data.model.GuardSecurityAlert;
import com.example.parkguardai.data.model.VehicleLookupResult;
import com.example.parkguardai.data.repository.GuardDraftStore;
import com.example.parkguardai.data.repository.GuardRepository;
import com.example.parkguardai.databinding.FragmentGuardCameraBinding;
import com.example.parkguardai.databinding.ItemGuardMultiVehicleBinding;
import com.example.parkguardai.ui.common.ViolationNotificationHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Màn hình Quét Camera AI phân hệ Bảo vệ (Guard Portal).
 * Hỗ trợ chế độ quét 1 xe và quét nhiều xe, camera mở rộng góc nhìn toàn cảnh,
 * không gán cứng biển số từ đầu, chỉ hiển thị kết quả nhận diện và nút ghi vi phạm sau khi quét xong.
 */
public final class GuardCameraFragment extends Fragment {
    public static final int MODE_SINGLE = 1;
    public static final int MODE_MULTI = 2;

    private FragmentGuardCameraBinding binding;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private GuardRepository repository;
    private GuardDraftStore draftStore;
    private CaptureLocationSource locationSource;
    private PlateDetector plateDetector;
    private PlateTextReader textReader;

    private static final int PENDING_NONE = 0;
    private static final int PENDING_CAPTURE = 1;
    private static final int PENDING_RESET_FOR_CAPTURE = 2;

    private int currentScanMode = MODE_SINGLE;
    private String currentDraftId;
    private String currentPlate = null;
    private String manualPlateFallback = null;
    private DuplicateScanCheckResult currentDuplicateResult;
    private CaptureLocationSource.Result lastLocationResult;
    private long lastCaptureMillis = 0L;
    private boolean isLocatingForCapture = false;
    private boolean waitingLocationToRecord = false;
    private int pendingLocationAction = PENDING_NONE;
    private boolean isAnalyzing = false;
    private boolean isAppendScan = false;

    // Lưu danh sách biển số đa xe phát hiện được
    private final List<String> multiDetectedPlates = new ArrayList<>();
    private String selectedMultiPlate = null;

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
                    if (pendingLocationAction != PENDING_NONE) {
                        ensureGpsEnabledAndProceed(pendingLocationAction);
                    }
                }
            });

    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (!isAdded() || locationSource == null) return;
                if (locationSource.hasFineLocationPermission()) {
                    locationSource.startWarmUp();
                    int action = pendingLocationAction != PENDING_NONE
                            ? pendingLocationAction : PENDING_CAPTURE;
                    ensureGpsEnabledAndProceed(action);
                } else {
                    showOpenAppLocationSettingsDialog();
                }
            });

    private final ActivityResultLauncher<IntentSenderRequest> gpsResolutionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (!isAdded() || locationSource == null) return;
                int action = pendingLocationAction;
                pendingLocationAction = PENDING_NONE;
                if (result.getResultCode() == Activity.RESULT_OK) {
                    locationSource.startWarmUp();
                    executePendingLocationAction(action);
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
                    if (pendingLocationAction != PENDING_NONE) {
                        ensureGpsEnabledAndProceed(pendingLocationAction);
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGuardCameraBinding.inflate(inflater, container, false);
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

        // Khởi tạo model AI phát hiện biển số trên luồng nền
        Context appContext = context.getApplicationContext();
        ioExecutor.execute(() -> {
            try {
                plateDetector = new PlateDetector(appContext);
            } catch (Exception ignored) { }
        });

        // Liên kết Bottom Navigation Bar (Tab Quét)
        GuardNavigation.bind(this, binding.guardBottomNav, R.id.guardCameraFragment);

        if (ViolationNotificationHelper.isPushEnabled(context)
                && ViolationNotificationHelper.hasNotificationPermission(context)) {
            ViolationNotificationHelper.startSecurityAlertListener(context);
        }

        // Khởi tạo trạng thái ban đầu: Camera quét mở rộng, ẩn card kết quả và nút ghi vi phạm
        resetToScanner();

        // Kiểm tra quyền Camera & Vị trí (Android 12+ yêu cầu xin kèm FINE + COARSE)
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

        // Tạo sẵn ID bản nháp cho lần chụp hiện tại
        currentDraftId = draftStore.newId();

        // Thiết lập sự kiện chuyển chế độ: Quét 1 xe / Quét nhiều xe
        setupScanModeSwitch();

        // Nút chụp ảnh quét biển số: kiểm tra quyền Vị trí + bật GPS trước khi chụp
        binding.btnCaptureShutter.setOnClickListener(v -> requestCaptureWithLocationCheck());

        // Nút Ghi vi phạm (chỉ hiển thị sau khi quét xong)
        binding.btnRecordViolation.setOnClickListener(v -> onRecordViolationClicked());

        // Chạm dòng trạng thái GPS để kiểm tra quyền vị trí / bật GPS nhanh nếu chưa đạt chuẩn
        binding.llResultLocation.setOnClickListener(v -> {
            if (lastLocationResult == null || !lastLocationResult.accepted) {
                handleMissingLocationForViolation();
            }
        });

        // Nút Quét tiếp / Quét lại: Đưa màn hình về lại Camera Quét Rộng
        binding.btnRescan.setOnClickListener(v -> {
            manualPlateFallback = null;
            resetToScanner();
        });

        // Nút Nhập biển số thủ công: Mở BottomSheet nhập biển số
        binding.btnManualPlateEntry.setOnClickListener(v -> {
            GuardManualEntryBottomSheet bottomSheet = new GuardManualEntryBottomSheet();
            bottomSheet.setOnPlateVerifiedListener((plate, vehicleType) -> {
                manualPlateFallback = plate;
                onPlateExtracted(plate, currentDraftId, effectiveCaptureMillis(), false);
                binding.tvResultVehicleType.setText(vehicleType);
            });
            bottomSheet.show(getChildFragmentManager(), "GuardManualEntryBottomSheet");
        });

        // Nút Quét thêm xe còn thiếu (Append Scan - Giữ lại các xe đã có)
        binding.btnMultiScanMore.setOnClickListener(v -> startAppendScan());

        // Nút Nhập xe còn thiếu thủ công trong đợt quét đa xe
        binding.btnMultiAddManual.setOnClickListener(v -> openManualAddVehicle());

        // Nút Quay lại trên Banner Quét Bổ Sung
        binding.btnCancelAppendScan.setOnClickListener(v -> cancelAppendScan());
    }

    /**
     * Bắt đầu luồng Quét bổ sung (Append Scan) khi nhận diện thiếu xe:
     * Giữ nguyên danh sách các xe đã quét trước đó, mở camera để chụp tiếp góc xe thiếu.
     */
    private void startAppendScan() {
        isAppendScan = true;
        binding.scrollResultContainer.setVisibility(View.GONE);
        binding.layoutScannerView.setVisibility(View.VISIBLE);
        binding.layoutCameraControls.setVisibility(View.VISIBLE);
        binding.cardAppendScanBanner.setVisibility(View.VISIBLE);
        binding.tvAppendScanSubtitle.setText("Đang có " + multiDetectedPlates.size() +
                " xe. Hướng camera vào xe còn thiếu để chụp thêm.");
    }

    /**
     * Hủy quét bổ sung và quay lại danh sách kết quả đa xe hiện có.
     */
    private void cancelAppendScan() {
        isAppendScan = false;
        binding.cardAppendScanBanner.setVisibility(View.GONE);
        binding.layoutScannerView.setVisibility(View.GONE);
        binding.layoutCameraControls.setVisibility(View.GONE);
        binding.scrollResultContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Nhập thủ công biển số xe còn thiếu vào danh sách vi phạm của đợt quét.
     */
    private void openManualAddVehicle() {
        GuardManualEntryBottomSheet bottomSheet = new GuardManualEntryBottomSheet();
        bottomSheet.setOnPlateVerifiedListener((plate, vehicleType) -> {
            if (plate == null || plate.trim().isEmpty()) return;
            String normalized = PlateParser.normalizePlate(plate);
            if (normalized.isEmpty()) normalized = plate.toUpperCase(Locale.US).trim();

            if (multiDetectedPlates.contains(normalized)) {
                Toast.makeText(requireContext(),
                        "Biển số " + normalized + " đã có trong danh sách vi phạm.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            multiDetectedPlates.add(normalized);
            Toast.makeText(requireContext(),
                    "Đã bổ sung xe " + normalized + " vào danh sách.",
                    Toast.LENGTH_SHORT).show();
            showMultiScanResults(new ArrayList<>(multiDetectedPlates));
        });
        bottomSheet.show(getChildFragmentManager(), "GuardManualEntryBottomSheet");
    }

    /**
     * Thiết lập chuyển đổi giữa 2 chế độ: Quét 1 xe và Quét nhiều xe.
     * Chỉ đổi khung ngắm Viewfinder, KHÔNG tự động chụp hay nhảy màn hình.
     */
    private void setupScanModeSwitch() {
        binding.btnModeSingle.setOnClickListener(v -> switchScanMode(MODE_SINGLE));
        binding.btnModeMulti.setOnClickListener(v -> switchScanMode(MODE_MULTI));
    }

    private void switchScanMode(int mode) {
        currentScanMode = mode;
        if (mode == MODE_SINGLE) {
            binding.btnModeSingle.setBackgroundResource(R.drawable.bg_segmented_active);
            binding.btnModeSingle.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            binding.btnModeMulti.setBackgroundResource(R.drawable.bg_segmented_inactive);
            binding.btnModeMulti.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

            binding.viewfinderSingle.setVisibility(View.VISIBLE);
            binding.viewfinderMulti.setVisibility(View.GONE);
        } else {
            binding.btnModeMulti.setBackgroundResource(R.drawable.bg_segmented_active);
            binding.btnModeMulti.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            binding.btnModeSingle.setBackgroundResource(R.drawable.bg_segmented_inactive);
            binding.btnModeSingle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

            binding.viewfinderSingle.setVisibility(View.GONE);
            binding.viewfinderMulti.setVisibility(View.VISIBLE);
        }

        // Đưa giao diện về trạng thái camera quét mở rộng
        resetToScanner();
    }

    /**
     * Đưa màn hình về lại trạng thái Camera Quét Mở Rộng:
     * Ẩn card kết quả nhận diện, ẩn nút ghi vi phạm, hiển thị camera preview toàn cảnh.
     */
    private void resetToScanner() {
        if (binding == null) return;
        if (locationSource != null) {
            locationSource.cancel();
        }
        currentPlate = null;
        selectedMultiPlate = null;
        currentDuplicateResult = null;
        lastLocationResult = null;
        lastCaptureMillis = 0L;
        isLocatingForCapture = false;
        waitingLocationToRecord = false;
        isAnalyzing = false;
        isAppendScan = false;
        multiDetectedPlates.clear();
        if (draftStore != null) {
            currentDraftId = draftStore.newId();
        }
        binding.btnRecordViolation.setEnabled(true);
        binding.btnRecordViolation.setText(R.string.guard_btn_record_violation);
        binding.btnRescan.setText(R.string.guard_btn_rescan);

        binding.cardAppendScanBanner.setVisibility(View.GONE);
        binding.layoutScanningProgress.setVisibility(View.GONE);
        binding.scrollResultContainer.setVisibility(View.GONE);
        binding.resultCard.setVisibility(View.GONE);
        binding.resultCardMulti.setVisibility(View.GONE);
        binding.btnRecordViolation.setVisibility(View.GONE);
        binding.layoutMultiVehicleItemsContainer.removeAllViews();

        // Hiển thị khung camera quét rộng và bảng điều khiển chụp
        binding.layoutScannerView.setVisibility(View.VISIBLE);
        binding.layoutCameraControls.setVisibility(View.VISIBLE);
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

    /**
     * Kiểm tra quyền Camera + quyền Vị trí chính xác + dịch vụ GPS đang bật trước khi chụp ảnh ghi vi phạm.
     */
    private void requestCaptureWithLocationCheck() {
        if (isAnalyzing || binding == null || !isAdded()) return;
        Context context = requireContext();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            pendingLocationAction = PENDING_CAPTURE;
            permissionsLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }

        if (!locationSource.hasFineLocationPermission()) {
            pendingLocationAction = PENDING_CAPTURE;
            showRequestLocationPermissionDialog();
            return;
        }

        ensureGpsEnabledAndProceed(PENDING_CAPTURE);
    }

    private void showRequestLocationPermissionDialog() {
        if (!isAdded()) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.guard_loc_perm_dialog_title)
                .setMessage(R.string.guard_loc_perm_dialog_message)
                .setPositiveButton(R.string.guard_loc_perm_btn_grant, (d, w) -> {
                    locationPermissionLauncher.launch(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    });
                })
                .setNegativeButton(R.string.action_cancel, (d, w) -> pendingLocationAction = PENDING_NONE)
                .show();
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
                .setNegativeButton(R.string.action_cancel, (d, w) -> pendingLocationAction = PENDING_NONE)
                .show();
    }

    private void ensureGpsEnabledAndProceed(int action) {
        if (!isAdded() || locationSource == null) return;
        pendingLocationAction = action;
        locationSource.checkLocationSettings()
                .addOnSuccessListener(response -> {
                    if (!isAdded()) return;
                    pendingLocationAction = PENDING_NONE;
                    locationSource.startWarmUp();
                    executePendingLocationAction(action);
                })
                .addOnFailureListener(exception -> {
                    if (!isAdded()) return;
                    if (exception instanceof ResolvableApiException) {
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) exception;
                            IntentSenderRequest intentSenderRequest =
                                    new IntentSenderRequest.Builder(resolvable.getResolution()).build();
                            gpsResolutionLauncher.launch(intentSenderRequest);
                            return;
                        } catch (Exception ignored) { }
                    }
                    showOpenSystemGpsDialog();
                });
    }

    private void showOpenSystemGpsDialog() {
        if (!isAdded()) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.guard_loc_gps_disabled_title)
                .setMessage(R.string.guard_loc_gps_disabled_message)
                .setPositiveButton(R.string.guard_loc_gps_btn_turn_on, (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    settingsLauncher.launch(intent);
                })
                .setNegativeButton(R.string.action_cancel, (d, w) -> pendingLocationAction = PENDING_NONE)
                .show();
    }

    private void executePendingLocationAction(int action) {
        if (!isAdded() || binding == null || locationSource == null) return;
        if (action == PENDING_CAPTURE) {
            if (locationSource.hasRecentWarmFix()) {
                performCapture();
            } else {
                binding.layoutScanningProgress.setVisibility(View.VISIBLE);
                locationSource.awaitInitialWarmFix(3_500L).addOnCompleteListener(t -> {
                    if (isAdded() && binding != null) {
                        performCapture();
                    }
                });
            }
        } else if (action == PENDING_RESET_FOR_CAPTURE) {
            resetToScanner();
            Toast.makeText(requireContext(),
                    R.string.guard_capture_hint, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Bắt đầu quá trình chụp ảnh và phân tích AI sau khi đã xác minh quyền vị trí và dịch vụ GPS.
     */
    private void performCapture() {
        if (isAnalyzing || binding == null) return;
        isAnalyzing = true;
        lastLocationResult = null;
        isLocatingForCapture = true;
        waitingLocationToRecord = false;

        // Bật loading overlay
        binding.layoutScanningProgress.setVisibility(View.VISIBLE);

        long captureElapsedNanos = SystemClock.elapsedRealtimeNanos();
        long captureMillis = System.currentTimeMillis();
        lastCaptureMillis = captureMillis;
        String draftId = currentDraftId != null ? currentDraftId : draftStore.newId();
        currentDraftId = draftId;

        // Lấy vị trí GPS gắn liền với thời điểm chụp (DEC-008A) và cập nhật UI ngay khi hoàn tất
        locationSource.request(captureElapsedNanos).addOnCompleteListener(locTask -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                isLocatingForCapture = false;
                if (locTask.isSuccessful()) {
                    lastLocationResult = locTask.getResult();
                }
                refreshLocationBanner();
                if (waitingLocationToRecord && binding != null) {
                    waitingLocationToRecord = false;
                    binding.btnRecordViolation.setEnabled(true);
                    onRecordViolationClicked();
                }
            });
        });

        if (imageCapture != null) {
            File originalFile = draftStore.imageFile(draftId, false);
            ImageCapture.OutputFileOptions outputOptions =
                    new ImageCapture.OutputFileOptions.Builder(originalFile).build();

            imageCapture.takePicture(outputOptions, ioExecutor, new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    processCapturedImageFile(originalFile, draftId, captureMillis, false);
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    handleScanFailure("Không thể chụp ảnh từ camera: " + exception.getMessage());
                }
            });
        } else {
            handleScanFailure("Cảm biến Camera chưa sẵn sàng. Vui lòng thử lại.");
        }
    }

    private void handleScanFailure(String message) {
        requireActivity().runOnUiThread(() -> {
            isAnalyzing = false;
            if (binding == null) return;
            binding.layoutScanningProgress.setVisibility(View.GONE);
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    private void processCapturedImageFile(File originalFile, String draftId,
                                          long captureMillis, boolean proceedToViolation) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 1; // Giữ độ nét tối đa cho OCR
            Bitmap fullBitmap = BitmapFactory.decodeFile(originalFile.getAbsolutePath(), opts);
            if (fullBitmap == null) {
                handleScanFailure("Không đọc được tệp ảnh chụp hiện trường.");
                return;
            }

            // =========================================================================
            // CHẾ ĐỘ 1: QUÉT 1 XE (SINGLE SCAN)
            // =========================================================================
            if (currentScanMode == MODE_SINGLE) {
                processSingleVehicleScan(fullBitmap, originalFile, draftId, captureMillis, proceedToViolation);
            }
            // =========================================================================
            // CHẾ ĐỘ 2: QUÉT NHIỀU XE (MULTI SCAN)
            // =========================================================================
            else {
                processMultiVehicleScan(fullBitmap, draftId, captureMillis);
            }

        } catch (Exception e) {
            handleScanFailure("Lỗi xử lý ảnh: " + e.getMessage());
        }
    }

    /**
     * Xử lý nhận diện 1 xe: Dùng PlateDetector tìm bounding box, crop ảnh với padding an toàn,
     * chạy Google ML Kit OCR và trích xuất bằng PlateParser đã hỗ trợ sửa lỗi ký tự và biển 2 dòng.
     */
    private void processSingleVehicleScan(Bitmap fullBitmap, File originalFile, String draftId,
                                         long captureMillis, boolean proceedToViolation) {
        RectF plateBox = null;
        Bitmap cropBitmap = null;

        if (plateDetector != null) {
            List<PlateDetector.Detection> detections = plateDetector.detect(fullBitmap);
            if (!detections.isEmpty()) {
                plateBox = detections.get(0).box;
                // Thêm padding 15% x, 20% y để không bị cắt xén mất dòng dưới biển số
                cropBitmap = cropWithPadding(fullBitmap, plateBox, 0.15f, 0.20f);
            }
        }

        // Lưu ảnh crop vào draft
        File cropFile = draftStore.imageFile(draftId, true);
        if (cropBitmap != null) {
            try (FileOutputStream fos = new FileOutputStream(cropFile)) {
                cropBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
            } catch (IOException ignored) { }
        }

        // Chạy OCR trên ảnh crop trước; nếu không nhận diện được sẽ fallback chạy trên ảnh gốc
        Bitmap primarySource = cropBitmap != null ? cropBitmap : fullBitmap;
        final Bitmap fallbackSource = cropBitmap != null ? fullBitmap : null;

        textReader.read(primarySource).addOnCompleteListener(ocrTask -> {
            String detectedPlate = null;
            if (ocrTask.isSuccessful() && ocrTask.getResult() != null) {
                PlateParser.Result parsed = PlateParser.parse(ocrTask.getResult());
                if (parsed != null && !parsed.formattedPlate.isEmpty()) {
                    detectedPlate = parsed.formattedPlate;
                }
            }

            // Nếu ảnh crop chưa đọc được, thử đọc trên ảnh gốc
            if (detectedPlate == null && fallbackSource != null) {
                textReader.read(fallbackSource).addOnCompleteListener(fallbackTask -> {
                    String fallbackPlate = null;
                    if (fallbackTask.isSuccessful() && fallbackTask.getResult() != null) {
                        PlateParser.Result parsed = PlateParser.parse(fallbackTask.getResult());
                        if (parsed != null && !parsed.formattedPlate.isEmpty()) {
                            fallbackPlate = parsed.formattedPlate;
                        }
                    }

                    deliverSingleScanResult(fallbackPlate, draftId, captureMillis, proceedToViolation);
                });
            } else {
                deliverSingleScanResult(detectedPlate, draftId, captureMillis, proceedToViolation);
            }
        });
    }

    private void deliverSingleScanResult(String detectedPlate, String draftId,
                                         long captureMillis, boolean proceedToViolation) {
        requireActivity().runOnUiThread(() -> {
            if (binding == null) return;
            binding.layoutScanningProgress.setVisibility(View.GONE);
            isAnalyzing = false;

            if (detectedPlate != null && !detectedPlate.isEmpty()) {
                manualPlateFallback = null;
                onPlateExtracted(detectedPlate, draftId, captureMillis, proceedToViolation);
            } else if (manualPlateFallback != null && !manualPlateFallback.trim().isEmpty()) {
                String fallback = manualPlateFallback.trim();
                manualPlateFallback = null;
                onPlateExtracted(fallback, draftId, captureMillis, proceedToViolation);
            } else {
                Toast.makeText(requireContext(),
                        "Không nhận diện được biển số rõ nét. Vui lòng căn chỉnh lại hoặc nhập thủ công.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Xử lý quét nhiều xe thực tế: Dùng PlateDetector tìm tất cả các biển số trong ảnh.
     */
    private void processMultiVehicleScan(Bitmap fullBitmap, String draftId, long captureMillis) {
        List<PlateDetector.Detection> detections = new ArrayList<>();
        if (plateDetector != null) {
            detections = plateDetector.detect(fullBitmap);
        }

        if (detections.isEmpty()) {
            // Thử nhận diện OCR trực tiếp trên toàn bức ảnh
            textReader.read(fullBitmap).addOnCompleteListener(ocrTask -> {
                String foundPlate = null;
                if (ocrTask.isSuccessful() && ocrTask.getResult() != null) {
                    PlateParser.Result parsed = PlateParser.parse(ocrTask.getResult());
                    if (parsed != null && !parsed.formattedPlate.isEmpty()) {
                        foundPlate = parsed.formattedPlate;
                    }
                }

                final String finalFound = foundPlate;
                requireActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    binding.layoutScanningProgress.setVisibility(View.GONE);
                    isAnalyzing = false;

                    if (finalFound != null) {
                        handleMultiScanRecognitionResult(java.util.Collections.singletonList(finalFound));
                    } else {
                        Toast.makeText(requireContext(),
                                isAppendScan ? "Chưa nhận diện thêm được xe nào. Vui lòng căn chỉnh lại góc hoặc nhập thủ công."
                                        : "Không tìm thấy biển số xe nào trong khung hình. Vui lòng thử lại.",
                                Toast.LENGTH_SHORT).show();
                        if (isAppendScan) {
                            cancelAppendScan();
                        }
                    }
                });
            });
            return;
        }

        // Với mỗi detection, crop và OCR
        final List<PlateDetector.Detection> finalDetections = detections;
        ioExecutor.execute(() -> {
            List<String> recognizedPlates = new ArrayList<>();
            for (PlateDetector.Detection d : finalDetections) {
                Bitmap crop = cropWithPadding(fullBitmap, d.box, 0.15f, 0.20f);
                if (crop != null) {
                    try {
                        com.google.android.gms.tasks.Task<com.google.mlkit.vision.text.Text> task = textReader.read(crop);
                        com.google.mlkit.vision.text.Text text = com.google.android.gms.tasks.Tasks.await(task);
                        PlateParser.Result parsed = PlateParser.parse(text);
                        if (parsed != null && !parsed.formattedPlate.isEmpty()) {
                            if (!recognizedPlates.contains(parsed.formattedPlate)) {
                                recognizedPlates.add(parsed.formattedPlate);
                            }
                        }
                    } catch (Exception ignored) { }
                }
            }

            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                binding.layoutScanningProgress.setVisibility(View.GONE);
                isAnalyzing = false;

                if (!recognizedPlates.isEmpty()) {
                    handleMultiScanRecognitionResult(recognizedPlates);
                } else {
                    Toast.makeText(requireContext(),
                            isAppendScan ? "Chưa đọc rõ biển số xe thiếu. Vui lòng thử lại hoặc bấm '+ Nhập xe thiếu'."
                                    : "Phát hiện vùng biển số nhưng chưa đọc được ký tự. Vui lòng đưa máy lại gần hơn.",
                            Toast.LENGTH_LONG).show();
                    if (isAppendScan) {
                        cancelAppendScan();
                    }
                }
            });
        });
    }

    /**
     * Xử lý kết quả nhận diện đa xe (phân biệt giữa Quét ban đầu và Quét bổ sung - Append Scan).
     */
    private void handleMultiScanRecognitionResult(List<String> recognizedPlates) {
        if (isAppendScan) {
            List<String> newPlates = new ArrayList<>();
            for (String p : recognizedPlates) {
                if (!multiDetectedPlates.contains(p)) {
                    newPlates.add(p);
                }
            }

            if (!newPlates.isEmpty()) {
                multiDetectedPlates.addAll(newPlates);
                Toast.makeText(requireContext(),
                        "Đã nhận diện thêm " + newPlates.size() + " xe mới! (Tổng: " + multiDetectedPlates.size() + " xe)",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(),
                        "Các xe trong ảnh này đã có trong danh sách. Vui lòng thử lại hoặc bấm '+ Nhập xe thiếu'.",
                        Toast.LENGTH_SHORT).show();
            }
            isAppendScan = false;
            binding.cardAppendScanBanner.setVisibility(View.GONE);
            showMultiScanResults(new ArrayList<>(multiDetectedPlates));
        } else {
            showMultiScanResults(recognizedPlates);
        }
    }

    /**
     * Xử lý sau khi đã nhận diện thành công biển số 1 xe:
     * CHỈ KHI NÀY MỚI HIỂN THỊ KẾT QUẢ BIỂN SỐ VÀ NÚT GHI VI PHẠM!
     */
    private void onPlateExtracted(String plate, String draftId,
                                  long captureMillis, boolean proceedToViolation) {
        isAnalyzing = false;
        currentPlate = plate;
        if (binding == null) return;

        // Tắt loading
        binding.layoutScanningProgress.setVisibility(View.GONE);

        // Chuyển sang hiển thị giao diện kết quả 1 xe
        binding.layoutScannerView.setVisibility(View.GONE);
        binding.scrollResultContainer.setVisibility(View.VISIBLE);
        binding.resultCard.setVisibility(View.VISIBLE);
        binding.resultCardMulti.setVisibility(View.GONE);

        // HIỂN THỊ BIỂN SỐ NHẬN DIỆN VÀ NÚT GHI VI PHẠM
        binding.tvResultPlate.setText(plate);
        binding.btnRecordViolation.setVisibility(View.VISIBLE);

        // Hiển thị thumbnail ảnh cắt nếu có
        File cropFile = draftStore.imageFile(draftId, true);
        if (cropFile.exists()) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 2;
            Bitmap cropBm = BitmapFactory.decodeFile(cropFile.getAbsolutePath(), opts);
            if (cropBm != null) {
                binding.ivResultThumbnail.setImageBitmap(cropBm);
            }
        }

        // Cập nhật thông tin chủ xe & kiểm tra quét lặp từ repository
        updateVehicleInfo(plate);

        if (proceedToViolation) {
            proceedToRecordViolation(plate, draftId, captureMillis);
        }
    }

    /**
     * Hiển thị kết quả quét Đa xe thực tế:
     * Dựng danh sách thẻ xe động linh hoạt (1, 2, 3, 4, 5+ xe), hỗ trợ quét thêm hoặc nhập thủ công.
     */
    private void showMultiScanResults(List<String> plates) {
        isAnalyzing = false;
        if (binding == null) return;

        binding.cardAppendScanBanner.setVisibility(View.GONE);
        binding.layoutScannerView.setVisibility(View.GONE);
        binding.scrollResultContainer.setVisibility(View.VISIBLE);
        binding.resultCard.setVisibility(View.GONE);
        binding.resultCardMulti.setVisibility(View.VISIBLE);

        if (plates != multiDetectedPlates) {
            multiDetectedPlates.clear();
            multiDetectedPlates.addAll(plates);
        }

        binding.layoutMultiVehicleItemsContainer.removeAllViews();
        int count = multiDetectedPlates.size();
        binding.tvMultiCountBadge.setText(count + " xe phát hiện");

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < multiDetectedPlates.size(); i++) {
            final int index = i;
            final String plate = multiDetectedPlates.get(i);

            ItemGuardMultiVehicleBinding itemBinding = ItemGuardMultiVehicleBinding.inflate(
                    inflater, binding.layoutMultiVehicleItemsContainer, false);

            itemBinding.tvItemIndex.setText(String.format(Locale.US, "#%02d", i + 1));
            itemBinding.tvItemPlate.setText(plate);

            // Tra cứu thông tin chủ xe và trạng thái
            loadMultiVehicleData(plate, itemBinding.tvItemOwnerInfo, itemBinding.badgeItemStatus);

            // Nút xóa xe khỏi danh sách vi phạm
            itemBinding.btnItemRemove.setOnClickListener(v -> {
                multiDetectedPlates.remove(index);
                if (multiDetectedPlates.isEmpty()) {
                    resetToScanner();
                } else {
                    showMultiScanResults(new ArrayList<>(multiDetectedPlates));
                }
            });

            binding.layoutMultiVehicleItemsContainer.addView(itemBinding.getRoot());
        }

        // Cập nhật nút chính Lập hồ sơ vi phạm
        binding.btnRecordViolation.setVisibility(View.VISIBLE);
        if (count > 1) {
            binding.btnRecordViolation.setText("Lập hồ sơ vi phạm cả " + count + " xe");
        } else if (count == 1) {
            binding.btnRecordViolation.setText("Lập hồ sơ vi phạm (" + multiDetectedPlates.get(0) + ")");
        }
        binding.btnRescan.setText("Quét lại từ đầu");
    }

    private void loadMultiVehicleData(String plate, android.widget.TextView tvOwner, android.widget.TextView badge) {
        repository.lookupVehicle(plate).addOnCompleteListener(t -> {
            if (binding == null || !t.isSuccessful() || t.getResult() == null) return;
            VehicleLookupResult v = t.getResult();
            tvOwner.setText(v.ownerName + " • " + v.vehicleModel);

            if (v.isValid()) {
                badge.setText(R.string.guard_plate_valid);
                badge.setBackgroundResource(R.drawable.bg_badge_processed);
                badge.setTextColor(0xFF10B981);
            } else {
                badge.setText(R.string.guard_plate_unregistered);
                badge.setBackgroundResource(R.drawable.bg_badge_warning_amber);
                badge.setTextColor(0xFFFFB525);

                // Tự động ghi nhận cảnh báo an ninh xe lạ cho ca trực
                String alertId = "ALT-" + (int)(1000 + Math.random() * 9000);
                String timeStr = new SimpleDateFormat("HH:mm - dd/MM", new Locale("vi", "VN")).format(new Date());
                repository.recordSecurityAlert(new GuardSecurityAlert(
                        alertId,
                        plate,
                        GuardSecurityAlert.AlertType.UNKNOWN_VEHICLE,
                        "Xe lạ chưa đăng ký: " + plate,
                        "Phát hiện phương tiện chưa đăng ký thông tin sinh viên/cán bộ trong khu vực bãi xe.",
                        GuardSecurityAlert.Severity.MEDIUM,
                        timeStr,
                        "Hiện trường quét đa xe",
                        GuardSecurityAlert.Status.UNHANDLED
                ));
            }
        });
    }

    private void refreshLocationBanner() {
        if (binding == null) return;
        if (lastLocationResult != null && lastLocationResult.accepted) {
            binding.tvResultLocation.setText(String.format(Locale.US,
                    "%.5f, %.5f · sai số %.0f m (Đạt chuẩn DEC-008A)",
                    lastLocationResult.latitude, lastLocationResult.longitude, lastLocationResult.accuracyMeters));
            binding.tvResultLocation.setTextColor(0xFF10B981);
        } else if (isLocatingForCapture) {
            binding.tvResultLocation.setText(R.string.guard_loc_acquiring);
            binding.tvResultLocation.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.accent_cyan));
        } else {
            binding.tvResultLocation.setText(
                    "Chưa có tín hiệu GPS đạt ngưỡng (<= 30m). Vui lòng bật GPS ở nơi thoáng và chụp lại.");
            binding.tvResultLocation.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.accent_cyan));
        }
    }

    /**
     * Tra cứu thông tin phương tiện và kiểm tra quét lặp trong ngày
     */
    private void updateVehicleInfo(String plate) {
        if (binding == null) return;

        // Cập nhật vị trí hiện trường theo chuẩn DEC-008A (không dùng tọa độ giả)
        refreshLocationBanner();

        repository.lookupVehicle(plate).addOnCompleteListener(task -> {
            if (binding == null || !task.isSuccessful() || task.getResult() == null) return;
            VehicleLookupResult v = task.getResult();
            binding.tvResultOwnerName.setText(v.ownerName);
            binding.tvResultVehicleType.setText(v.vehicleModel);

            // Kiểm tra quét lặp trong ngày
            repository.checkDuplicateScan(plate).addOnCompleteListener(dupTask -> {
                if (binding == null || !dupTask.isSuccessful() || dupTask.getResult() == null) return;
                currentDuplicateResult = dupTask.getResult();

                if (currentDuplicateResult.isDuplicate) {
                    binding.badgePlateStatus.setText(
                            "Quét lại (" + currentDuplicateResult.scanCountToday + " lần)");
                    binding.badgePlateStatus.setBackgroundResource(R.drawable.bg_badge_warning_amber);
                    binding.badgePlateStatus.setTextColor(0xFFFFB525);

                    binding.tvResultViolationTitle.setText("Cảnh báo quét lặp (" + currentDuplicateResult.scanCountToday + " lần trong ngày)");
                    binding.tvResultViolationTitle.setTextColor(0xFFFFB525);
                    binding.ivViolationIcon.setColorFilter(0xFFFFB525);
                    binding.tvResultViolationDesc.setText("Phương tiện xuất hiện nhiều lần trong ngày. Nghi vấn đỗ xe quá giờ hoặc sai vị trí.");

                    // Tự động ghi nhận cảnh báo an ninh quét lặp nhiều lần (Mức độ Khẩn cấp)
                    String alertId = "ALT-" + (int)(1000 + Math.random() * 9000);
                    String timeStr = new SimpleDateFormat("HH:mm", new Locale("vi", "VN")).format(new Date()) + " Hôm nay";
                    repository.recordSecurityAlert(new GuardSecurityAlert(
                            alertId,
                            plate,
                            GuardSecurityAlert.AlertType.REPEATED_SCAN,
                            "Quét lặp " + currentDuplicateResult.scanCountToday + " lần: " + plate,
                            "Phương tiện quét lại nhiều lần trong ca trực. Nghi vấn đỗ xe quá giờ hoặc thiếu lượt ra hợp lệ.",
                            GuardSecurityAlert.Severity.HIGH,
                            timeStr,
                            "Bãi xe A (Cổng chính)",
                            GuardSecurityAlert.Status.UNHANDLED
                    ));
                } else if (v.isValid()) {
                    binding.badgePlateStatus.setText(R.string.guard_plate_valid);
                    binding.badgePlateStatus.setBackgroundResource(R.drawable.bg_badge_processed);
                    binding.badgePlateStatus.setTextColor(0xFF10B981);

                    binding.tvResultViolationTitle.setText("Phương tiện hợp lệ (Đã đăng ký)");
                    binding.tvResultViolationTitle.setTextColor(0xFF10B981);
                    binding.ivViolationIcon.setColorFilter(0xFF10B981);
                    binding.tvResultViolationDesc.setText("Sẵn sàng ghi nhận nếu phương tiện vi phạm đỗ sai vị trí hoặc chắn lối đi.");
                } else {
                    binding.badgePlateStatus.setText(R.string.guard_plate_unregistered);
                    binding.badgePlateStatus.setBackgroundResource(R.drawable.bg_badge_warning_amber);
                    binding.badgePlateStatus.setTextColor(0xFFFFB525);

                    binding.tvResultViolationTitle.setText("Phương tiện chưa đăng ký trong hệ thống");
                    binding.tvResultViolationTitle.setTextColor(0xFFF13261);
                    binding.ivViolationIcon.setColorFilter(0xFFF13261);
                    binding.tvResultViolationDesc.setText("Không tìm thấy thông tin sinh viên/cán bộ. Nghi vấn gửi xe trái phép.");

                    // Tự động ghi nhận cảnh báo xe chưa đăng ký
                    String alertId = "ALT-" + (int)(1000 + Math.random() * 9000);
                    String timeStr = new SimpleDateFormat("HH:mm", new Locale("vi", "VN")).format(new Date()) + " Hôm nay";
                    repository.recordSecurityAlert(new GuardSecurityAlert(
                            alertId,
                            plate,
                            GuardSecurityAlert.AlertType.UNKNOWN_VEHICLE,
                            "Xe chưa đăng ký: " + plate,
                            "Phương tiện chưa đăng ký thông tin sinh viên/cán bộ trong hệ thống. Nghi vấn gửi xe trái phép.",
                            GuardSecurityAlert.Severity.MEDIUM,
                            timeStr,
                            "Bãi xe A (Cổng chính)",
                            GuardSecurityAlert.Status.UNHANDLED
                    ));
                }
            });
        });
    }

    private long effectiveCaptureMillis() {
        return lastCaptureMillis > 0L ? lastCaptureMillis : System.currentTimeMillis();
    }

    /**
     * Khi bấm "Ghi vi phạm" mà chưa có tọa độ GPS hợp lệ (hoặc nhập tay chưa chụp ảnh hiện trường):
     * Kiểm tra quyền Vị trí -> Kiểm tra bật GPS -> Hướng dẫn chụp lại ảnh kèm GPS.
     */
    private void handleMissingLocationForViolation() {
        if (!isAdded() || locationSource == null) return;
        if (!locationSource.hasFineLocationPermission()) {
            pendingLocationAction = PENDING_RESET_FOR_CAPTURE;
            showRequestLocationPermissionDialog();
            return;
        }
        locationSource.checkLocationSettings()
                .addOnSuccessListener(response -> {
                    if (!isAdded()) return;
                    locationSource.startWarmUp();
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.guard_loc_missing_for_record_title)
                            .setMessage(R.string.guard_loc_missing_for_record_message)
                            .setPositiveButton(R.string.guard_loc_btn_retake_with_gps, (d, w) -> resetToScanner())
                            .setNegativeButton(R.string.action_cancel, null)
                            .show();
                })
                .addOnFailureListener(exception -> {
                    if (!isAdded()) return;
                    pendingLocationAction = PENDING_RESET_FOR_CAPTURE;
                    if (exception instanceof ResolvableApiException) {
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) exception;
                            IntentSenderRequest req =
                                    new IntentSenderRequest.Builder(resolvable.getResolution()).build();
                            gpsResolutionLauncher.launch(req);
                            return;
                        } catch (Exception ignored) { }
                    }
                    showOpenSystemGpsDialog();
                });
    }

    private void onRecordViolationClicked() {
        if (isLocatingForCapture) {
            waitingLocationToRecord = true;
            binding.btnRecordViolation.setEnabled(false);
            Toast.makeText(requireContext(),
                    R.string.guard_loc_waiting_before_record, Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentScanMode == MODE_MULTI && multiDetectedPlates.size() > 1) {
            proceedToRecordBatchViolation(multiDetectedPlates, currentDraftId, effectiveCaptureMillis());
            return;
        }

        String targetPlate = (currentScanMode == MODE_MULTI && !multiDetectedPlates.isEmpty())
                ? (selectedMultiPlate != null ? selectedMultiPlate : multiDetectedPlates.get(0))
                : currentPlate;

        if (targetPlate == null || targetPlate.trim().isEmpty()) {
            Toast.makeText(requireContext(),
                    "Vui lòng chọn hoặc quét nhận diện biển số trước khi lập hồ sơ vi phạm",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Nếu xe có cảnh báo quét lặp nhiều lần trong ngày
        if (currentDuplicateResult != null && currentDuplicateResult.isDuplicate) {
            showDuplicateScanDialog(targetPlate);
        } else {
            proceedToRecordViolation(targetPlate, currentDraftId, effectiveCaptureMillis());
        }
    }

    private void proceedToRecordBatchViolation(List<String> plates, String draftId, long captureMillis) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Phiên bảo vệ hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            return;
        }
        File originalFile = draftId != null ? draftStore.imageFile(draftId, false) : null;
        if (originalFile == null || !originalFile.exists()
                || lastLocationResult == null || !lastLocationResult.accepted) {
            handleMissingLocationForViolation();
            return;
        }

        GuardDraft draft = new GuardDraft();
        draft.id = draftId;
        draft.ownerUid = currentUser.getUid();
        draft.capturedAtMillis = captureMillis;
        draft.isBatch = true;
        draft.batchPlates = new ArrayList<>(plates);
        draft.plate = android.text.TextUtils.join(", ", plates);
        draft.originalName = draftId + "_original.jpg";

        File cropFile = draftStore.imageFile(draftId, true);
        if (cropFile.exists()) {
            draft.cropName = cropFile.getName();
        }

        draft.latitude = lastLocationResult.latitude;
        draft.longitude = lastLocationResult.longitude;
        draft.accuracyMeters = lastLocationResult.accuracyMeters;
        draft.locationAtMillis = lastLocationResult.locationAtMillis;
        draft.locationAccepted = true;

        try {
            draftStore.save(draft);
        } catch (IOException ignored) { }

        Bundle args = new Bundle();
        args.putString("draftId", draftId);

        try {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_camera_to_review, args);
        } catch (Exception e) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.guardReviewFragment, args);
        }
    }

    private void showDuplicateScanDialog(String plate) {
        String msg = "Phương tiện " + plate + " đã được ghi nhận " +
                (currentDuplicateResult != null ? currentDuplicateResult.scanCountToday : 2) +
                " lần trong ngày hôm nay.\n\nVui lòng chọn cơ chế xử lý:";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cảnh báo xe quét lại trong ngày")
                .setMessage(msg)
                .setPositiveButton("Lập vi phạm lần 2", (d, w) -> {
                    proceedToRecordViolation(plate, currentDraftId, effectiveCaptureMillis());
                })
                .setNeutralButton("Bổ sung bằng chứng", (d, w) -> {
                    String existingId = currentDuplicateResult != null ? currentDuplicateResult.previousViolationId : null;
                    File originalFile = currentDraftId != null ? draftStore.imageFile(currentDraftId, false) : null;
                    if (existingId != null && !existingId.isEmpty() && originalFile != null && originalFile.exists()) {
                        double lat = lastLocationResult != null ? lastLocationResult.latitude : 0;
                        double lng = lastLocationResult != null ? lastLocationResult.longitude : 0;
                        float acc = lastLocationResult != null ? lastLocationResult.accuracyMeters : 0;
                        repository.supplementViolationEvidence(existingId, originalFile, lat, lng, acc)
                                .addOnSuccessListener(unused -> {
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(),
                                                "Đã tải và bổ sung ảnh bằng chứng vào hồ sơ " + existingId,
                                                Toast.LENGTH_SHORT).show();
                                        resetToScanner();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(),
                                                "Không thể bổ sung ảnh bằng chứng: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        Toast.makeText(requireContext(),
                                "Không tìm thấy hồ sơ vi phạm trước đó hoặc ảnh chụp để bổ sung.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void proceedToRecordViolation(String plate, String draftId, long captureMillis) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Phiên bảo vệ hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            return;
        }
        File originalFile = draftId != null ? draftStore.imageFile(draftId, false) : null;
        if (originalFile == null || !originalFile.exists()
                || lastLocationResult == null || !lastLocationResult.accepted) {
            handleMissingLocationForViolation();
            return;
        }

        GuardDraft draft = new GuardDraft();
        draft.id = draftId;
        draft.ownerUid = currentUser.getUid();
        draft.capturedAtMillis = captureMillis;
        draft.plate = plate;
        draft.originalName = draftId + "_original.jpg";

        File cropFile = draftStore.imageFile(draftId, true);
        if (cropFile.exists()) {
            draft.cropName = cropFile.getName();
        }

        draft.latitude = lastLocationResult.latitude;
        draft.longitude = lastLocationResult.longitude;
        draft.accuracyMeters = lastLocationResult.accuracyMeters;
        draft.locationAtMillis = lastLocationResult.locationAtMillis;
        draft.locationAccepted = true;

        try {
            draftStore.save(draft);
        } catch (IOException ignored) { }

        // Chuyển sang màn hình GuardReviewFragment với draftId
        Bundle args = new Bundle();
        args.putString("draftId", draftId);

        try {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_camera_to_review, args);
        } catch (Exception e) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.guardReviewFragment, args);
        }
    }

    @Nullable
    private static Bitmap cropWithPadding(Bitmap image, RectF box, float padFactorX, float padFactorY) {
        int padX = Math.round(box.width() * padFactorX);
        int padY = Math.round(box.height() * padFactorY);
        int left = Math.max(0, (int) box.left - padX);
        int top = Math.max(0, (int) box.top - padY);
        int right = Math.min(image.getWidth(), (int) Math.ceil(box.right) + padX);
        int bottom = Math.min(image.getHeight(), (int) Math.ceil(box.bottom) + padY);
        if (right <= left || bottom <= top) return null;
        return Bitmap.createBitmap(image, left, top, right - left, bottom - top);
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
