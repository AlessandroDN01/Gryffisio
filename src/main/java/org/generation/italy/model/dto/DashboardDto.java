package org.generation.italy.model.dto;

import java.util.List;

public record DashboardDto(
        long totalRegistrations,
        Long selectProjectRegistration,
        List<OperatorMatricsDto> operatorMatrics,
        List<ActivityMatricsDto> activityMatrics,
        double totalHours
) {}
