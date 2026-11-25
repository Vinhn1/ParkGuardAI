package com.example.parkguardai.ui.student;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.databinding.FragmentStudentAddVehicleBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Màn hình Đăng ký xe mới dành cho Sinh viên.
 * Chọn ảnh cà vẹt và ảnh xe thật từ thiết bị, tải lên Cloudinary và ghi hồ sơ PENDING lên Firestore.
 */
public final class StudentAddVehicleFragment extends Fragment {
    private FragmentStudentAddVehicleBinding binding;
    private StudentViewModel viewModel;
    private String selectedVehicleType = "MOTORBIKE";
    private File registrationCardFile;
    private File vehiclePhotoFile;

    private final ActivityResultLauncher<String> pickRegCardLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && getContext() != null) {
                    File copied = copyUriToCache(uri, "reg_card_" + System.currentTimeMillis() + ".jpg");
                    if (copied != null) {
                        registrationCardFile = copied;
                        Toast.makeText(requireContext(), "Đã chọn ảnh cà vẹt xe sẵn sàng tải lên", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Không đọc được tệp ảnh đã chọn", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<String> pickVehiclePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && getContext() != null) {
                    File copied = copyUriToCache(uri, "veh_photo_" + System.currentTimeMillis() + ".jpg");
                    if (copied != null) {
                        vehiclePhotoFile = copied;
                        Toast.makeText(requireContext(), "Đã chọn ảnh phương tiện sẵn sàng tải lên", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Không đọc được tệp ảnh đã chọn", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentAddVehicleBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StudentViewModel.class);

        binding.btnBack.setOnClickListener(v -> {
            if (!NavHostFragment.findNavController(this).popBackStack()) {
                requireActivity().onBackPressed();
            }
        });

        android.text.InputFilter plateFilter = (source, start, end, dest, dstart, dend) -> {
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (Character.isLetterOrDigit(c) || c == '-' || c == '.' || c == ' ') {
                    sb.append(Character.toUpperCase(c));
                }
            }
            return sb.toString();
        };
        binding.etPlate.setFilters(new android.text.InputFilter[]{
                new android.text.InputFilter.AllCaps(),
                new android.text.InputFilter.LengthFilter(15),
                plateFilter
        });

        setupInputSoftKeyboard(binding.etPlate);
        setupInputSoftKeyboard(binding.etBrand);
        setupInputSoftKeyboard(binding.etColor);

        setupVehicleTypeTabs();

        binding.cardUploadRegistration.setOnClickListener(v -> pickRegCardLauncher.launch("image/*"));
        binding.cardUploadVehiclePhoto.setOnClickListener(v -> pickVehiclePhotoLauncher.launch("image/*"));

        binding.btnSubmitVerification.setOnClickListener(v -> {
            String plate = binding.etPlate.getText() != null
                    ? binding.etPlate.getText().toString().trim() : "";
            String brand = binding.etBrand.getText() != null
                    ? binding.etBrand.getText().toString().trim() : "";
            String color = binding.etColor.getText() != null
                    ? binding.etColor.getText().toString().trim() : "";

            if (TextUtils.isEmpty(plate)) {
                binding.tilPlate.setError("Vui lòng nhập biển số xe");
                binding.etPlate.requestFocus();
                showKeyboard(binding.etPlate);
                return;
            }

            binding.tilPlate.setError(null);
            binding.btnSubmitVerification.setEnabled(false);

            viewModel.registerVehicle(
                    requireContext(),
                    plate,
                    selectedVehicleType,
                    TextUtils.isEmpty(brand) ? "" : brand,
                    TextUtils.isEmpty(color) ? "" : color,
                    registrationCardFile,
                    vehiclePhotoFile,
                    () -> {
                        if (isAdded()) {
                            binding.btnSubmitVerification.setEnabled(true);
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
                binding.btnSubmitVerification.setEnabled(!loading);
            }
        });
    }

    @Nullable
    private File copyUriToCache(@NonNull Uri uri, @NonNull String fileName) {
        if (getContext() == null) return null;
        File outFile = new File(requireContext().getCacheDir(), fileName);
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(outFile)) {
            if (in == null) return null;
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            return outFile;
        } catch (IOException e) {
            return null;
        }
    }

    private void setupInputSoftKeyboard(View view) {
        view.setOnClickListener(v -> {
            v.requestFocus();
            showKeyboard(v);
        });
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showKeyboard(v);
            }
        });
    }

    private void showKeyboard(View view) {
        view.post(() -> {
            if (getContext() != null) {
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) requireContext()
                                .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
    }

    private void setupVehicleTypeTabs() {
        binding.tabMotorbike.setOnClickListener(v -> {
            selectTab(binding.tabMotorbike);
            selectedVehicleType = "MOTORBIKE";
        });
        binding.tabElectricBike.setOnClickListener(v -> {
            selectTab(binding.tabElectricBike);
            selectedVehicleType = "ELECTRIC_BIKE";
        });
        binding.tabCar.setOnClickListener(v -> {
            selectTab(binding.tabCar);
            selectedVehicleType = "CAR";
        });
    }

    private void selectTab(TextView selected) {
        resetTab(binding.tabMotorbike);
        resetTab(binding.tabElectricBike);
        resetTab(binding.tabCar);

        selected.setBackgroundResource(R.drawable.bg_segmented_active);
        selected.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        selected.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void resetTab(TextView tab) {
        tab.setBackground(null);
        tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        tab.setTypeface(null, android.graphics.Typeface.NORMAL);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
