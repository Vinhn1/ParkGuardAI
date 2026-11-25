package com.example.parkguardai.ui.guard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.parkguardai.R;

/** Vùng chọn ảnh cắt có thể kéo và thay kích thước bằng hai góc. */
public final class CropSelectionView extends View {
    private final RectF selection = new RectF(0.22f, 0.35f, 0.78f, 0.65f);
    private final Paint shade = new Paint();
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int dragMode;
    private float lastX;
    private float lastY;

    public CropSelectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        shade.setColor(0x66000000);
        border.setColor(Color.WHITE);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(3f * getResources().getDisplayMetrics().density);
        setContentDescription(getResources().getString(R.string.guard_review_crop_accessibility));
    }

    public RectF getNormalizedSelection() { return new RectF(selection); }

    /** Đặt khung theo tọa độ chuẩn hóa 0..1 của view, ví dụ từ gợi ý AI; bảo vệ vẫn có thể kéo chỉnh. */
    public void setNormalizedSelection(RectF value) {
        float left = clamp(value.left, 0f, 0.9f);
        float top = clamp(value.top, 0f, 0.94f);
        float right = clamp(value.right, left + 0.1f, 1f);
        float bottom = clamp(value.bottom, top + 0.06f, 1f);
        selection.set(left, top, right, bottom);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float l = selection.left * getWidth();
        float t = selection.top * getHeight();
        float r = selection.right * getWidth();
        float b = selection.bottom * getHeight();
        canvas.drawRect(0, 0, getWidth(), t, shade);
        canvas.drawRect(0, b, getWidth(), getHeight(), shade);
        canvas.drawRect(0, t, l, b, shade);
        canvas.drawRect(r, t, getWidth(), b, shade);
        canvas.drawRoundRect(l, t, r, b, 8f, 8f, border);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (getWidth() == 0 || getHeight() == 0) return false;
        float x = event.getX() / getWidth();
        float y = event.getY() / getHeight();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                float edge = 0.09f;
                dragMode = Math.abs(x - selection.left) < edge && Math.abs(y - selection.top) < edge ? 1
                        : Math.abs(x - selection.right) < edge && Math.abs(y - selection.bottom) < edge ? 2
                        : selection.contains(x, y) ? 3 : 0;
                lastX = x;
                lastY = y;
                return dragMode != 0;
            case MotionEvent.ACTION_MOVE:
                float dx = x - lastX;
                float dy = y - lastY;
                if (dragMode == 1) {
                    selection.left = clamp(x, 0, selection.right - 0.1f);
                    selection.top = clamp(y, 0, selection.bottom - 0.06f);
                } else if (dragMode == 2) {
                    selection.right = clamp(x, selection.left + 0.1f, 1);
                    selection.bottom = clamp(y, selection.top + 0.06f, 1);
                } else if (dragMode == 3) {
                    dx = clamp(dx, -selection.left, 1 - selection.right);
                    dy = clamp(dy, -selection.top, 1 - selection.bottom);
                    selection.offset(dx, dy);
                }
                lastX = x;
                lastY = y;
                invalidate();
                return dragMode != 0;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragMode = 0;
                performClick();
                return true;
            default: return false;
        }
    }

    @Override public boolean performClick() { super.performClick(); return true; }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
