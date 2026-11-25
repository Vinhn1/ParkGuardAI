package com.example.parkguardai.data.model;

/**
 * Mục dòng thời gian trong lịch sử các lần quét của phương tiện trong ngày.
 */
public final class ScanTimelineItem {
    public final String time;
    public final String action;      // "Vào", "Ra", "Vi phạm", "Quét lại"
    public final String gate;        // "Cổng chính", "Cổng phụ", "Bãi A"...
    public final String status;      // "Hợp lệ", "Đã gộp", "Cần xác nhận", "Vi phạm"
    public final boolean isCurrent;  // Đánh dấu lần quét hiện tại

    public ScanTimelineItem(String time, String action, String gate,
                            String status, boolean isCurrent) {
        this.time = time;
        this.action = action;
        this.gate = gate;
        this.status = status;
        this.isCurrent = isCurrent;
    }
}
