package com.example.parkguardai.ui.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.ViewParent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.Icon;
import org.maplibre.android.annotations.IconFactory;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.annotations.PolylineOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Tiện ích cấu hình khung bản đồ MapLibre cố định cho khu vực Trường Đại học Cửu Long
 * (Quốc lộ 1A, xã Phú Quới, huyện Long Hồ, tỉnh Vĩnh Long) không cần Google Cloud API Key.
 */
public final class CampusMapHelper {

    public static final String CAMPUS_NAME = "Trường Đại học Cửu Long";
    public static final double CAMPUS_CENTER_LAT = 10.1696d;
    public static final double CAMPUS_CENTER_LNG = 105.9276d;

    public static final LatLng CAMPUS_CENTER = new LatLng(CAMPUS_CENTER_LAT, CAMPUS_CENTER_LNG);

    /** Giới hạn khung nhìn khuôn viên Trường Đại học Cửu Long và các bãi xe xung quanh. */
    public static final LatLngBounds CAMPUS_BOUNDS = new LatLngBounds.Builder()
            .include(new LatLng(10.1650d, 105.9225d))
            .include(new LatLng(10.1742d, 105.9327d))
            .build();

    public static final double DEFAULT_ZOOM = 16.5d;
    public static final double MIN_ZOOM = 14.5d;
    public static final double MAX_ZOOM = 19.0d;

    /** Style vector OpenFreeMap Bright độ tương phản cao, miễn phí, không cần API Key. */
    public static final String CAMPUS_STYLE_URI = "https://tiles.openfreemap.org/styles/bright";

    private static final String SATELLITE_STYLE_JSON = "{\n"
            + "  \"version\": 8,\n"
            + "  \"name\": \"ParkGuard-CuuLong-Satellite\",\n"
            + "  \"sources\": {\n"
            + "    \"esri-sat\": {\n"
            + "      \"type\": \"raster\",\n"
            + "      \"tiles\": [\"https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}\"],\n"
            + "      \"tileSize\": 256,\n"
            + "      \"maxzoom\": 19,\n"
            + "      \"attribution\": \"© Esri, Maxar, Earthstar Geographics\"\n"
            + "    }\n"
            + "  },\n"
            + "  \"layers\": [\n"
            + "    {\n"
            + "      \"id\": \"esri-sat-layer\",\n"
            + "      \"type\": \"raster\",\n"
            + "      \"source\": \"esri-sat\",\n"
            + "      \"minzoom\": 0,\n"
            + "      \"maxzoom\": 20\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    private CampusMapHelper() { }

    public static void ensureInitialized(@NonNull Context context) {
        MapLibre.getInstance(context.getApplicationContext());
    }

    @NonNull
    public static Style.Builder buildCampusStyle() {
        return new Style.Builder().fromUri(CAMPUS_STYLE_URI);
    }

    @NonNull
    public static Style.Builder buildSatelliteStyle() {
        return new Style.Builder().fromJson(SATELLITE_STYLE_JSON);
    }

    /**
     * Khóa khung camera trong khuôn viên Trường Đại học Cửu Long, vẽ ranh giới khu vực
     * và đặt điểm đánh dấu tại tọa độ vi phạm (hoặc tâm trường nếu chưa có tọa độ).
     */
    public static void configureCampusFrame(@NonNull MapLibreMap map,
                                            @Nullable Double lat,
                                            @Nullable Double lng,
                                            @Nullable String markerTitle) {
        map.setLatLngBoundsForCameraTarget(CAMPUS_BOUNDS);
        map.setMinZoomPreference(MIN_ZOOM);
        map.setMaxZoomPreference(MAX_ZOOM);

        map.clear();
        drawCampusPerimeter(map);

        boolean hasValidPoint = isValidCoordinate(lat, lng);
        LatLng target = hasValidPoint ? new LatLng(lat, lng) : CAMPUS_CENTER;
        String title = (markerTitle != null && !markerTitle.isEmpty())
                ? markerTitle
                : (hasValidPoint ? "Vị trí ghi nhận vi phạm" : CAMPUS_NAME);

        map.addMarker(new MarkerOptions()
                .position(target)
                .title(title));

        // Nếu điểm nằm ngoài khung khuôn viên, mở rộng camera về tâm trường nhưng vẫn giữ marker
        LatLng cameraTarget = CAMPUS_BOUNDS.contains(target) ? target : CAMPUS_CENTER;
        map.setCameraPosition(new CameraPosition.Builder()
                .target(cameraTarget)
                .zoom(DEFAULT_ZOOM)
                .build());
    }

    public static void drawCampusPerimeter(@NonNull MapLibreMap map) {
        List<LatLng> perimeter = Arrays.asList(
                new LatLng(10.1668d, 105.9242d),
                new LatLng(10.1668d, 105.9310d),
                new LatLng(10.1724d, 105.9310d),
                new LatLng(10.1724d, 105.9242d),
                new LatLng(10.1668d, 105.9242d)
        );
        map.addPolyline(new PolylineOptions()
                .addAll(perimeter)
                .color(0xFFE86A24)
                .width(2.5f));
    }

    public static boolean isValidCoordinate(@Nullable Double lat, @Nullable Double lng) {
        return lat != null && lng != null
                && Double.isFinite(lat) && Double.isFinite(lng)
                && lat >= -90d && lat <= 90d
                && lng >= -180d && lng <= 180d
                && !(Math.abs(lat) < 0.0001d && Math.abs(lng) < 0.0001d);
    }

    /** Cho phép kéo bản đồ mượt bên trong ScrollView mà không bị ScrollView cướp sự kiện chạm. */
    @SuppressLint("ClickableViewAccessibility")
    public static void enableScrollViewTouchSupport(@NonNull MapView mapView) {
        mapView.setOnTouchListener((v, event) -> {
            ViewParent parent = v.getParent();
            if (parent != null) {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                    parent.requestDisallowInterceptTouchEvent(true);
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    parent.requestDisallowInterceptTouchEvent(false);
                }
            }
            return false;
        });
    }

    /**
     * Tạo biểu tượng cảnh báo vi phạm nổi bật (khung đỏ/cam kèm biểu tượng cảnh báo + biển số)
     * để hiển thị trực tiếp trên bản đồ khuôn viên Trường Đại học Cửu Long.
     */
    @NonNull
    public static Icon createViolationAlertIcon(@NonNull Context context,
                                                @Nullable String plate,
                                                boolean isResolved) {
        float d = context.getResources().getDisplayMetrics().density;
        String label = (plate != null && !plate.trim().isEmpty()) ? plate.trim() : "VI PHẠM";
        if (label.length() > 12) label = label.substring(0, 12);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(11.5f * d);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        float textWidth = textPaint.measureText(label);
        float iconCircleSize = 18f * d;
        float padH = 8f * d;
        float gap = 5f * d;
        float pillW = padH + iconCircleSize + gap + textWidth + padH;
        float pillH = 28f * d;
        float pointerH = 7f * d;

        int width = Math.max(1, Math.round(pillW));
        int height = Math.max(1, Math.round(pillH + pointerH));

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        int bgColor = isResolved ? Color.parseColor("#16A34A") : Color.parseColor("#DC2626");

        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(bgColor);
        fillPaint.setStyle(Paint.Style.FILL);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(Color.WHITE);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(1.8f * d);

        RectF rect = new RectF(1f * d, 1f * d, width - 1f * d, pillH - 1f * d);
        float radius = (pillH - 2f * d) / 2f;
        canvas.drawRoundRect(rect, radius, radius, fillPaint);
        canvas.drawRoundRect(rect, radius, radius, strokePaint);

        // Mũi nhọn chỉ xuống đúng tọa độ GPS
        Path pointer = new Path();
        float cx = width / 2f;
        pointer.moveTo(cx - 5f * d, pillH - 2f * d);
        pointer.lineTo(cx, height - 1f * d);
        pointer.lineTo(cx + 5f * d, pillH - 2f * d);
        pointer.close();
        canvas.drawPath(pointer, fillPaint);

        // Vòng tròn trắng + dấu chấm than cảnh báo bên trái
        float circleCx = padH + iconCircleSize / 2f;
        float circleCy = pillH / 2f;
        Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgePaint.setColor(Color.WHITE);
        canvas.drawCircle(circleCx, circleCy, iconCircleSize / 2f, badgePaint);

        Paint bangPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bangPaint.setColor(bgColor);
        bangPaint.setTextSize(12f * d);
        bangPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        bangPaint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics bangFm = bangPaint.getFontMetrics();
        float bangY = circleCy - (bangFm.ascent + bangFm.descent) / 2f;
        canvas.drawText("!", circleCx, bangY, bangPaint);

        // Biển số xe
        Paint.FontMetrics textFm = textPaint.getFontMetrics();
        float textY = pillH / 2f - (textFm.ascent + textFm.descent) / 2f;
        canvas.drawText(label, padH + iconCircleSize + gap, textY, textPaint);

        return IconFactory.getInstance(context).fromBitmap(bitmap);
    }

    /** Mở ứng dụng bản đồ ngoài (Google Maps) tại tọa độ hiện trường hoặc tâm ĐH Cửu Long. */
    public static void openInExternalMaps(@NonNull Context context,
                                          @Nullable Double lat,
                                          @Nullable Double lng,
                                          @Nullable String label) {
        double targetLat = isValidCoordinate(lat, lng) ? lat : CAMPUS_CENTER_LAT;
        double targetLng = isValidCoordinate(lat, lng) ? lng : CAMPUS_CENTER_LNG;
        String safeLabel = (label != null && !label.isEmpty()) ? label : CAMPUS_NAME;
        String uriStr = String.format(Locale.US, "geo:%.6f,%.6f?q=%.6f,%.6f(%s)",
                targetLat, targetLng, targetLat, targetLng, Uri.encode(safeLabel));
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Không tìm thấy ứng dụng bản đồ trên thiết bị", Toast.LENGTH_SHORT).show();
        }
    }
}
