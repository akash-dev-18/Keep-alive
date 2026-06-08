package akakash.backend.dashboard;

public record DashboardSummary(
    long totalMonitors,
    long activeMonitors,
    long upMonitors,
    long downMonitors,
    long unknownMonitors,
    long sslWarnings,
    long openIncidents
) {}
