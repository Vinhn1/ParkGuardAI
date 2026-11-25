package com.example.parkguardai.ui.guard;

import android.app.Activity;
import android.app.Dialog;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.ai.detection.PlateDetector;
import com.example.parkguardai.ai.ocr.PlateParser;
import com.example.parkguardai.ai.ocr.PlateTextReader;
import com.example.parkguardai.data.datasource.GuardImageProcessor;
import com.example.parkguardai.data.model.GuardDraft;
import com.example.parkguardai.data.repository.GuardDraftStore;
import com.example.parkguardai.data.repository.ViolationRepository;
import com.example.parkguardai.databinding.FragmentGuardReviewBinding;
import com.example.parkguardai.databinding.BottomSheetViolationTypeBinding;
import com.example.parkguardai.databinding.DialogLeaveDraftBinding;
import com.example.parkguardai.databinding.DialogUploadProgressBinding;
import com.example.parkguardai.databinding.DialogSaveSuccessBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Hai bước rà soát ảnh/thông tin rồi xác nhận gửi hồ sơ; AI chỉ gợi ý vùng và chữ biển số. */
public final class GuardReviewFragment extends Fragment {
    private static final List<String> TYPE_CODES = Arrays.asList(
            "PARKING_WRONG_AREA", "BLOCKING_ENTRANCE", "NO_PARKING_ZONE", "OTHER");
    private FragmentGuardReviewBinding binding;
    private GuardReviewViewModel viewModel;
    private GuardDraftStore store;
    private GuardDraft draft;
    private Bitmap preview;
    private List<String> scopes;
    private boolean cropping;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    /** Chỉ được tạo, dùng và đóng trên luồng {@link #io}. */
    private PlateDetector detector;
    private PlateTextReader textReader;
    /** Biển số AI đã điền gần nhất; dùng để không ghi đè giá trị bảo vệ tự nhập. */
    private String aiPlate = "";
    /** Tăng trên luồng chính để bỏ kết quả AI cũ. */
    private int aiVersion;
    private int selectedTypeIndex = -1;
    private Dialog uploadProgressDialog;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle state) {
        binding = FragmentGuardReviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        store = new GuardDraftStore(requireContext());
        textReader = new PlateTextReader();
        viewModel = new ViewModelProvider(this).get(GuardReviewViewModel.class);
        binding.backButton.setOnClickListener(v -> handleBack());
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() { handleBack(); }
                });
        String id = getArguments() == null ? null : getArguments().getString("draftId");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : "";
        try { draft = store.load(id, uid); }
        catch (IOException | IllegalArgumentException failure) {
            binding.locationText.setText(R.string.guard_review_missing);
            binding.continueButton.setEnabled(false);
            return;
        }
        binding.backEditButton.setOnClickListener(v -> showEdit());
        binding.cropButton.setOnClickListener(v -> crop());
        binding.continueButton.setOnClickListener(v -> review());
        binding.submitButton.setOnClickListener(v -> viewModel.submit(draft));
        selectedTypeIndex = state == null ? TYPE_CODES.indexOf(draft.typeCode)
                : state.getInt("selectedTypeIndex", TYPE_CODES.indexOf(draft.typeCode));
        updateSelectedType();
        binding.typeSelectButton.setOnClickListener(v -> showTypeSheet());
        binding.plateInput.setText(draft.plate);
        if (draft.isBatch) {
            int count = draft.batchPlates != null ? draft.batchPlates.size() : 2;
            showPlateHint("Hồ sơ vi phạm hàng loạt (" + count + " xe) cùng hiện trường & thời điểm.");
        }
        binding.noteInput.setText(draft.note);
        if (draft.penaltyAmount != null && !draft.penaltyAmount.isEmpty()) {
            binding.penaltyInput.setText(draft.penaltyAmount);
        } else {
            int vehicleCount = (draft.batchPlates != null && !draft.batchPlates.isEmpty()) ? draft.batchPlates.size() : 1;
            int total = 100000 * vehicleCount;
            binding.penaltyInput.setText(String.format(Locale.US, "%,d đ", total).replace(',', '.'));
        }

        binding.chipPenalty100.setOnClickListener(v -> binding.penaltyInput.setText("100.000 đ"));
        binding.chipPenalty150.setOnClickListener(v -> binding.penaltyInput.setText("150.000 đ"));
        binding.chipPenalty200.setOnClickListener(v -> binding.penaltyInput.setText("200.000 đ"));
        binding.chipPenalty300.setOnClickListener(v -> binding.penaltyInput.setText("300.000 đ"));
        binding.chipPenaltyZero.setOnClickListener(v -> binding.penaltyInput.setText("0 đ (Nhắc nhở)"));

        binding.timeText.setText(getString(R.string.guard_review_time,
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                        new Locale("vi", "VN")).format(new Date(draft.capturedAtMillis))));
        binding.locationText.setText(getString(R.string.guard_review_location,
                draft.latitude, draft.longitude, draft.accuracyMeters));
        binding.cropButton.setEnabled(draft.originalUpload == null && draft.cropUpload == null);
        loadImages();
        viewModel.getScopes().observe(getViewLifecycleOwner(), values -> {
            scopes = values;
            if (values == null || values.isEmpty()) {
                binding.continueButton.setEnabled(false);
                binding.locationText.setText(R.string.guard_review_no_scope);
                return;
            }
            binding.scopeSpinner.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_dropdown_item, values));
            int selectedScope = values.indexOf(draft.scopeId);
            if (selectedScope >= 0) binding.scopeSpinner.setSelection(selectedScope);
            binding.continueButton.setEnabled(draft.locationAccepted && !cropping);
        });
        viewModel.getSubmit().observe(getViewLifecycleOwner(), this::renderSubmit);
        viewModel.loadScopes();
    }

    private void loadImages() {
        Activity activity = requireActivity();
        io.execute(() -> {
            try {
                File originalFile = store.imageFile(draft.id, false);
                if (!originalFile.exists()) {
                    throw new IOException("Primary photo file not found");
                }
                Bitmap loaded = new GuardImageProcessor().loadPreview(originalFile);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                Bitmap crop = draft.cropName.isEmpty() ? null : BitmapFactory.decodeFile(
                        store.imageFile(draft.id, true).getAbsolutePath(), options);
                activity.runOnUiThread(() -> {
                    if (binding == null) return;
                    preview = loaded;
                    binding.originalImage.setImageBitmap(loaded);
                    binding.confirmImage.setImageBitmap(loaded);
                    if (crop != null) {
                        binding.cropPreview.setImageBitmap(crop);
                        binding.cropPreview.setVisibility(View.VISIBLE);
                    }
                    binding.cropButton.setEnabled(draft.originalUpload == null && draft.cropUpload == null);
                    // Chỉ gợi ý khi hồ sơ chưa có biển số đã rà; bản nháp cũ giữ nguyên giá trị bảo vệ nhập.
                    if (draft.plate.isEmpty()) suggestPlate(loaded);
                });
            } catch (IOException failure) {
                activity.runOnUiThread(() -> {
                    if (binding != null) {
                        binding.cropButton.setEnabled(false);
                        binding.continueButton.setEnabled(false);
                        binding.locationText.setText(R.string.guard_review_missing);
                    }
                });
            }
        });
    }

    private void crop() {
        if (preview == null || draft.originalUpload != null || draft.cropUpload != null) return;
        RectF selection = binding.cropSelection.getNormalizedSelection();
        float frameW = binding.imageFrame.getWidth();
        float frameH = binding.imageFrame.getHeight();
        float scale = Math.min(frameW / preview.getWidth(), frameH / preview.getHeight());
        float left = (frameW - preview.getWidth() * scale) / 2f;
        float top = (frameH - preview.getHeight() * scale) / 2f;
        RectF imageRect = new RectF(
                (selection.left * frameW - left) / (preview.getWidth() * scale),
                (selection.top * frameH - top) / (preview.getHeight() * scale),
                (selection.right * frameW - left) / (preview.getWidth() * scale),
                (selection.bottom * frameH - top) / (preview.getHeight() * scale));
        binding.cropButton.setEnabled(false);
        cropping = true;
        binding.continueButton.setEnabled(false);
        Activity activity = requireActivity();
        io.execute(() -> {
            try {
                File output = store.imageFile(draft.id, true);
                new GuardImageProcessor().saveCrop(preview, imageRect, output);
                draft.cropName = output.getName();
                store.save(draft);
                BitmapFactory.Options small = new BitmapFactory.Options();
                small.inSampleSize = 2;
                Bitmap crop = BitmapFactory.decodeFile(output.getAbsolutePath(), small);
                Bitmap ocrSource = BitmapFactory.decodeFile(output.getAbsolutePath());
                activity.runOnUiThread(() -> {
                    if (binding == null) return;
                    binding.cropPreview.setImageBitmap(crop);
                    binding.cropPreview.setVisibility(View.VISIBLE);
                    binding.locationText.setText(R.string.guard_review_crop_done);
                    binding.cropButton.setEnabled(true);
                    cropping = false;
                    binding.continueButton.setEnabled(scopes != null && !scopes.isEmpty()
                            && draft.locationAccepted);
                    // Bảo vệ đã chỉnh khung: đọc lại chữ trên vùng mới.
                    readPlate(ocrSource, ++aiVersion);
                });
            } catch (IOException failure) {
                activity.runOnUiThread(() -> {
                    if (binding == null) return;
                    binding.locationText.setText(R.string.guard_review_crop_failed);
                    binding.cropButton.setEnabled(true);
                    cropping = false;
                    binding.continueButton.setEnabled(scopes != null && !scopes.isEmpty()
                            && draft.locationAccepted);
                });
            }
        });
    }

    /** Tìm vùng biển số trên ảnh chính ở luồng nền, đặt khung cắt theo gợi ý rồi OCR. */
    private void suggestPlate(Bitmap image) {
        int version = ++aiVersion;
        showPlateHint(getString(R.string.guard_review_ai_detecting));
        Activity activity = requireActivity();
        Context appContext = requireContext().getApplicationContext();
        io.execute(() -> {
            RectF box = null;
            Bitmap plateCrop = null;
            try {
                if (detector == null) detector = new PlateDetector(appContext);
                List<PlateDetector.Detection> found = detector.detect(image);
                if (!found.isEmpty()) {
                    box = found.get(0).box;
                    plateCrop = cropWithPadding(image, box);
                }
            } catch (Exception ignored) {
                // TFLite có thể không tìm thấy góc này, tiếp tục fallback sang OCR toàn ảnh
            }
            RectF resultBox = box;
            Bitmap resultCrop = plateCrop;
            activity.runOnUiThread(() -> {
                if (binding == null || version != aiVersion) return;
                if (resultBox != null && resultCrop != null) {
                    if (draft.originalUpload == null && draft.cropUpload == null) {
                        showSelection(resultBox, image);
                    }
                    readPlate(resultCrop, version);
                } else {
                    // Fallback: Quét trực tiếp bằng ML Kit OCR trên toàn ảnh
                    readPlateFromFullImage(image, version);
                }
            });
        });
    }

    private void readPlateFromFullImage(Bitmap image, int version) {
        if (binding == null || textReader == null) return;
        showPlateHint(getString(R.string.guard_review_ai_detecting));
        try {
            textReader.read(image).addOnCompleteListener(task -> {
                if (binding == null || version != aiVersion) return;
                if (!task.isSuccessful() || task.getResult() == null) {
                    showPlateHint(getString(R.string.guard_review_ai_no_box));
                    return;
                }
                PlateParser.Result parsed = PlateParser.parse(task.getResult());
                if (parsed == null) {
                    showPlateHint(getString(R.string.guard_review_ai_no_box));
                    return;
                }
                String current = currentPlateInput();
                if (current.isEmpty() || current.equals(aiPlate)) {
                    aiPlate = parsed.formattedPlate;
                    binding.plateInput.setText(parsed.formattedPlate);
                    showPlateHint(getString(R.string.guard_review_ai_suggestion, parsed.formattedPlate));
                }
                if (parsed.boundingBox != null && draft.originalUpload == null && draft.cropUpload == null) {
                    RectF rawBox = new RectF(parsed.boundingBox);
                    RectF paddedBox = expandBox(rawBox, image.getWidth(), image.getHeight(), 0.15f);
                    showSelection(paddedBox, image);
                    io.execute(() -> {
                        try {
                            Bitmap crop = cropWithPadding(image, paddedBox);
                            if (crop != null) {
                                File output = store.imageFile(draft.id, true);
                                try (java.io.FileOutputStream stream = new java.io.FileOutputStream(output)) {
                                    crop.compress(Bitmap.CompressFormat.JPEG, 92, stream);
                                }
                                draft.cropName = output.getName();
                                store.save(draft);
                                requireActivity().runOnUiThread(() -> {
                                    if (binding != null) {
                                        binding.cropPreview.setImageBitmap(crop);
                                        binding.cropPreview.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        } catch (Exception ignored) { }
                    });
                }
            });
        } catch (RuntimeException e) {
            showPlateHint(getString(R.string.guard_review_ai_error));
        }
    }

    private static RectF expandBox(RectF box, int imgW, int imgH, float fraction) {
        float padX = box.width() * fraction;
        float padY = box.height() * fraction;
        return new RectF(
                Math.max(0f, box.left - padX),
                Math.max(0f, box.top - padY),
                Math.min((float) imgW, box.right + padX),
                Math.min((float) imgH, box.bottom + padY)
        );
    }

    @Nullable
    private static Bitmap cropWithPadding(Bitmap image, RectF box) {
        int padX = Math.round(box.width() * 0.1f);
        int padY = Math.round(box.height() * 0.1f);
        int left = Math.max(0, (int) box.left - padX);
        int top = Math.max(0, (int) box.top - padY);
        int right = Math.min(image.getWidth(), (int) Math.ceil(box.right) + padX);
        int bottom = Math.min(image.getHeight(), (int) Math.ceil(box.bottom) + padY);
        if (right <= left || bottom <= top) return null;
        return Bitmap.createBitmap(image, left, top, right - left, bottom - top);
    }

    /** Chuyển hộp theo pixel ảnh sang tọa độ chuẩn hóa của khung (ảnh hiển thị fitCenter). */
    private void showSelection(RectF box, Bitmap image) {
        binding.imageFrame.post(() -> {
            if (binding == null) return;
            float frameW = binding.imageFrame.getWidth();
            float frameH = binding.imageFrame.getHeight();
            if (frameW <= 0 || frameH <= 0) return;
            float scale = Math.min(frameW / image.getWidth(), frameH / image.getHeight());
            float left = (frameW - image.getWidth() * scale) / 2f;
            float top = (frameH - image.getHeight() * scale) / 2f;
            binding.cropSelection.setNormalizedSelection(new RectF(
                    (left + box.left * scale) / frameW,
                    (top + box.top * scale) / frameH,
                    (left + box.right * scale) / frameW,
                    (top + box.bottom * scale) / frameH));
        });
    }

    private void readPlate(@Nullable Bitmap crop, int version) {
        if (binding == null || textReader == null) return;
        if (crop == null) {
            showPlateHint(getString(R.string.guard_review_ai_ocr_empty));
            return;
        }
        showPlateHint(getString(R.string.guard_review_ai_reading));
        try {
            textReader.read(crop).addOnCompleteListener(task -> {
                if (binding == null || version != aiVersion) return;
                if (!task.isSuccessful() || task.getResult() == null) {
                    showPlateHint(getString(R.string.guard_review_ai_error));
                    return;
                }
                PlateParser.Result parsed = PlateParser.parse(task.getResult());
                if (parsed == null) {
                    showPlateHint(getString(R.string.guard_review_ai_ocr_empty));
                    return;
                }
                String current = currentPlateInput();
                if (current.isEmpty() || current.equals(aiPlate)) {
                    aiPlate = parsed.formattedPlate;
                    binding.plateInput.setText(parsed.formattedPlate);
                    showPlateHint(getString(R.string.guard_review_ai_suggestion, parsed.formattedPlate));
                } else {
                    showPlateHint(getString(R.string.guard_review_ai_kept_manual, parsed.formattedPlate));
                }
            });
        } catch (RuntimeException failure) {
            showPlateHint(getString(R.string.guard_review_ai_error));
        }
    }

    private String currentPlateInput() {
        return binding.plateInput.getText() == null ? ""
                : binding.plateInput.getText().toString().toUpperCase(Locale.ROOT)
                        .replaceAll("[\\s.\\-]", "");
    }

    private void showPlateHint(String message) {
        binding.plateSuggestionHint.setText(message);
        binding.plateSuggestionHint.setVisibility(View.VISIBLE);
    }

    private void showTypeSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        BottomSheetViolationTypeBinding sheet = BottomSheetViolationTypeBinding.inflate(
                getLayoutInflater());
        dialog.setContentView(sheet.getRoot());
        sheet.wrongAreaButton.setOnClickListener(v -> selectType(0, dialog));
        sheet.blockingButton.setOnClickListener(v -> selectType(1, dialog));
        sheet.noParkingButton.setOnClickListener(v -> selectType(2, dialog));
        sheet.otherButton.setOnClickListener(v -> selectType(3, dialog));
        dialog.show();
    }

    private void selectType(int index, BottomSheetDialog dialog) {
        selectedTypeIndex = index;
        updateSelectedType();
        binding.typeErrorText.setVisibility(View.GONE);

        int vehicleCount = (draft != null && draft.batchPlates != null && !draft.batchPlates.isEmpty())
                ? draft.batchPlates.size() : 1;
        int baseFine = 100000;
        if (index == 1) baseFine = 200000;
        else if (index == 2) baseFine = 150000;
        else if (index == 3) baseFine = 100000;

        int totalFine = baseFine * vehicleCount;
        String formattedFine = String.format(Locale.US, "%,d đ", totalFine).replace(',', '.');
        binding.penaltyInput.setText(formattedFine);

        dialog.dismiss();
    }

    private void updateSelectedType() {
        binding.typeSelectButton.setText(selectedTypeIndex < 0
                ? R.string.guard_review_type_unselected : typeLabelRes(selectedTypeIndex));
    }

    private void handleBack() {
        if (binding.confirmPanel.getVisibility() == View.VISIBLE) {
            showEdit();
        } else if (draft != null) {
            showLeaveDialog();
        } else {
            NavHostFragment.findNavController(this).navigateUp();
        }
    }

    private void showLeaveDialog() {
        DialogLeaveDraftBinding content = DialogLeaveDraftBinding.inflate(getLayoutInflater());
        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(content.getRoot()).create();
        content.continueButton.setOnClickListener(v -> dialog.dismiss());
        content.saveButton.setOnClickListener(v -> {
            draft.plate = currentPlateInput();
            draft.note = binding.noteInput.getText() == null ? ""
                    : binding.noteInput.getText().toString().trim();
            draft.penaltyAmount = binding.penaltyInput.getText() == null ? "100.000 đ"
                    : binding.penaltyInput.getText().toString().trim();
            if (selectedTypeIndex >= 0) draft.typeCode = TYPE_CODES.get(selectedTypeIndex);
            int selectedScope = binding.scopeSpinner.getSelectedItemPosition();
            if (scopes != null && selectedScope >= 0 && selectedScope < scopes.size()) {
                draft.scopeId = scopes.get(selectedScope);
            }
            try {
                store.save(draft);
                dialog.dismiss();
                NavHostFragment.findNavController(this).navigateUp();
            } catch (IOException failure) {
                Snackbar.make(binding.getRoot(), R.string.guard_review_error,
                        Snackbar.LENGTH_LONG).show();
            }
        });
        content.discardButton.setOnClickListener(v -> {
            try {
                if (store.delete(draft.id, draft.ownerUid)) {
                    dialog.dismiss();
                    NavHostFragment.findNavController(this).navigateUp();
                } else {
                    Snackbar.make(binding.getRoot(), R.string.guard_draft_delete_failed,
                            Snackbar.LENGTH_LONG).show();
                }
            } catch (IOException failure) {
                Snackbar.make(binding.getRoot(), R.string.guard_draft_delete_failed,
                        Snackbar.LENGTH_LONG).show();
            }
        });
        dialog.show();
    }

    @StringRes
    private int typeLabelRes(int index) {
        if (index == 0) return R.string.guard_type_wrong_area;
        if (index == 1) return R.string.guard_type_blocking;
        if (index == 2) return R.string.guard_type_no_parking;
        return R.string.guard_type_other;
    }

    private void review() {
        if (draft == null || scopes == null || scopes.isEmpty() || !draft.locationAccepted) return;
        String rawPlate = binding.plateInput.getText() == null ? ""
                : binding.plateInput.getText().toString().toUpperCase(Locale.ROOT);
        String note = binding.noteInput.getText() == null ? ""
                : binding.noteInput.getText().toString().trim();
        binding.plateField.setError(null);
        binding.typeErrorText.setVisibility(View.GONE);

        boolean isBatchInput = (draft != null && draft.isBatch) || rawPlate.contains(",");
        String plateToSave;

        if (isBatchInput) {
            String[] parts = rawPlate.split(",");
            boolean anyInvalid = false;
            for (String p : parts) {
                String clean = p.replaceAll("[\\s.\\-]", "");
                if (!clean.isEmpty() && !clean.matches("[A-Z0-9]{4,12}")) {
                    anyInvalid = true;
                    break;
                }
            }
            if (anyInvalid || (rawPlate.trim().isEmpty() && note.length() < 5)) {
                binding.plateField.setError(getString(R.string.guard_review_plate_error));
                return;
            }
            plateToSave = rawPlate.trim();
        } else {
            String singleClean = rawPlate.replaceAll("[\\s.\\-]", "");
            if ((!singleClean.isEmpty() && !singleClean.matches("[A-Z0-9]{4,12}"))
                    || (singleClean.isEmpty() && note.length() < 5)) {
                binding.plateField.setError(getString(R.string.guard_review_plate_error));
                return;
            }
            plateToSave = singleClean;
        }

        binding.noteField.setError(null);
        if (selectedTypeIndex < 0) {
            binding.typeErrorText.setVisibility(View.VISIBLE);
            return;
        }
        if (selectedTypeIndex == 3 && note.isEmpty()) {
            binding.noteField.setError(getString(R.string.guard_type_other_note_required));
            return;
        }
        String penaltyText = binding.penaltyInput.getText() == null ? "100.000 đ"
                : binding.penaltyInput.getText().toString().trim();
        if (penaltyText.isEmpty()) penaltyText = "100.000 đ";
        draft.penaltyAmount = penaltyText;
        draft.plate = plateToSave;
        draft.note = note;
        draft.typeCode = TYPE_CODES.get(selectedTypeIndex);
        int selectedScope = binding.scopeSpinner.getSelectedItemPosition();
        if (selectedScope < 0 || selectedScope >= scopes.size()) return;
        draft.scopeId = scopes.get(selectedScope);
        try { store.save(draft); }
        catch (IOException failure) {
            binding.locationText.setText(R.string.guard_review_error);
            return;
        }
        String plateDisplay = plateToSave.isEmpty()
                ? getString(R.string.guard_review_notice_unknown_plate)
                : (isBatchInput ? (plateToSave + " (" + (draft.batchPlates != null ? draft.batchPlates.size() : "đa") + " xe)") : plateToSave);

        String summary = getString(R.string.guard_review_notice_template,
                plateDisplay,
                getString(typeLabelRes(selectedTypeIndex)),
                binding.timeText.getText().toString(),
                getString(R.string.guard_review_location,
                        draft.latitude, draft.longitude, draft.accuracyMeters),
                draft.scopeId,
                note.isEmpty() ? getString(R.string.guard_review_notice_no_note) : note);
        binding.confirmSummary.setText(summary);

        // Cập nhật giao diện xác nhận hiện đại, làm nổi bật biển số và chi tiết vi phạm
        List<String> detectedPlates = new ArrayList<>();
        if (draft.batchPlates != null && !draft.batchPlates.isEmpty()) {
            detectedPlates.addAll(draft.batchPlates);
        } else if (plateToSave.contains(",") || plateToSave.contains("\n") || plateToSave.contains("•")) {
            for (String p : plateToSave.replace("•", "").split("[,\\n]+")) {
                String clean = p.trim();
                if (!clean.isEmpty()) detectedPlates.add(clean);
            }
        } else if (!plateToSave.trim().isEmpty()) {
            detectedPlates.add(plateToSave.trim());
        }

        if (detectedPlates.size() > 1) {
            binding.tvConfirmVehicleCountBadge.setText(detectedPlates.size() + " xe phát hiện");
            populateConfirmBatchPlates(detectedPlates.toArray(new String[0]));
            binding.tvConfirmViolationType.setText(getString(typeLabelRes(selectedTypeIndex)) + " (Hàng loạt " + detectedPlates.size() + " xe)");
        } else {
            binding.tvConfirmVehicleCountBadge.setText("1 xe");
            for (int i = binding.layoutConfirmPlatesList.getChildCount() - 1; i >= 0; i--) {
                View child = binding.layoutConfirmPlatesList.getChildAt(i);
                if (child.getId() != R.id.tvConfirmPlateDisplay) {
                    binding.layoutConfirmPlatesList.removeViewAt(i);
                }
            }
            binding.tvConfirmPlateDisplay.setVisibility(View.VISIBLE);
            binding.tvConfirmPlateDisplay.setText(detectedPlates.isEmpty() ? "CHƯA XÁC ĐỊNH" : detectedPlates.get(0));
            binding.tvConfirmViolationType.setText(getString(typeLabelRes(selectedTypeIndex)));
        }

        binding.tvConfirmTime.setText(binding.timeText.getText().toString().replace("Chụp lúc: ", ""));
        binding.tvConfirmScope.setText(draft.scopeId);
        binding.tvConfirmLocation.setText(String.format(Locale.US, "%.5f, %.5f (sai số %.0f m)",
                draft.latitude, draft.longitude, draft.accuracyMeters));
        binding.tvConfirmNote.setText(note.isEmpty() ? "Không có ghi chú bổ sung" : note);
        binding.tvConfirmPenalty.setText(penaltyText);

        binding.editPanel.setVisibility(View.GONE);
        binding.confirmPanel.setVisibility(View.VISIBLE);
        animatePanel(binding.confirmPanel);
    }

    private void populateConfirmBatchPlates(String[] parts) {
        if (binding == null) return;
        for (int i = binding.layoutConfirmPlatesList.getChildCount() - 1; i >= 0; i--) {
            View child = binding.layoutConfirmPlatesList.getChildAt(i);
            if (child.getId() != R.id.tvConfirmPlateDisplay) {
                binding.layoutConfirmPlatesList.removeViewAt(i);
            }
        }
        binding.tvConfirmPlateDisplay.setVisibility(View.GONE);

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

            android.widget.TextView tvBadge = new android.widget.TextView(requireContext());
            tvBadge.setText("Chưa đăng ký");
            tvBadge.setTextSize(11f);
            tvBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_amber));
            tvBadge.setTypeface(null, android.graphics.Typeface.BOLD);
            tvBadge.setBackgroundResource(R.drawable.bg_badge_warning_amber);
            tvBadge.setPadding(Math.round(8 * getResources().getDisplayMetrics().density),
                    Math.round(3 * getResources().getDisplayMetrics().density),
                    Math.round(8 * getResources().getDisplayMetrics().density),
                    Math.round(3 * getResources().getDisplayMetrics().density));
            row.addView(tvBadge);

            binding.layoutConfirmPlatesList.addView(row);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("selectedTypeIndex", selectedTypeIndex);
        super.onSaveInstanceState(outState);
    }

    private void showEdit() {
        binding.confirmPanel.setVisibility(View.GONE);
        binding.editPanel.setVisibility(View.VISIBLE);
        animatePanel(binding.editPanel);
    }

    private void animatePanel(View panel) {
        if (!ValueAnimator.areAnimatorsEnabled()) return;
        panel.animate().cancel();
        panel.setAlpha(0f);
        panel.setTranslationY(12f * getResources().getDisplayMetrics().density);
        panel.animate().alpha(1f).translationY(0f).setDuration(220L).start();
    }

    private void renderSubmit(GuardReviewViewModel.SubmitState state) {
        if (binding == null || draft == null) return;
        binding.submitProgress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
        binding.submitButton.setEnabled(!state.loading && !state.success);
        binding.backEditButton.setEnabled(!state.loading);
        binding.submitStatus.setText(state.error ? R.string.guard_review_error
                : state.success ? R.string.guard_review_success
                : state.stage == ViolationRepository.Stage.UPLOADING_ORIGINAL
                    ? R.string.guard_review_upload_original
                : state.stage == ViolationRepository.Stage.UPLOADING_CROP
                    ? R.string.guard_review_upload_crop
                : state.stage == ViolationRepository.Stage.SAVING_RECORD
                    ? R.string.guard_review_saving
                : state.stage == ViolationRepository.Stage.CHECKING_ACCESS
                    ? R.string.guard_review_checking : R.string.guard_review_confirm_text);

        // Hiển thị Dialog tiến trình (TASK-BACKEND-07)
        if (state.loading) {
            if (uploadProgressDialog == null) {
                DialogUploadProgressBinding progBinding = DialogUploadProgressBinding.inflate(getLayoutInflater());
                uploadProgressDialog = new MaterialAlertDialogBuilder(requireContext())
                        .setView(progBinding.getRoot())
                        .setCancelable(false)
                        .create();
                uploadProgressDialog.show();
            }
        } else {
            if (uploadProgressDialog != null) {
                uploadProgressDialog.dismiss();
                uploadProgressDialog = null;
            }
        }

        // Hiển thị Dialog thành công trước khi chuyển màn hình (TASK-BACKEND-07)
        if (state.success && state.recordId != null) {
            DialogSaveSuccessBinding successBinding = DialogSaveSuccessBinding.inflate(getLayoutInflater());
            Dialog successDialog = new MaterialAlertDialogBuilder(requireContext())
                    .setView(successBinding.getRoot())
                    .setCancelable(false)
                    .create();
            successDialog.show();

            binding.getRoot().postDelayed(() -> {
                if (successDialog.isShowing()) {
                    successDialog.dismiss();
                }
                if (binding == null) return;
                Bundle args = new Bundle();
                args.putString("recordId", state.recordId);
                viewModel.consumeSuccess();
                NavHostFragment.findNavController(this).navigate(R.id.action_review_to_detail, args);
            }, 1200L);
        }
    }

    @Override public void onDestroyView() {
        aiVersion++;
        if (textReader != null) textReader.close();
        textReader = null;
        binding = null;
        super.onDestroyView();
    }

    @Override public void onDestroy() {
        io.execute(() -> {
            if (detector != null) detector.close();
            detector = null;
        });
        io.shutdown();
        super.onDestroy();
    }
}
