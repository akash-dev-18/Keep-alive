package akakash.backend.common.dto;

import java.util.List;

public record DashboardActivityResponse(
    List<MonitorCheckResponse> recentChecks,
    List<IncidentResponse> recentIncidents,
    List<MonitorCheckResponse> recentFailures
) {}
