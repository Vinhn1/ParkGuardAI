package com.example.parkguardai.data.model;

import java.io.Serializable;

/**
 * Cấu hình quy tắc quét lặp và an ninh bãi xe lưu tại Firestore systemConfig/scanRules.
 */
public class ScanRuleConfig implements Serializable {
    private int duplicateIntervalSeconds; // 30, 60, 120
    private boolean enableDuplicateAlert;
    private boolean autoFlagOvernight;
    private int maxDailyScans;

    public ScanRuleConfig() {
        this.duplicateIntervalSeconds = 60;
        this.enableDuplicateAlert = true;
        this.autoFlagOvernight = true;
        this.maxDailyScans = 10;
    }

    public ScanRuleConfig(int duplicateIntervalSeconds, boolean enableDuplicateAlert,
                          boolean autoFlagOvernight, int maxDailyScans) {
        this.duplicateIntervalSeconds = duplicateIntervalSeconds;
        this.enableDuplicateAlert = enableDuplicateAlert;
        this.autoFlagOvernight = autoFlagOvernight;
        this.maxDailyScans = maxDailyScans;
    }

    public int getDuplicateIntervalSeconds() {
        return duplicateIntervalSeconds;
    }

    public void setDuplicateIntervalSeconds(int duplicateIntervalSeconds) {
        this.duplicateIntervalSeconds = duplicateIntervalSeconds;
    }

    public boolean isEnableDuplicateAlert() {
        return enableDuplicateAlert;
    }

    public void setEnableDuplicateAlert(boolean enableDuplicateAlert) {
        this.enableDuplicateAlert = enableDuplicateAlert;
    }

    public boolean isAutoFlagOvernight() {
        return autoFlagOvernight;
    }

    public void setAutoFlagOvernight(boolean autoFlagOvernight) {
        this.autoFlagOvernight = autoFlagOvernight;
    }

    public int getMaxDailyScans() {
        return maxDailyScans;
    }

    public void setMaxDailyScans(int maxDailyScans) {
        this.maxDailyScans = maxDailyScans;
    }
}
