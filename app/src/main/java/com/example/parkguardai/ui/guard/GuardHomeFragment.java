package com.example.parkguardai.ui.guard;

import android.os.Bundle;
import android.animation.ValueAnimator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.ui.common.LogoutConfirmationDialog;
import com.example.parkguardai.databinding.FragmentGuardHomeBinding;
import com.example.parkguardai.data.model.GuardDraft;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/** Trang tác nghiệp và hồ sơ của tài khoản bảo vệ đã được cấp quyền. */
public final class GuardHomeFragment extends Fragment {
    private static final String SELECTED_TAB = "selectedTab";
    private FragmentGuardHomeBinding binding;
    private GuardHomeViewModel viewModel;
    private int selectedTab = R.id.guard_nav_home;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGuardHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(GuardHomeViewModel.class);
        binding.captureButton.setOnClickListener(ignored -> openCamera());
        binding.signOutButton.setOnClickListener(ignored ->
                LogoutConfirmationDialog.show(this));
        binding.resetPasswordButton.setOnClickListener(ignored -> {
            binding.resetPasswordButton.setEnabled(false);
            viewModel.sendPasswordReset().addOnCompleteListener(task -> {
                if (binding == null) return;
                binding.resetPasswordButton.setEnabled(true);
                Snackbar.make(binding.getRoot(), task.isSuccessful()
                        ? R.string.guard_profile_reset_sent : R.string.guard_profile_reset_error,
                        Snackbar.LENGTH_LONG).show();
            });
        });
        binding.refreshRecordsButton.setOnClickListener(ignored -> viewModel.refreshRecords());
        binding.recordsSearchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                GuardHomeViewModel.RecordsState current = viewModel.getRecords().getValue();
                if (current != null) renderRecords(current);
            }
            @Override public void afterTextChanged(Editable text) { }
        });
        binding.recordsFilterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            GuardHomeViewModel.RecordsState current = viewModel.getRecords().getValue();
            if (current != null) renderRecords(current);
        });
        binding.quickScanShortcut.setOnClickListener(v -> openCamera());
        binding.recordsShortcut.setOnClickListener(v -> selectTab(R.id.guard_nav_records));
        binding.mapShortcut.setOnClickListener(v -> selectTab(R.id.guard_nav_map));
        binding.viewAllRecordsButton.setOnClickListener(v -> selectTab(R.id.guard_nav_records));
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            // Tab Quét nhanh là lối vào luồng chụp ảnh chính, không phải một panel riêng.
            if (item.getItemId() == R.id.guard_nav_quick_scan) {
                openCamera();
                return false;
            }
            return showTab(item.getItemId());
        });
        // Quay lại từ camera/chi tiết thì giữ tab đang xem; trạng thái cũ trỏ tới Quét nhanh thì về Trang chủ.
        int initialTab = savedInstanceState == null ? selectedTab
                : savedInstanceState.getInt(SELECTED_TAB, R.id.guard_nav_home);
        if (initialTab == R.id.guard_nav_quick_scan) initialTab = R.id.guard_nav_home;
        showTab(initialTab);
        binding.bottomNavigation.setSelectedItemId(initialTab);
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getRecords().observe(getViewLifecycleOwner(), this::renderRecords);
        viewModel.refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.refresh();
    }

    private boolean showTab(int id) {
        if (id != R.id.guard_nav_home && id != R.id.guard_nav_records
                && id != R.id.guard_nav_map && id != R.id.guard_nav_profile) return false;
        boolean changed = selectedTab != id;
        selectedTab = id;
        binding.overviewPanel.setVisibility(id == R.id.guard_nav_home ? View.VISIBLE : View.GONE);
        binding.recordsPanel.setVisibility(id == R.id.guard_nav_records ? View.VISIBLE : View.GONE);
        binding.profilePanel.setVisibility(id == R.id.guard_nav_profile ? View.VISIBLE : View.GONE);
        boolean feature = id == R.id.guard_nav_map;
        binding.featureContainer.setVisibility(feature ? View.VISIBLE : View.GONE);
        Fragment current = getChildFragmentManager().findFragmentById(R.id.featureContainer);
        if (!feature) {
            if (current != null) getChildFragmentManager().beginTransaction().remove(current).commitNow();
            if (changed) animatePanel(id == R.id.guard_nav_home ? binding.overviewPanel
                    : id == R.id.guard_nav_records ? binding.recordsPanel : binding.profilePanel);
            return true;
        }
        if (!(current instanceof GuardMapFragment)) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.featureContainer, new GuardMapFragment()).commitNow();
        }
        if (changed) animatePanel(binding.featureContainer);
        return true;
    }

    private void selectTab(int id) {
        binding.bottomNavigation.setSelectedItemId(id);
    }

    /** Mở màn chụp ảnh chính; chỉ cho phép khi quyền bảo vệ đã được xác nhận từ server. */
    private void openCamera() {
        if (binding == null) return;
        GuardHomeViewModel.State state = viewModel.getState().getValue();
        if (state == null || !state.authorized) {
            Snackbar.make(binding.getRoot(), R.string.guard_camera_access_unavailable,
                    Snackbar.LENGTH_LONG).show();
            return;
        }
        NavController nav = NavHostFragment.findNavController(this);
        // Chặn chạm liên tiếp gây điều hướng hai lần.
        if (nav.getCurrentDestination() == null
                || nav.getCurrentDestination().getId() != R.id.guardHomeFragment) return;
        nav.navigate(R.id.action_guard_to_camera);
    }

    private void animatePanel(View panel) {
        if (!ValueAnimator.areAnimatorsEnabled()) return;
        panel.animate().cancel();
        panel.setAlpha(0f);
        panel.setTranslationY(12f * getResources().getDisplayMetrics().density);
        panel.animate().alpha(1f).translationY(0f).setDuration(220L).start();
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(SELECTED_TAB, selectedTab);
        super.onSaveInstanceState(outState);
    }

    private void render(GuardHomeViewModel.State state) {
        binding.captureButton.setEnabled(state.authorized);
        binding.profileEmailText.setText(state.email);
        if (state.revoked) {
            Snackbar.make(binding.getRoot(), R.string.guard_profile_session_revoked,
                    Snackbar.LENGTH_LONG).show();
            signOut();
            return;
        }
        String scopes = state.scopes == null ? getString(state.loading
                ? R.string.guard_profile_scope_pending : R.string.guard_profile_scope_error)
                : String.join(", ", state.scopes);
        binding.homeScopeText.setText(scopes);
        binding.profileScopeText.setText(scopes);
    }

    /** Thu hồi quyền làm kết thúc phiên tác nghiệp ngay. */
    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        NavOptions options = new NavOptions.Builder()
                .setPopUpTo(R.id.loginFragment, true).build();
        NavHostFragment.findNavController(this).navigate(R.id.loginFragment, null, options);
    }

    private void renderRecords(GuardHomeViewModel.RecordsState state) {
        if (binding == null) return;
        String query = binding.recordsSearchInput.getText() == null ? ""
                : binding.recordsSearchInput.getText().toString().trim().toUpperCase(Locale.ROOT);
        int filter = binding.recordsFilterGroup.getCheckedChipId();
        boolean showDrafts = filter != R.id.recordsFilterServer;
        boolean showServer = filter != R.id.recordsFilterDrafts;
        binding.homeDraftCountText.setText(getString(R.string.guard_home_draft_count,
                state.drafts == null ? 0 : state.drafts.size()));
        binding.homeRecordCountText.setText(state.loading || state.error || state.records == null
                ? getString(R.string.guard_home_record_count_loading)
                : getString(R.string.guard_home_record_count, state.records.size()));
        binding.homeRecentContainer.removeAllViews();
        if (state.records == null || state.records.isEmpty()) {
            addText(binding.homeRecentContainer, getString(state.loading
                    ? R.string.guard_records_loading : state.error
                    ? R.string.guard_records_error : R.string.guard_records_empty));
        } else {
            for (int index = 0; index < Math.min(3, state.records.size()); index++) {
                DocumentSnapshot recent = state.records.get(index);
                Date captured = recent.getDate("capturedAt");
                String title = getString(R.string.guard_record_row,
                        recent.getString("plateFinal") == null ? "" : recent.getString("plateFinal"),
                        captured == null ? "" : date(captured.getTime()));
                addCard(binding.homeRecentContainer, title,
                        getString(R.string.guard_detail_pending), R.string.guard_detail_title,
                        () -> {
                            Bundle args = new Bundle();
                            args.putString("recordId", recent.getId());
                            NavHostFragment.findNavController(this).navigate(
                                    R.id.action_guard_to_detail, args);
                        }, null);
            }
        }
        binding.draftContainer.removeAllViews();
        binding.draftSectionTitle.setVisibility(showDrafts ? View.VISIBLE : View.GONE);
        binding.draftContainer.setVisibility(showDrafts ? View.VISIBLE : View.GONE);
        if (showDrafts && state.drafts != null) {
            for (GuardDraft draft : state.drafts) {
                if (!matches(query, draft.plate, draft.id)) continue;
                String title = getString(R.string.guard_draft_row, date(draft.capturedAtMillis));
                String description = (draft.sent ? getString(R.string.guard_local_sent)
                        : getString(R.string.guard_local_pending))
                        + (draft.plate.isEmpty() ? "" : " · " + draft.plate);
                addCard(binding.draftContainer, title, description,
                        draft.sent ? R.string.guard_detail_title : R.string.guard_draft_open,
                        () -> {
                            Bundle args = new Bundle();
                            if (draft.sent) {
                                args.putString("recordId", draft.id);
                                NavHostFragment.findNavController(this).navigate(
                                        R.id.action_guard_to_detail, args);
                            } else {
                                args.putString("draftId", draft.id);
                                NavHostFragment.findNavController(this).navigate(
                                        R.id.action_guard_to_review, args);
                            }
                        }, () -> new MaterialAlertDialogBuilder(requireContext())
                            .setMessage(draft.sent ? R.string.guard_local_sent_delete_confirm
                                    : R.string.guard_draft_delete_confirm)
                            .setNegativeButton(R.string.action_cancel, null)
                            .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                                if (!viewModel.deleteDraft(draft)) Snackbar.make(binding.getRoot(),
                                        R.string.guard_draft_delete_failed, Snackbar.LENGTH_LONG).show();
                            }).show());
            }
        }
        if (showDrafts && binding.draftContainer.getChildCount() == 0) {
            addText(binding.draftContainer, getString(query.isEmpty()
                    ? R.string.guard_draft_empty : R.string.guard_records_search_empty));
        }
        binding.recordsServerTitle.setVisibility(showServer ? View.VISIBLE : View.GONE);
        binding.recordsStatus.setVisibility(showServer ? View.VISIBLE : View.GONE);
        binding.recordsContainer.setVisibility(showServer ? View.VISIBLE : View.GONE);
        binding.recordsContainer.removeAllViews();
        if (state.loading) binding.recordsStatus.setText(R.string.guard_records_loading);
        else if (state.error) binding.recordsStatus.setText(R.string.guard_records_error);
        else if (state.records == null || state.records.isEmpty()) {
            binding.recordsStatus.setText(R.string.guard_records_empty);
        } else {
            binding.recordsStatus.setText(R.string.guard_records_title);
            for (DocumentSnapshot record : state.records) {
                if (!matches(query, record.getString("plateFinal"), record.getId())) continue;
                Date captured = record.getDate("capturedAt");
                String title = getString(R.string.guard_record_row,
                        record.getString("plateFinal") == null ? "" : record.getString("plateFinal"),
                        captured == null ? "" : date(captured.getTime()));
                addCard(binding.recordsContainer, title, getString(R.string.guard_detail_pending),
                        R.string.guard_detail_title,
                        () -> {
                            Bundle args = new Bundle();
                            args.putString("recordId", record.getId());
                            NavHostFragment.findNavController(this).navigate(R.id.action_guard_to_detail, args);
                        }, null);
            }
            if (binding.recordsContainer.getChildCount() == 0) {
                binding.recordsStatus.setText(R.string.guard_records_search_empty);
            } else {
                binding.recordsStatus.setVisibility(View.GONE);
            }
        }
    }

    private boolean matches(String query, String plate, String id) {
        return query.isEmpty()
                || (plate != null && plate.toUpperCase(Locale.ROOT).contains(query))
                || (id != null && id.toUpperCase(Locale.ROOT).contains(query));
    }

    private void addText(LinearLayout container, String value) {
        TextView text = new TextView(requireContext());
        text.setText(value);
        text.setPadding(0, 16, 0, 16);
        container.addView(text);
    }

    private void addCard(LinearLayout container, String title, String description,
                         int openLabel,
                         Runnable open, Runnable delete) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int) (10 * getResources().getDisplayMetrics().density);
        card.setLayoutParams(params);
        card.setRadius(20 * getResources().getDisplayMetrics().density);
        card.setCardBackgroundColor(requireContext().getColor(R.color.pg_surface_card));
        card.setStrokeColor(requireContext().getColor(R.color.pg_outline));
        card.setStrokeWidth((int) getResources().getDisplayMetrics().density);
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        content.setPadding(pad, pad, pad, pad);
        TextView heading = new TextView(requireContext());
        heading.setText(title);
        heading.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        heading.setTextColor(requireContext().getColor(R.color.pg_on_surface));
        content.addView(heading);
        if (description != null && !description.isEmpty()) {
            TextView sub = new TextView(requireContext());
            sub.setText(description);
            sub.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
            sub.setTextColor(requireContext().getColor(R.color.pg_on_surface_muted));
            sub.setPadding(0, (int) (6 * getResources().getDisplayMetrics().density), 0, 0);
            content.addView(sub);
        }
        MaterialButton openButton = new MaterialButton(requireContext());
        openButton.setCornerRadius((int) (14 * getResources().getDisplayMetrics().density));
        openButton.setText(openLabel);
        openButton.setOnClickListener(v -> open.run());
        content.addView(openButton);
        if (delete != null) {
            MaterialButton deleteButton = new MaterialButton(requireContext());
            deleteButton.setText(R.string.guard_draft_delete);
            deleteButton.setOnClickListener(v -> delete.run());
            content.addView(deleteButton);
        }
        card.addView(content);
        container.addView(card);
    }

    private String date(long millis) {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                new Locale("vi", "VN")).format(new Date(millis));
    }

    @Override public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
