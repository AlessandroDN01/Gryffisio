package org.generation.italy.controllers;

import jakarta.validation.Valid;
import org.generation.italy.model.dto.PagedResponse;
import org.generation.italy.model.dto.RegistrationDto;
import org.generation.italy.model.dto.RegistrationRequest;
import org.generation.italy.model.exceptions.BadRequestException;
import org.generation.italy.model.exceptions.NotFoundException;
import org.generation.italy.services.RegistrationExportService;
import org.generation.italy.services.RegistrationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {
    private final RegistrationService registrationService;
    private final RegistrationExportService registrationExportService;

    public RegistrationController(RegistrationService registrationService, RegistrationExportService registrationExportService) {
        this.registrationService = registrationService;
        this.registrationExportService = registrationExportService;
    }



    @GetMapping("/{id}")
    public RegistrationDto getById(@PathVariable Long id) throws NotFoundException {
        return registrationService.findById(id);
    }

    @GetMapping
    public PagedResponse<RegistrationDto> getAll(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer operatorId,
            @RequestParam(required = false) Integer activityId,
            @RequestParam(required = false) Integer domainId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return registrationService.findAll(
                projectId, fromDate, toDate, operatorId, activityId, domainId, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationDto create(@Valid @RequestBody RegistrationRequest request) throws NotFoundException {
        return registrationService.createRegistration(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RegistrationDto update(@PathVariable Long id, @Valid @RequestBody RegistrationRequest request, @AuthenticationPrincipal Jwt jwt) throws NotFoundException {
        Integer adminId = extractOperatorId(jwt);
        return registrationService.updateRegistration(id, request, adminId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) throws NotFoundException {
        Integer adminId = extractOperatorId(jwt);
        registrationService.deleteRegistration(id, adminId);
    }

    private Integer extractOperatorId(Jwt jwt) {
        Number uid = jwt.getClaim("uid");
        if (uid == null) {
            throw new BadRequestException("Invalid_token", "Token is missing the 'uid' claim");
        }
        return uid.intValue();
    }
}
