package com.example.parkguardai.ui.guard;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.parkguardai.databinding.BottomSheetGuardManualEntryBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * BottomSheet nhap bien so thu cong danh cho Bao ve.
 * Dung trong truong hop camera gap su co khach quan hoac bien so khong the nhan dien tu dong.
 */
public final class GuardManualEntryBottomSheet extends BottomSheetDialogFragment {
    private BottomSheetGuardManualEntryBinding binding;

    public interface OnPlateVerifiedListener {
        void onPlateVerified(String plateNumber, String vehicleType);
    }

    private OnPlateVerifiedListener listener;

    public void setOnPlateVerifiedListener(OnPlateVerifiedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetGuardManualEntryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnClose.setOnClickListener(v -> dismiss());

        binding.btnVerifyManual.setOnClickListener(v -> {
            String plate = binding.etManualPlate.getText() != null
                    ? binding.etManualPlate.getText().toString().trim()
                    : "";

            if (TextUtils.isEmpty(plate)) {
                binding.tilManualPlate.setError("Vui long nhap bien so xe");
                return;
            }

            binding.tilManualPlate.setError(null);

            String vehicleType = "Xe may";
            int checkedId = binding.chipGroupVehicleType.getCheckedChipId();
            if (checkedId == binding.chipCar.getId()) {
                vehicleType = "O to";
            } else if (checkedId == binding.chipElectricBike.getId()) {
                vehicleType = "Xe dap dien";
            }

            if (listener != null) {
                listener.onPlateVerified(plate, vehicleType);
            } else {
                Toast.makeText(requireContext(),
                        "Da xac minh bien so " + plate + " (" + vehicleType + "): Hop le - Mo barie",
                        Toast.LENGTH_SHORT).show();
            }

            dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
