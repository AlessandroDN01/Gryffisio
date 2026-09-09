package org.generation.italy.controllers;

import org.generation.italy.model.dto.DashboardDto;
import org.generation.italy.services.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping ("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    @GetMapping
    public DashboardDto getDashboard(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Integer operatorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer activityId,
            @RequestParam(required = false) Integer domainId) {
        return dashboardService.getDashboard(projectId, operatorId, fromDate, toDate, activityId, domainId);
    }
}

