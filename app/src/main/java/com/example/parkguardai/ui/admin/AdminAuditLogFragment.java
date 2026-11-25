package com.example.parkguardai.ui.admin;

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
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.data.repository.AdminRepository;
import com.example.parkguardai.databinding.FragmentAdminAuditLogBinding;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Hiển thị nhật ký kiểm toán thật từ Cloud Firestore (collection auditLogs). */
public class AdminAuditLogFragment extends Fragment {
    private FragmentAdminAuditLogBinding binding;
    private AdminRepository adminRepository;
    private List<DocumentSnapshot> loadedLogs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminAuditLogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adminRepository = AdminRepository.getInstance(requireContext());

        binding.backButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigateUp());

        binding.exportLogButton.setOnClickListener(v -> exportLogsToCsv());

        loadAuditLogs();
    }

    private void loadAuditLogs() {
        adminRepository.getAuditLogs().addOnSuccessListener(docs -> {
            if (binding == null || !isAdded()) return;
            loadedLogs = docs != null ? docs : new ArrayList<>();
            renderLogs(loadedLogs);
        }).addOnFailureListener(e -> {
            if (binding == null || !isAdded()) return;
            Snackbar.make(binding.getRoot(),
                    "Không thể tải nhật ký kiểm toán: " + e.getMessage(),
                    Snackbar.LENGTH_LONG).show();
        });
    }

    private void renderLogs(List<DocumentSnapshot> docs) {
        binding.auditLogContainer.removeAllViews();
        float density = getResources().getDisplayMetrics().density;

        if (docs.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("Chưa có bản ghi nhật ký kiểm toán nào trong hệ thống.");
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_on_surface_muted));
            empty.setTextSize(13);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, (int) (32 * density), 0, (int) (32 * density));
            binding.auditLogContainer.addView(empty);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss - dd/MM/yyyy", Locale.getDefault());
        for (DocumentSnapshot doc : docs) {
            String action = doc.getString("action");
            String actorUid = doc.getString("actorUid");
            String targetPath = doc.getString("targetPath");
            String reason = doc.getString("reason");
            Date at = doc.getDate("at");

            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = (int) (10 * density);
            card.setLayoutParams(cardParams);
            card.setRadius(16 * density);
            card.setStrokeWidth((int) (1 * density));
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.pg_outline));
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pg_surface_card));

            LinearLayout box = new LinearLayout(requireContext());
            box.setOrientation(LinearLayout.VERTICAL);
            int pad = (int) (14 * density);
            box.setPadding(pad, pad, pad, pad);

            LinearLayout header = new LinearLayout(requireContext());
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvAction = new TextView(requireContext());
            tvAction.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            tvAction.setText(action != null ? action : "AUDIT_EVENT");
            tvAction.setTextSize(13);
            tvAction.setTypeface(null, android.graphics.Typeface.BOLD);
            int actionColor = (action != null && (action.contains("REJECT") || action.contains("DELETE") || action.contains("CANCEL")))
                    ? ContextCompat.getColor(requireContext(), R.color.error_red)
                    : ContextCompat.getColor(requireContext(), R.color.brand_teal);
            tvAction.setTextColor(actionColor);
            header.addView(tvAction);

            TextView tvTime = new TextView(requireContext());
            tvTime.setText(at != null ? sdf.format(at) : "--");
            tvTime.setTextSize(11);
            tvTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_on_surface_muted));
            header.addView(tvTime);

            box.addView(header);

            StringBuilder body = new StringBuilder();
            body.append("• ID sự kiện: ").append(doc.getId());
            if (actorUid != null && !actorUid.isEmpty()) {
                body.append("\n• Tác nhân (UID): ").append(actorUid);
            }
            if (targetPath != null && !targetPath.isEmpty()) {
                body.append("\n• Đối tượng: ").append(targetPath);
            }
            if (reason != null && !reason.isEmpty()) {
                body.append("\n• Ghi chú: ").append(reason);
            }

            TextView tvBody = new TextView(requireContext());
            tvBody.setText(body.toString());
            tvBody.setTextSize(12);
            tvBody.setTextColor(ContextCompat.getColor(requireContext(), R.color.pg_on_surface));
            tvBody.setPadding(0, (int) (6 * density), 0, 0);
            box.addView(tvBody);

            card.addView(box);
            binding.auditLogContainer.addView(card);
        }
    }

    private void exportLogsToCsv() {
        if (loadedLogs.isEmpty()) {
            Snackbar.make(binding.getRoot(), "Không có bản ghi kiểm toán nào để xuất.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        try {
            File dir = requireContext().getExternalFilesDir(null);
            if (dir == null) dir = requireContext().getCacheDir();
            String fileName = "audit_log_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".csv";
            File csvFile = new File(dir, fileName);
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            try (FileWriter writer = new FileWriter(csvFile)) {
                writer.write("id,action,actorUid,targetPath,reason,at\n");
                for (DocumentSnapshot doc : loadedLogs) {
                    Date at = doc.getDate("at");
                    writer.write(csvEscape(doc.getId()) + ","
                            + csvEscape(doc.getString("action")) + ","
                            + csvEscape(doc.getString("actorUid")) + ","
                            + csvEscape(doc.getString("targetPath")) + ","
                            + csvEscape(doc.getString("reason")) + ","
                            + csvEscape(at != null ? iso.format(at) : "") + "\n");
                }
            }
            Snackbar.make(binding.getRoot(),
                    "Đã xuất " + loadedLogs.size() + " bản ghi kiểm toán: " + fileName,
                    Snackbar.LENGTH_LONG).show();
        } catch (Exception e) {
            Snackbar.make(binding.getRoot(),
                    "Lỗi xuất file CSV: " + e.getMessage(),
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private String csvEscape(@Nullable String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
