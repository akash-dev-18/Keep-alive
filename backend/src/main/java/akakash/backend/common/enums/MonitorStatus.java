package akakash.backend.common.enums;

public enum MonitorStatus {
    UP("up"),
    DOWN("down"),
    DEGRADED("degraded"),
    UNKNOWN("unknown"),
    PAUSED("paused");

    private final String dbValue;

    MonitorStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static MonitorStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        for (MonitorStatus status : values()) {
            if (status.dbValue.equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid monitor status: " + value);
    }
}
