package com.example.parkguardai.ui.guard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.databinding.FragmentGuardViolationFormBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Màn hình Ghi nhận vi phạm dành cho bảo vệ.
 * Theo DEC-008A/DEC-029, mọi biên bản vi phạm chính thức phải đi kèm ảnh hiện trường và định vị GPS thật từ Camera.
 */
public class GuardViolationFormFragment extends Fragment {
    public static final String ARG_PLATE = "plate";
    public static final String ARG_REASON = "reason";

    private FragmentGuardViolationFormBinding binding;

    private final String[] violationReasons = {
            "Đỗ sai khu vực quy định",
            "Chắn lối đi cứu hỏa & thoát hiểm",
            "Đỗ tại khu vực cấm đỗ",
            "Vi phạm quy định khác"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGuardViolationFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            String passedPlate = getArguments().getString("plate");
            if (passedPlate != null && !passedPlate.isEmpty()) {
                binding.tvViolationPlate.setText(passedPlate);
            }
            String passedReason = getArguments().getString("reason");
            if (passedReason != null && !passedReason.isEmpty()) {
                binding.tvSelectedReason.setText(passedReason);
            }
        }

        GuardNavigation.bind(this, binding.guardBottomNav, R.id.guardCameraFragment);

        View.OnClickListener cancelListener = v -> {
            if (!NavHostFragment.findNavController(this).navigateUp()) {
                requireActivity().onBackPressed();
            }
        };
        binding.btnBack.setOnClickListener(cancelListener);
        binding.btnCancel.setOnClickListener(cancelListener);

        binding.layoutSelectReason.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Chọn lý do vi phạm")
                    .setItems(violationReasons, (dialog, which) ->
                            binding.tvSelectedReason.setText(violationReasons[which]))
                    .show();
        });

        // Yêu cầu chụp ảnh hiện trường kèm GPS thật qua GuardCameraFragment -> GuardReviewFragment
        binding.btnSaveViolation.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Cần chụp ảnh bằng chứng hiện trường kèm định vị GPS trước khi gửi biên bản.",
                    Toast.LENGTH_LONG).show();
            try {
                NavHostFragment.findNavController(this).navigate(R.id.guardCameraFragment);
            } catch (Exception ignored) { }
        });
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
