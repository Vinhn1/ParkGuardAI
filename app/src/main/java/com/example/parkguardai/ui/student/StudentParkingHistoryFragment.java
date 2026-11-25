package com.example.parkguardai.ui.student;

import android.content.res.ColorStateList;
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
import com.example.parkguardai.data.model.StudentParkingRecord;
import com.example.parkguardai.databinding.FragmentStudentParkingHistoryBinding;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/** Màn hình Lịch sử vào/ra của sinh viên, đồng bộ dữ liệu thật từ Cloud Firestore. */
public class StudentParkingHistoryFragment extends Fragment {

    private FragmentStudentParkingHistoryBinding binding;
    private StudentViewModel viewModel;
    private int currentFilter = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentParkingHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StudentViewModel.class);

        StudentNavigation.bind(this, binding.bottomNav, R.id.studentHomeFragment);

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        binding.tabToday.setOnClickListener(v -> {
            selectFilter(0);
            viewModel.loadParkingHistory(requireContext(), 0);
        });
        binding.tab7Days.setOnClickListener(v -> {
            selectFilter(1);
            viewModel.loadParkingHistory(requireContext(), 1);
        });
        binding.tabMonth.setOnClickListener(v -> {
            selectFilter(2);
            viewModel.loadParkingHistory(requireContext(), 2);
        });

        viewModel.getParkingHistory().observe(getViewLifecycleOwner(), this::renderHistory);
        viewModel.loadParkingHistory(requireContext(), currentFilter);
    }

    private void renderHistory(List<StudentParkingRecord> records) {
        if (binding == null || getContext() == null) return;

        int entries = 0;
        int exits = 0;
        if (records != null) {
            for (StudentParkingRecord r : records) {
                if ("ENTRY".equalsIgnoreCase(r.getActionType())) entries++;
                else exits++;
            }
        }
        binding.tvEntryCount.setText(String.valueOf(entries));
        binding.tvTotalDuration.setText((entries + exits) + " lượt");

        binding.historyListContainer.removeAllViews();
        float density = getResources().getDisplayMetrics().density;

        if (records == null || records.isEmpty()) {
            MaterialCardView emptyCard = new MaterialCardView(requireContext());
            emptyCard.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            emptyCard.setRadius(14 * density);
            emptyCard.setStrokeWidth(Math.round(1 * density));
            emptyCard.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.pg_border_subtle));
            emptyCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pg_card_dark));

            TextView empty = new TextView(requireContext());
            empty.setText("Chưa có lượt xe vào/ra nào trong khoảng thời gian này.");
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_on_surface_muted));
            empty.setGravity(Gravity.CENTER);
            int p24 = Math.round(24 * density);
            empty.setPadding(p24, p24, p24, p24);
            emptyCard.addView(empty);
            binding.historyListContainer.addView(emptyCard);
            return;
        }

        String lastDateHeader = "";
        for (StudentParkingRecord rec : records) {
            if (!rec.getDateTitle().equals(lastDateHeader)) {
                lastDateHeader = rec.getDateTitle();
                TextView header = new TextView(requireContext());
                header.setText(lastDateHeader);
                header.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_on_surface_muted));
                header.setTextSize(13f);
                LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                hLp.bottomMargin = Math.round(8 * density);
                hLp.topMargin = binding.historyListContainer.getChildCount() > 0 ? Math.round(12 * density) : 0;
                header.setLayoutParams(hLp);
                binding.historyListContainer.addView(header);
            }

            boolean isEntry = "ENTRY".equalsIgnoreCase(rec.getActionType());

            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = Math.round(10 * density);
            card.setLayoutParams(cardLp);
            card.setRadius(14 * density);
            card.setStrokeWidth(Math.round(1 * density));
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.pg_border_subtle));
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pg_card_dark));

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int p14 = Math.round(14 * density);
            row.setPadding(p14, p14, p14, p14);

            View dot = new View(requireContext());
            int d10 = Math.round(10 * density);
            dot.setLayoutParams(new LinearLayout.LayoutParams(d10, d10));
            dot.setBackgroundResource(R.drawable.bg_badge_kpi_cyan);
            dot.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),
                    isEntry ? R.color.pg_emerald : R.color.pg_cyan)));
            row.addView(dot);

            LinearLayout info = new LinearLayout(requireContext());
            info.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            infoLp.setMarginStart(Math.round(12 * density));
            info.setLayoutParams(infoLp);

            TextView tvTimeAction = new TextView(requireContext());
            tvTimeAction.setText(rec.getTimeFormatted() + (isEntry ? " • Vào bãi" : " • Rời bãi"));
            tvTimeAction.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_on_surface));
            tvTimeAction.setTextSize(15f);
            tvTimeAction.setTypeface(null, android.graphics.Typeface.BOLD);
            info.addView(tvTimeAction);

            TextView tvGate = new TextView(requireContext());
            String gateText = rec.getLocationName() + (rec.getPlateNumber().isEmpty() ? "" : " • " + rec.getPlateNumber());
            tvGate.setText(gateText);
            tvGate.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_on_surface_muted));
            tvGate.setTextSize(13f);
            info.addView(tvGate);

            row.addView(info);

            TextView badge = new TextView(requireContext());
            badge.setText(R.string.student_history_status_valid);
            badge.setBackgroundResource(R.drawable.bg_badge_processed);
            badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_emerald));
            badge.setTextSize(12f);
            badge.setTypeface(null, android.graphics.Typeface.BOLD);
            badge.setPadding(Math.round(10 * density), Math.round(4 * density),
                    Math.round(10 * density), Math.round(4 * density));
            row.addView(badge);

            card.addView(row);
            binding.historyListContainer.addView(card);
        }
    }

    private void selectFilter(int index) {
        currentFilter = index;
        if (binding == null) return;
        TextView[] tabs = new TextView[]{binding.tabToday, binding.tab7Days, binding.tabMonth};
        for (int i = 0; i < tabs.length; i++) {
            if (i == index) {
                tabs[i].setBackgroundResource(R.drawable.bg_segmented_active);
                tabs[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            } else {
                tabs[i].setBackgroundResource(R.drawable.bg_segmented_inactive);
                tabs[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_on_surface_muted));
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
