package com.example.parkguardai.ui.student;

import android.os.Bundle;
import android.text.TextUtils;
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
import com.example.parkguardai.data.model.UserProfile;
import com.example.parkguardai.data.model.VehicleItem;
import com.example.parkguardai.databinding.FragmentStudentVehicleDetailBinding;

import java.util.List;

/** Hiển thị chi tiết phương tiện thật của sinh viên từ Cloud Firestore. */
public class StudentVehicleDetailFragment extends Fragment {
    public static final String ARG_VEHICLE_ID = "arg_vehicle_id";

    private FragmentStudentVehicleDetailBinding binding;
    private StudentViewModel viewModel;
    private String vehicleId = "";
    private UserProfile cachedProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentVehicleDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StudentViewModel.class);

        if (getArguments() != null) {
            vehicleId = getArguments().getString(ARG_VEHICLE_ID, "");
        }

        binding.backButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigateUp());

        viewModel.getProfile().observe(getViewLifecycleOwner(), profile -> {
            cachedProfile = profile;
            List<VehicleItem> vehicles = viewModel.getVehicles().getValue();
            if (vehicles != null) {
                renderSelectedVehicle(vehicles);
            }
        });

        viewModel.getVehicles().observe(getViewLifecycleOwner(), this::renderSelectedVehicle);
        viewModel.loadProfile(requireContext());
        viewModel.loadVehicles(requireContext());
    }

    private void renderSelectedVehicle(@Nullable List<VehicleItem> vehicles) {
        if (binding == null || getContext() == null) return;
        if (vehicles == null || vehicles.isEmpty()) {
            binding.tvVehicleSpecs.setText("Không tìm thấy phương tiện trong tài khoản của bạn.");
            return;
        }
        VehicleItem target = null;
        if (!TextUtils.isEmpty(vehicleId)) {
            for (VehicleItem item : vehicles) {
                if (vehicleId.equals(item.id)) {
                    target = item;
                    break;
                }
            }
        }
        if (target == null) {
            target = vehicles.get(0);
        }

        if (target.isApproved()) {
            binding.tvVehicleStatusBadge.setText("ĐÃ XÁC MINH");
            binding.tvVehicleStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green));
        } else if (target.isRejectedOrBlocked()) {
            binding.tvVehicleStatusBadge.setText("BỊ TỪ CHỐI");
            binding.tvVehicleStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red));
        } else {
            binding.tvVehicleStatusBadge.setText("CHỜ XÁC MINH");
            binding.tvVehicleStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.warn_yellow));
        }

        String plate = target.plateNumber != null ? target.plateNumber.trim() : "--";
        String[] parts = plate.split("\\s+", 2);
        binding.tvPlateLine1.setText(parts[0]);
        binding.tvPlateLine2.setText(parts.length > 1 ? parts[1] : "");

        String ownerText = cachedProfile != null && cachedProfile.getFullName() != null
                ? cachedProfile.getFullName()
                + (cachedProfile.getStudentCode() != null && !cachedProfile.getStudentCode().isEmpty()
                ? " (" + cachedProfile.getStudentCode() + ")" : "")
                : "Chủ tài khoản";

        String specs = "• Biển số chuẩn hóa: " + target.plateCanonical + "\n"
                + "• Hãng & Dòng xe: " + target.model + "\n"
                + "• Loại phương tiện: " + (target.vehicleType != null && !target.vehicleType.isEmpty() ? target.vehicleType : "Xe máy") + "\n"
                + "• Màu sơn: " + (target.color != null && !target.color.isEmpty() ? target.color : "Chưa cập nhật") + "\n"
                + "• Chủ sở hữu: " + ownerText + "\n"
                + "• Ngày đăng ký: " + (target.submissionDate != null && !target.submissionDate.isEmpty() ? target.submissionDate : "Chưa rõ") + "\n"
                + "• Mã phương tiện: " + target.id;
        binding.tvVehicleSpecs.setText(specs);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
