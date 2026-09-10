package org.generation.italy.services;

import org.generation.italy.model.dto.PagedResponse;
import org.generation.italy.model.dto.RegistrationDto;
import org.generation.italy.model.dto.RegistrationRequest;
import org.generation.italy.model.entities.*;
import org.generation.italy.model.exceptions.BadRequestException;
import org.generation.italy.model.exceptions.NotFoundException;
import org.generation.italy.model.repositories.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RegistrationService {
    private static final int MAX_PAGE_SIZE = 100;

    private final RegistrationRepository registrationRepository;
    private final ProjectRepository projectRepository;
    private final DomainRepository domainRepository;
    private final SessionRepository sessionRepository;
    private final DoctorRepository doctorRepository;
    private final OperatorRepository operatorRepository;
    private final SubjectRepository subjectRepository;
    private final ActivityRepository activityRepository;
    private final AuditLogService auditLogService;

    public RegistrationService(
            RegistrationRepository registrationRepository,
            ProjectRepository projectRepository,
            DomainRepository domainRepository,
            SessionRepository sessionRepository,
            DoctorRepository doctorRepository,
            OperatorRepository operatorRepository,
            SubjectRepository subjectRepository,
            ActivityRepository activityRepository, AuditLogService auditLogService
    ) {
        this.registrationRepository = registrationRepository;
        this.projectRepository = projectRepository;
        this.domainRepository = domainRepository;
        this.sessionRepository = sessionRepository;
        this.doctorRepository = doctorRepository;
        this.operatorRepository = operatorRepository;
        this.subjectRepository = subjectRepository;
        this.activityRepository = activityRepository;
        this.auditLogService = auditLogService;
    }

    private RegistrationDto toDto(Registration registration) {
        Doctor doctor = registration.getDoctor();
        return new RegistrationDto(
                registration.getId(),
                registration.getProject().getId(),
                registration.getProject().getName(),
                registration.getDomain().getId(),
                registration.getDomain().getName(),
                registration.getSession().getId(),
                registration.getSession().getSession(),
                doctor != null ? doctor.getId() : null,
                doctor != null ? doctor.getFirstName() + " " + doctor.getLastName() : null,
                registration.getActivityDate(),
                registration.getDurationMinutes(),
                registration.getOperators().stream().map(Operator::getId).toList(),
                registration.getSubjects().stream().map(Subject::getId).toList(),
                registration.getActivities().stream().map(Activity::getId).toList(),
                registration.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<RegistrationDto> findAll(
            Integer projectId,
            LocalDate fromDate,
            LocalDate toDate,
            Integer operatorId,
            Integer activityId,
            Integer domainId,
            String search,
            int page,
            int size
    ) {
        validateFilters(fromDate, toDate, page, size);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("activityDate"),
                Sort.Order.desc("id")
        ));
        Page<Registration> filteredPage = registrationRepository.findFiltered(
                projectId, fromDate, toDate, operatorId, activityId, domainId, search, pageable);

        List<Long> pageIds = filteredPage.getContent().stream()
                .map(Registration::getId)
                .toList();

        List<Registration> fullyLoaded = registrationRepository.findByIdIn(pageIds);

        Map<Long, Registration> byId = fullyLoaded.stream()
                .collect(Collectors.toMap(Registration::getId, r -> r));

        List<RegistrationDto> orderedDtos = filteredPage.getContent().stream()
                .map(r -> toDto(byId.get(r.getId())))
                .toList();

        return new PagedResponse<>(
                orderedDtos,
                filteredPage.getNumber(),
                filteredPage.getSize(),
                filteredPage.getTotalElements(),
                filteredPage.getTotalPages(),
                filteredPage.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public RegistrationDto findById(Long id) throws NotFoundException {
        Registration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Registration_not_found", "Registration not found: " + id));
        return toDto(registration);
    }

    @Transactional
    public RegistrationDto createRegistration(RegistrationRequest request) throws NotFoundException {
        Registration registration = new Registration();
        applyRequest(registration, request);
        registration.setCreatedAt(LocalDateTime.now());
        Registration saved = registrationRepository.save(registration);

        auditLogService.log(new HashSet<>(saved.getOperators()), "CREATE", "Registration", saved.getId(),
                "Registration created via public form");

        return toDto(saved);
    }

    @Transactional
    public RegistrationDto updateRegistration(Long id, RegistrationRequest request, Integer adminId) throws NotFoundException {
        Registration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Registration_not_found", "Registration not found: " + id));
        applyRequest(registration, request);
        registration.setUpdatedAt(LocalDateTime.now());
        Registration saved = registrationRepository.save(registration);

        Operator admin = operatorRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Operator_not_found", "Operator not found: " + adminId));
        auditLogService.log(Set.of(admin), "UPDATE", "Registration", saved.getId(), "Registration updated by admin");

        return toDto(saved);
    }

    private void applyRequest(Registration registration, RegistrationRequest request) throws NotFoundException {
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new NotFoundException("Project_not_found", "Project not found: " + request.projectId()));

        Domain domain = domainRepository.findById(request.domainId())
                .orElseThrow(() -> new NotFoundException("Domain_not_found", "Domain not found: " + request.domainId()));

        Session session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new NotFoundException("Session_not_found", "Session not found: " + request.sessionId()));

        Doctor doctor = null;
        if (request.doctorId() != null) {
            doctor = doctorRepository.findById(request.doctorId())
                    .orElseThrow(() -> new NotFoundException("Doctor_not_found", "Doctor not found: " + request.doctorId()));
        }

        Set<Operator> operators = new HashSet<>(operatorRepository.findAllById(request.operatorIds()));
        if (operators.size() != Set.copyOf(request.operatorIds()).size()) {
            throw new NotFoundException("Operator_not_found", "One or more operator ids do not exist");
        }

        Set<Subject> subjects = new HashSet<>(subjectRepository.findAllById(request.subjectIds()));
        if (subjects.size() != Set.copyOf(request.subjectIds()).size()) {
            throw new NotFoundException("Subject_not_found", "One or more subject ids do not exist");
        }

        Set<Activity> activities = new HashSet<>(activityRepository.findAllById(request.activityIds()));
        if (activities.size() != Set.copyOf(request.activityIds()).size()) {
            throw new NotFoundException("Activity_not_found", "One or more activity ids do not exist");
        }

        registration.setProject(project);
        registration.setDomain(domain);
        registration.setSession(session);
        registration.setDoctor(doctor);
        registration.setActivityDate(request.activityDate());
        registration.setDurationMinutes(request.durationMinutes());
        registration.setOperators(operators);
        registration.setSubjects(subjects);
        registration.setActivities(activities);
    }

    @Transactional
    public void deleteRegistration(Long id, Integer adminId) throws NotFoundException {
        if (!registrationRepository.existsById(id)) {
            throw new NotFoundException("Registration_not_found", "Registration not found: " + id);
        }
        Operator admin = operatorRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Operator_not_found", "Operator not found: " + adminId));
        auditLogService.log(Set.of(admin), "DELETE", "Registration", id, "Registration deleted by admin");
        registrationRepository.deleteById(id);
    }

    private void validateFilters(LocalDate fromDate, LocalDate toDate, int page, int size) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("INVALID_DATE_RANGE", "fromDate cannot be after toDate");
        }
        if (page < 0) {
            throw new BadRequestException("Invalid_pagination", "page must be greater than or equal to 0");
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException(
                    "Invalid_pagination",
                    "size must be greater than 0 and no greater than " + MAX_PAGE_SIZE
            );
        }
    }
}