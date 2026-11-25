package com.example.parkguardai.ui.student;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.data.model.VehicleItem;
import com.example.parkguardai.databinding.FragmentStudentVehiclesBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/** Hiển thị danh sách phương tiện thật của sinh viên từ Cloud Firestore. */
public class StudentVehiclesFragment extends Fragment {
    private FragmentStudentVehiclesBinding binding;
    private StudentViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentVehiclesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StudentViewModel.class);
        StudentNavigation.bind(this, binding.bottomNav, R.id.studentVehiclesFragment);

        binding.addVehicleButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigate(R.id.studentAddVehicleFragment));

        viewModel.getVehicles().observe(getViewLifecycleOwner(), this::renderVehicles);
        viewModel.loadVehicles(requireContext());
    }

    private void renderVehicles(List<VehicleItem> vehicles) {
        if (binding == null || getContext() == null) return;
        binding.vehiclesListContainer.removeAllViews();
        float density = getResources().getDisplayMetrics().density;

        if (vehicles == null || vehicles.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("Bạn chưa đăng ký phương tiện nào. Hãy bấm nút bên dưới để đăng ký xe.");
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_on_surface_muted));
            empty.setTextSize(14);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, (int) (32 * density), 0, (int) (32 * density));
            binding.vehiclesListContainer.addView(empty);
            return;
        }

        for (VehicleItem vehicle : vehicles) {
            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = (int) (12 * density);
            card.setLayoutParams(params);
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pg_surface_card));
            card.setRadius(20 * density);
            card.setStrokeWidth((int) (1 * density));
            card.setStrokeColor(ContextCompat.getColor(requireContext(),
                    vehicle.isPending() ? R.color.warn_yellow : R.color.pg_outline));

            LinearLayout box = new LinearLayout(requireContext());
            box.setOrientation(LinearLayout.VERTICAL);
            int pad = (int) (18 * density);
            box.setPadding(pad, pad, pad, pad);

            LinearLayout header = new LinearLayout(requireContext());
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvPlate = new TextView(requireContext());
            tvPlate.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            tvPlate.setText(vehicle.plateNumber);
            tvPlate.setTextSize(20);
            tvPlate.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
            tvPlate.setTextColor(ContextCompat.getColor(requireContext(),
                    vehicle.isApproved() ? R.color.accent_cyan
                            : vehicle.isRejectedOrBlocked() ? R.color.error_red : R.color.warn_yellow));
            header.addView(tvPlate);

            TextView badge = new TextView(requireContext());
            badge.setBackgroundResource(R.drawable.bg_badge_status);
            badge.setPadding((int) (10 * density), (int) (4 * density), (int) (10 * density), (int) (4 * density));
            badge.setTextSize(11);
            if (vehicle.isApproved()) {
                badge.setText("ĐÃ XÁC MINH");
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green));
            } else if (vehicle.isRejectedOrBlocked()) {
                badge.setText("TỪ CHỐI");
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red));
            } else {
                badge.setText("CHỜ DUYỆT");
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.warn_yellow));
            }
            header.addView(badge);
            box.addView(header);

            TextView tvDesc = new TextView(requireContext());
            String desc = vehicle.model
                    + (vehicle.color != null && !vehicle.color.isEmpty() ? " • " + vehicle.color : "");
            tvDesc.setText(desc);
            tvDesc.setTextSize(14);
            tvDesc.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_on_surface));
            tvDesc.setPadding(0, (int) (6 * density), 0, 0);
            box.addView(tvDesc);

            TextView tvSub = new TextView(requireContext());
            tvSub.setText("Loại xe: " + (vehicle.vehicleType != null && !vehicle.vehicleType.isEmpty() ? vehicle.vehicleType : "Xe máy")
                    + (vehicle.submissionDate != null && !vehicle.submissionDate.isEmpty() ? " • Đăng ký: " + vehicle.submissionDate : ""));
            tvSub.setTextSize(12);
            tvSub.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_on_surface_muted));
            tvSub.setPadding(0, (int) (2 * density), 0, 0);
            box.addView(tvSub);

            MaterialButton btnDetail = new MaterialButton(requireContext(), null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            btnParams.topMargin = (int) (12 * density);
            btnDetail.setLayoutParams(btnParams);
            btnDetail.setText("Xem chi tiết xe");
            btnDetail.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString(StudentVehicleDetailFragment.ARG_VEHICLE_ID, vehicle.id);
                NavHostFragment.findNavController(this).navigate(R.id.studentVehicleDetailFragment, args);
            });
            box.addView(btnDetail);

            card.addView(box);
            binding.vehiclesListContainer.addView(card);
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
