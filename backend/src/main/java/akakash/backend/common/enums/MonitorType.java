package akakash.backend.common.enums;

import java.util.Set;

public enum MonitorType {
    HTTP("http"),
    SSL("ssl"),
    HEARTBEAT("cron"),
    KEYWORD("keyword");

    private final String dbValue;

    MonitorType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static MonitorType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Monitor type is required");
        }
        String normalized = value.trim().toLowerCase();
        if ("heartbeat".equals(normalized) || "cron".equals(normalized)) {
            return HEARTBEAT;
        }
        for (MonitorType type : values()) {
            if (type.dbValue.equals(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid monitor type: " + value);
    }

    public static final Set<Integer> ALLOWED_INTERVALS = Set.of(1, 2, 3, 5, 10, 15, 30, 60);
    public static final Set<Integer> ALLOWED_TIMEOUTS = Set.of(5, 10, 20, 30, 60);
    public static final Set<Integer> ALLOWED_ALERT_AFTER = Set.of(0, 1, 2, 3, 5);
}
