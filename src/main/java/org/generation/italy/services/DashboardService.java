package org.generation.italy.services;


import org.generation.italy.model.dto.DashboardDto;
import org.generation.italy.model.dto.ActivityMatricsDto;
import org.generation.italy.model.dto.OperatorMatricsDto;
import org.generation.italy.model.exceptions.BadRequestException;
import org.generation.italy.model.repositories.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {
    private final RegistrationRepository registrationRepository;

    public DashboardService(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDto getDashboard(Integer projectId, Integer operatorId, LocalDate fromDate, LocalDate toDate, Integer activityId, Integer domainId) {
        validateDateRange(fromDate, toDate);

        long totalRegistrations = registrationRepository.countFiltered(projectId, fromDate, toDate, operatorId, activityId, domainId);
        long totalMinutes = registrationRepository.sumDurationMinutesFiltered(projectId, fromDate, toDate, operatorId, activityId, domainId);
        Long projectCount = projectId != null ? totalRegistrations : null;

        List<ActivityMatricsDto> activityMatrics = registrationRepository.findActivityMatricsFiltered(projectId, fromDate, toDate, operatorId, activityId, domainId).stream()
                .map(row -> new ActivityMatricsDto(
                        row.getActivityId(),
                        row.getActivityName(),
                        row.getRegistrationCount(),
                        minutesToHours(row.getTotalMinutes())
                ))
                .toList();
        List<OperatorMatricsDto> operatorMatrics = registrationRepository.findOperationMatricsFiltered(projectId, fromDate, toDate, operatorId, activityId, domainId).stream()
                .map(row -> new OperatorMatricsDto(
                        row.getOperatorId(),
                        row.getOperatorName(),
                        row.getRegistrationCount(),
                        minutesToHours(row.getTotalMinutes())
                ))
                .toList();

        return new DashboardDto(totalRegistrations, projectCount, operatorMatrics, activityMatrics, minutesToHours(totalMinutes));
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("INVALID_DATE_RANGE", "fromDate cannot be after toDate");
        }
    }

    public double minutesToHours (long minutes){
        return Math.round((minutes/60.0)*100.0)/100.0;
    }
}
