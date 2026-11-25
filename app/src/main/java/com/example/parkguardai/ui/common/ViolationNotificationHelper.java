package com.example.parkguardai.ui.common;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.parkguardai.MainActivity;
import com.example.parkguardai.R;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashSet;
import java.util.Set;

/**
 * Lắng nghe thông báo vi phạm mới trên Cloud Firestore và phát thông báo đẩy hệ thống Android
 * khi tài khoản Sinh viên bật tùy chọn Thông báo đẩy (hoạt động miễn phí trên Firebase Spark).
 */
public final class ViolationNotificationHelper {
    private static final String PREFS = "parkguard_notif_prefs";
    private static final String KEY_PUSH_ENABLED = "push_enabled";
    private static final String KEY_NOTIFIED_IDS = "notified_ids";
    private static final String CHANNEL_ID = "parkguard_violations";
    private static final String CHANNEL_NAME = "Thông báo vi phạm bãi xe";

    private static ListenerRegistration registration;
    private static ListenerRegistration securityAlertRegistration;
    private static String activeUid;
    private static String activeAlertUid;

    private ViolationNotificationHelper() { }

    public static boolean isPushEnabled(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_PUSH_ENABLED, true);
    }

    public static void setPushEnabled(@NonNull Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_PUSH_ENABLED, enabled).apply();
        if (enabled) {
            startRealtimeListener(context);
        } else {
            stopRealtimeListener();
        }
    }

    public static boolean hasNotificationPermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void ensureChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo khi phương tiện của bạn có biên bản vi phạm mới.");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static synchronized void startRealtimeListener(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        if (!isPushEnabled(appContext)) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            stopRealtimeListener();
            return;
        }
        if (registration != null && user.getUid().equals(activeUid)) {
            return;
        }
        stopRealtimeListener();
        activeUid = user.getUid();
        ensureChannel(appContext);

        registration = FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("recipientUid", activeUid)
                .whereEqualTo("read", false)
                .limit(20)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    if (!isPushEnabled(appContext) || !hasNotificationPermission(appContext)) return;

                    SharedPreferences p = prefs(appContext);
                    Set<String> notified = new HashSet<>(
                            p.getStringSet(KEY_NOTIFIED_IDS, new HashSet<>()));
                    boolean changed = false;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String id = doc.getId();
                        if (notified.contains(id)) continue;
                        notified.add(id);
                        changed = true;

                        String title = doc.getString("title");
                        String message = doc.getString("message");
                        if (title == null || title.isEmpty()) title = "Ghi nhận vi phạm mới";
                        if (message == null || message.isEmpty()) {
                            message = "Bạn có thông báo mới từ hệ thống ParkGuard AI.";
                        }
                        showSystemNotification(appContext, id.hashCode(), title, message);
                    }

                    if (changed) {
                        p.edit().putStringSet(KEY_NOTIFIED_IDS, notified).apply();
                    }
                });
    }

    public static synchronized void stopRealtimeListener() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        if (securityAlertRegistration != null) {
            securityAlertRegistration.remove();
            securityAlertRegistration = null;
        }
        activeUid = null;
        activeAlertUid = null;
    }

    /** Lắng nghe cảnh báo an ninh mới cho Bảo vệ và Quản trị viên để phát thông báo đẩy hệ thống. */
    public static synchronized void startSecurityAlertListener(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        if (!isPushEnabled(appContext)) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            stopRealtimeListener();
            return;
        }
        if (securityAlertRegistration != null && user.getUid().equals(activeAlertUid)) {
            return;
        }
        if (securityAlertRegistration != null) {
            securityAlertRegistration.remove();
            securityAlertRegistration = null;
        }
        activeAlertUid = user.getUid();
        ensureChannel(appContext);

        securityAlertRegistration = FirebaseFirestore.getInstance()
                .collection("security_alerts")
                .whereEqualTo("status", "UNHANDLED")
                .limit(15)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    if (!isPushEnabled(appContext) || !hasNotificationPermission(appContext)) return;

                    SharedPreferences p = prefs(appContext);
                    Set<String> notified = new HashSet<>(
                            p.getStringSet(KEY_NOTIFIED_IDS, new HashSet<>()));
                    boolean changed = false;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String id = "alert_" + doc.getId();
                        if (notified.contains(id)) continue;
                        notified.add(id);
                        changed = true;

                        String title = doc.getString("title");
                        String desc = doc.getString("description");
                        if (title == null || title.isEmpty()) title = "Cảnh báo an ninh bãi xe";
                        if (desc == null || desc.isEmpty()) desc = "Phát hiện cảnh báo mới cần kiểm tra.";
                        showSystemNotification(appContext, id.hashCode(), title, desc);
                    }

                    if (changed) {
                        p.edit().putStringSet(KEY_NOTIFIED_IDS, notified).apply();
                    }
                });
    }

    /** Gắn logic bật/tắt thông báo đẩy dùng chung cho cả 3 vai trò. */
    public static void bindSwitch(@NonNull Fragment fragment,
                                  @NonNull MaterialSwitch switchView,
                                  @NonNull ActivityResultLauncher<String> permissionLauncher) {
        Context context = fragment.requireContext();
        boolean pushOn = isPushEnabled(context) && hasNotificationPermission(context);
        switchView.setChecked(pushOn);
        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            Context ctx = fragment.requireContext();
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        && !hasNotificationPermission(ctx)) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    return;
                }
                setPushEnabled(ctx, true);
                Toast.makeText(ctx, "Đã bật thông báo đẩy", Toast.LENGTH_SHORT).show();
            } else {
                setPushEnabled(ctx, false);
                Toast.makeText(ctx, "Đã tắt thông báo đẩy", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static void showSystemNotification(@NonNull Context context,
                                               int notifyId,
                                               @NonNull String title,
                                               @NonNull String message) {
        if (!hasNotificationPermission(context)) return;

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notifyId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_guard_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(notifyId, builder.build());
        } catch (SecurityException ignored) { }
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
