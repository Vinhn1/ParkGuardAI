package com.example.parkguardai.ui.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.ai.ocr.PlateParser;
import com.example.parkguardai.ai.ocr.PlateTextReader;
import com.example.parkguardai.databinding.FragmentStudentOnboardingBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

/** Thu thông tin tự khai của sinh viên sau xác minh email và gửi xe đầu tiên. */
public class StudentOnboardingFragment extends Fragment {

    private FragmentStudentOnboardingBinding binding;
    private StudentOnboardingViewModel viewModel;

    private String savedFullName = "";
    private String savedStudentCode = "";
    private String savedStudentClass = "";
    private String savedPhone = "";
    private String registrationCardKey = "";
    private File registrationCardFile;
    private File vehiclePhotoFile;

    private final ActivityResultLauncher<String> pickRegistrationCard =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && getContext() != null) {
                    File copied = copyUriToCache(uri, "onboarding_reg_" + System.currentTimeMillis() + ".jpg");
                    if (copied != null) {
                        registrationCardFile = copied;
                        registrationCardKey = "";
                        binding.textUploadRegStatus.setText("Đã chọn ảnh cà vẹt xe (Sẵn sàng tải lên)");
                        binding.textUploadRegStatus.setTextColor(requireContext().getColor(R.color.accent_cyan));
                        Snackbar.make(binding.getRoot(), "Đã chọn ảnh cà vẹt xe thành công.", Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(binding.getRoot(), "Không đọc được tệp ảnh cà vẹt đã chọn.", Snackbar.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<String> pickVehiclePhoto =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && getContext() != null) {
                    File copied = copyUriToCache(uri, "onboarding_veh_" + System.currentTimeMillis() + ".jpg");
                    if (copied != null) {
                        vehiclePhotoFile = copied;
                    }
                    processVehiclePhoto(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentOnboardingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(StudentOnboardingViewModel.class);

        Bundle args = getArguments();
        if (args != null) {
            savedFullName = args.getString("fullName", "");
            savedStudentCode = args.getString("studentCode", "");
            savedStudentClass = args.getString("studentClass", "");
            savedPhone = args.getString("phone", "");
        }
        // Khôi phục thông tin tự khai đã lưu tạm lúc đăng ký nếu người dùng thoát app để xác minh email rồi mở lại
        if (savedFullName.isEmpty() || savedStudentCode.isEmpty()) {
            SharedPreferences draft = requireContext().getSharedPreferences("parkguard_onboarding_draft", Context.MODE_PRIVATE);
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String currentEmail = currentUser != null && currentUser.getEmail() != null
                    ? currentUser.getEmail().toLowerCase(Locale.ROOT) : "";
            String draftEmail = draft.getString("email", "");
            if (currentEmail.isEmpty() || draftEmail.isEmpty() || currentEmail.equals(draftEmail)) {
                if (savedFullName.isEmpty()) savedFullName = draft.getString("fullName", "");
                if (savedStudentCode.isEmpty()) savedStudentCode = draft.getString("studentCode", "");
                if (savedStudentClass.isEmpty()) savedStudentClass = draft.getString("studentClass", "");
                if (savedPhone.isEmpty()) savedPhone = draft.getString("phone", "");
            }
        }

        binding.backButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.submitButton.setOnClickListener(ignored -> submit());
        binding.skipVehicleButton.setOnClickListener(ignored -> {
            // Cho phép sinh viên bỏ qua đăng ký xe để vào tài khoản trước
            NavController nav = NavHostFragment.findNavController(this);
            if (nav.getCurrentDestination() != null
                    && nav.getCurrentDestination().getId() == R.id.studentOnboardingFragment) {
                nav.navigate(R.id.registrationSubmittedFragment);
            }
        });

        // Xử lý chuyển đổi loại phương tiện (Xe máy | Xe đạp điện | Ô tô)
        binding.vehicleTypeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            updateVehicleTypeToggleStyle(checkedId);
        });

        // Tương tác thẻ tải ảnh cà vẹt xe
        binding.cardUploadRegistration.setOnClickListener(v ->
                pickRegistrationCard.launch("image/*"));

        // Tương tác thẻ tải ảnh xe và biển số (AI đối chiếu)
        binding.cardUploadVehicle.setOnClickListener(v ->
                pickVehiclePhoto.launch("image/*"));

        // Nút thêm xe khác
        binding.addAnotherVehicleButton.setOnClickListener(v -> {
            Snackbar.make(binding.getRoot(),
                    "Bạn có thể đăng ký thêm xe khác sau khi hồ sơ đầu tiên được duyệt.",
                    Snackbar.LENGTH_SHORT).show();
        });

        binding.plateInput.setOnEditorActionListener((ignored, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            submit();
            return true;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.load();
    }

    private void processVehiclePhoto(Uri uri) {
        try {
            Bitmap bitmap;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(requireContext().getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> decoder.setTargetSampleSize(2));
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
            }
            if (bitmap != null) {
                PlateTextReader reader = new PlateTextReader();
                reader.read(bitmap).addOnSuccessListener(text -> {
                    if (binding == null || !isAdded()) {
                        reader.close();
                        return;
                    }
                    PlateParser.Result result = PlateParser.parse(text);
                    if (result != null && result.formattedPlate != null && !result.formattedPlate.isEmpty()) {
                        binding.plateInput.setText(result.formattedPlate);
                        binding.textUploadVehicleStatus.setText("AI nhận diện: " + result.formattedPlate);
                        binding.textUploadVehicleStatus.setTextColor(requireContext().getColor(R.color.accent_cyan));
                        Snackbar.make(binding.getRoot(),
                                "AI đã nhận diện biển số: " + result.formattedPlate,
                                Snackbar.LENGTH_LONG).show();
                    } else {
                        binding.textUploadVehicleStatus.setText("Đã chọn ảnh xe (Chưa đọc rõ biển số)");
                        binding.textUploadVehicleStatus.setTextColor(requireContext().getColor(R.color.accent_cyan));
                        Snackbar.make(binding.getRoot(),
                                "Đã tải ảnh xe. Vui lòng kiểm tra lại biển số.",
                                Snackbar.LENGTH_SHORT).show();
                    }
                    reader.close();
                }).addOnFailureListener(e -> {
                    if (binding != null && isAdded()) {
                        binding.textUploadVehicleStatus.setText("Đã chọn ảnh xe");
                        binding.textUploadVehicleStatus.setTextColor(requireContext().getColor(R.color.accent_cyan));
                    }
                    reader.close();
                });
            }
        } catch (Exception e) {
            binding.textUploadVehicleStatus.setText("Đã chọn ảnh xe");
            Snackbar.make(binding.getRoot(), "Đã chọn ảnh xe thành công.", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void updateVehicleTypeToggleStyle(int checkedId) {
        int activeBg = requireContext().getColor(R.color.accent_cyan);
        int inactiveBg = android.graphics.Color.TRANSPARENT;
        int activeText = requireContext().getColor(R.color.white);
        int inactiveText = requireContext().getColor(R.color.text_muted);

        boolean isMotorbike = checkedId == R.id.typeMotorbikeButton;
        boolean isElectric = checkedId == R.id.typeElectricBikeButton;
        boolean isCar = checkedId == R.id.typeCarButton;

        binding.typeMotorbikeButton.setBackgroundColor(isMotorbike ? activeBg : inactiveBg);
        binding.typeMotorbikeButton.setTextColor(isMotorbike ? activeText : inactiveText);

        binding.typeElectricBikeButton.setBackgroundColor(isElectric ? activeBg : inactiveBg);
        binding.typeElectricBikeButton.setTextColor(isElectric ? activeText : inactiveText);

        binding.typeCarButton.setBackgroundColor(isCar ? activeBg : inactiveBg);
        binding.typeCarButton.setTextColor(isCar ? activeText : inactiveText);
    }

    private void submit() {
        String plateRaw = textOf(binding.plateInput).trim();
        String plateCanonical = plateRaw.toUpperCase(Locale.ROOT)
                .replace(" ", "").replace("-", "").replace(".", "");
        binding.plateField.setError(null);

        String vehicleType;
        int checkedId = binding.vehicleTypeToggleGroup.getCheckedButtonId();
        if (checkedId == R.id.typeMotorbikeButton) {
            vehicleType = "MOTORBIKE";
        } else if (checkedId == R.id.typeElectricBikeButton) {
            vehicleType = "ELECTRIC_BIKE";
        } else if (checkedId == R.id.typeCarButton) {
            vehicleType = "CAR";
        } else {
            vehicleType = "MOTORBIKE";
        }

        if (plateRaw.length() < 5 || plateRaw.length() > 16
                || !plateCanonical.matches("[0-9A-Z]{5,12}")) {
            binding.plateField.setError(getString(R.string.onboarding_plate_error));
            binding.plateInput.requestFocus();
            return;
        }

        String brand = textOf(binding.brandInput).trim();
        String color = textOf(binding.colorInput).trim();
        String fullName = savedFullName.isEmpty() ? "Sinh viên" : savedFullName;

        viewModel.save(fullName, savedStudentCode, savedStudentClass, savedPhone,
                plateRaw, plateCanonical, vehicleType, brand, color, registrationCardKey,
                registrationCardFile, vehiclePhotoFile);
    }

    @Nullable
    private File copyUriToCache(@NonNull Uri uri, @NonNull String fileName) {
        try {
            File outFile = new File(requireContext().getCacheDir(), fileName);
            try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(outFile)) {
                if (in == null) return null;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            return outFile;
        } catch (Exception e) {
            return null;
        }
    }

    private String textOf(com.google.android.material.textfield.TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private void render(StudentOnboardingViewModel.UiState state) {
        if (state.saved) {
            if (getContext() != null) {
                requireContext().getSharedPreferences("parkguard_onboarding_draft", Context.MODE_PRIVATE)
                        .edit().clear().apply();
            }
            NavController nav = NavHostFragment.findNavController(this);
            if (nav.getCurrentDestination() != null
                    && nav.getCurrentDestination().getId() == R.id.studentOnboardingFragment) {
                nav.navigate(R.id.registrationSubmittedFragment);
            }
            return;
        }
        binding.onboardingForm.setVisibility(state.saved ? View.GONE : View.VISIBLE);
        binding.successPanel.setVisibility(state.saved ? View.VISIBLE : View.GONE);
        if (state.saved) {
            binding.successMessage.setText(state.messageRes == 0
                    ? R.string.onboarding_already_sent : state.messageRes);
        }
        binding.submitButton.setEnabled(!state.loading);
        binding.loadingProgress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
        binding.formStatus.setVisibility(!state.saved && state.messageRes != 0
                ? View.VISIBLE : View.GONE);
        if (!state.saved && state.messageRes != 0) {
            binding.formStatus.setText(state.messageRes);
            binding.formStatus.setTextColor(requireContext().getColor(
                    state.error ? R.color.pg_error : R.color.pg_primary));
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
