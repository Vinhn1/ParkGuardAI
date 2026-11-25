package com.example.parkguardai.data.datasource;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

/** Thu các fix foreground trong cửa sổ pilot và chỉ nhận fix gắn được với ảnh chính. */
public final class CaptureLocationSource {
    private static final long TIMEOUT_MILLIS = 15_000L;
    private static final long MAX_CAPTURE_DIFFERENCE_NANOS = 5_000_000_000L;
    private static final long MAX_FIX_AGE_NANOS = 10_000_000_000L;

    public static final class Result {
        public final boolean accepted;
        public final boolean timeout;
        public final double latitude;
        public final double longitude;
        public final float accuracyMeters;
        public final long locationAtMillis;

        Result(boolean accepted, boolean timeout, @Nullable Location fix) {
            this.accepted = accepted;
            this.timeout = timeout;
            this.latitude = fix == null ? 0 : fix.getLatitude();
            this.longitude = fix == null ? 0 : fix.getLongitude();
            this.accuracyMeters = fix == null ? 0 : fix.getAccuracy();
            this.locationAtMillis = fix == null ? 0 : fix.getTime();
        }
    }

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FusedLocationProviderClient client;
    private final SettingsClient settingsClient;
    private Session active;
    private boolean warmingUp;
    private Location latestWarmFix;

    private final LocationCallback warmUpCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult result) {
            Location last = result.getLastLocation();
            if (last != null) {
                latestWarmFix = last;
            }
        }
    };

    public CaptureLocationSource(Context context) {
        this.context = context.getApplicationContext();
        client = LocationServices.getFusedLocationProviderClient(this.context);
        settingsClient = LocationServices.getSettingsClient(this.context);
    }

    /** Kiểm tra quyền truy cập vị trí chính xác (ACCESS_FINE_LOCATION). */
    public boolean hasFineLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Kiểm tra dịch vụ định vị (GPS) trên thiết bị đã bật ở mức độ chính xác cao hay chưa.
     * Nếu tắt, caller có thể bắt ResolvableApiException để hiển thị hộp thoại hệ thống bật GPS 1 chạm.
     */
    public Task<LocationSettingsResponse> checkLocationSettings() {
        LocationRequest highAccuracyRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
                .setMinUpdateIntervalMillis(500L)
                .build();
        LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(highAccuracyRequest)
                .setAlwaysShow(true)
                .build();
        return settingsClient.checkLocationSettings(settingsRequest);
    }

    /**
     * Khởi động làm ấm GPS foreground khi màn hình Camera đang mở để tránh trễ khóa vệ tinh lần đầu.
     */
    @SuppressLint("MissingPermission")
    public void startWarmUp() {
        if (warmingUp || !hasFineLocationPermission()) return;
        LocationRequest warmRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
                .setMinUpdateIntervalMillis(1_000L)
                .build();
        try {
            warmingUp = true;
            client.requestLocationUpdates(warmRequest, warmUpCallback, Looper.getMainLooper())
                    .addOnFailureListener(e -> warmingUp = false);
        } catch (SecurityException e) {
            warmingUp = false;
        }
    }

    /** Dừng làm ấm GPS khi màn hình Camera tạm dừng hoặc đóng để tiết kiệm pin. */
    public void stopWarmUp() {
        if (!warmingUp) return;
        warmingUp = false;
        client.removeLocationUpdates(warmUpCallback);
    }

    /** Kiểm tra đã có sẵn fix GPS đạt chuẩn vừa cập nhật hay chưa. */
    public boolean hasRecentWarmFix() {
        return isAccepted(latestWarmFix, SystemClock.elapsedRealtimeNanos());
    }

    /**
     * Chờ tối đa maxWaitMillis để chip GPS kịp khóa vệ tinh ngay sau khi người dùng vừa bật GPS,
     * tránh việc chụp ảnh ngay lập tức khi GPS vừa bật từ trạng thái ngủ.
     */
    public Task<Void> awaitInitialWarmFix(long maxWaitMillis) {
        if (hasRecentWarmFix()) {
            return com.google.android.gms.tasks.Tasks.forResult(null);
        }
        startWarmUp();
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        long deadline = SystemClock.elapsedRealtime() + Math.max(0L, maxWaitMillis);
        Runnable check = new Runnable() {
            @Override
            public void run() {
                if (tcs.getTask().isComplete()) return;
                if (hasRecentWarmFix() || SystemClock.elapsedRealtime() >= deadline) {
                    tcs.trySetResult(null);
                } else {
                    handler.postDelayed(this, 250L);
                }
            }
        };
        handler.postDelayed(check, 250L);
        return tcs.getTask();
    }

    @SuppressLint("MissingPermission")
    public Task<Result> request(long capturedElapsedNanos) {
        cancel();
        TaskCompletionSource<Result> completion = new TaskCompletionSource<>();
        if (!hasFineLocationPermission()) {
            completion.setResult(new Result(false, false, null));
            return completion.getTask();
        }

        // Kiểm tra ngay fix vừa làm ấm nếu nằm đúng trong cửa sổ thời gian & độ chính xác DEC-008A
        if (isAccepted(latestWarmFix, capturedElapsedNanos)) {
            completion.setResult(new Result(true, false, latestWarmFix));
            return completion.getTask();
        }

        Session session = new Session(completion, capturedElapsedNanos);
        active = session;

        // Kiểm tra song song lastLocation từ hệ thống nếu vừa được cập nhật sát thời điểm chụp
        try {
            client.getLastLocation().addOnSuccessListener(lastFix -> {
                if (!session.finished && isAccepted(lastFix, capturedElapsedNanos)) {
                    latestWarmFix = lastFix;
                    session.finish(true, false, lastFix);
                }
            });
        } catch (SecurityException ignored) { }

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
                .setMinUpdateIntervalMillis(200L)
                .setMaxUpdateAgeMillis(5_000L)
                .setDurationMillis(TIMEOUT_MILLIS)
                .build();
        handler.postDelayed(session.timeout, TIMEOUT_MILLIS);
        try {
            client.requestLocationUpdates(request, session.callback, Looper.getMainLooper())
                    .addOnFailureListener(failure -> session.finish(false, false, null))
                    .addOnSuccessListener(ignored -> {
                        // Hủy cả trường hợp callback kết thúc trước khi đăng ký hoàn tất.
                        if (session.finished) client.removeLocationUpdates(session.callback);
                    });
        } catch (SecurityException failure) {
            session.finish(false, false, null);
        }
        return completion.getTask();
    }

    public void cancel() {
        if (active != null) active.finish(false, false, null);
    }

    private final class Session {
        final TaskCompletionSource<Result> completion;
        final long capturedElapsedNanos;
        boolean receivedFix;
        Location lastRejectedFix;
        final Runnable timeout = () -> finish(false, !receivedFix, lastRejectedFix);
        final LocationCallback callback = new LocationCallback() {
            @Override public void onLocationResult(@NonNull LocationResult result) {
                for (Location fix : result.getLocations()) {
                    receivedFix = true;
                    latestWarmFix = fix;
                    if (isAccepted(fix, capturedElapsedNanos)) {
                        finish(true, false, fix);
                        return;
                    }
                    lastRejectedFix = fix;
                    // Nếu thời điểm đo của fix mới đã vượt quá cửa sổ 5 giây sau khi bấm chụp,
                    // mọi fix tiếp theo đều sẽ vượt ngưỡng -> kết thúc sớm thay vì treo 15 giây.
                    if (fix != null && fix.getElapsedRealtimeNanos() > 0
                            && fix.getElapsedRealtimeNanos() - capturedElapsedNanos > MAX_CAPTURE_DIFFERENCE_NANOS) {
                        finish(false, false, fix);
                        return;
                    }
                }
            }
        };
        boolean finished;

        Session(TaskCompletionSource<Result> completion, long capturedElapsedNanos) {
            this.completion = completion;
            this.capturedElapsedNanos = capturedElapsedNanos;
        }

        void finish(boolean accepted, boolean timeoutReached, @Nullable Location fix) {
            if (finished) return;
            finished = true;
            handler.removeCallbacks(timeout);
            client.removeLocationUpdates(callback);
            if (active == this) active = null;
            completion.setResult(new Result(accepted, timeoutReached, fix));
        }
    }

    private boolean isAccepted(@Nullable Location fix, long capturedElapsedNanos) {
        if (fix == null || !fix.hasAccuracy() || !Float.isFinite(fix.getAccuracy())
                || fix.getAccuracy() < 0f || fix.getAccuracy() > 30f
                || !Double.isFinite(fix.getLatitude()) || !Double.isFinite(fix.getLongitude())
                || Math.abs(fix.getLatitude()) > 90d || Math.abs(fix.getLongitude()) > 180d
                || fix.getTime() <= 0 || fix.getElapsedRealtimeNanos() <= 0) return false;
        long measured = fix.getElapsedRealtimeNanos();
        long now = SystemClock.elapsedRealtimeNanos();
        if (measured > now || now - measured > MAX_FIX_AGE_NANOS) return false;
        long difference = measured >= capturedElapsedNanos
                ? measured - capturedElapsedNanos : capturedElapsedNanos - measured;
        return difference <= MAX_CAPTURE_DIFFERENCE_NANOS;
    }
}
