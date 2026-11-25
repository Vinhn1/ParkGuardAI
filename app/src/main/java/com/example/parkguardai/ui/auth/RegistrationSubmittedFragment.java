package com.example.parkguardai.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.databinding.FragmentRegistrationSubmittedBinding;

/** Giải thích hồ sơ đã lưu và xe vẫn chờ xét duyệt thủ công. */
public class RegistrationSubmittedFragment extends Fragment {
    private FragmentRegistrationSubmittedBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegistrationSubmittedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.continueButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigate(R.id.studentHomeFragment));
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
