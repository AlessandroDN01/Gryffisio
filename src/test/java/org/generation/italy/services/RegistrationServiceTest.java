package org.generation.italy.services;

import org.generation.italy.model.dto.PagedResponse;
import org.generation.italy.model.dto.RegistrationDto;
import org.generation.italy.model.exceptions.BadRequestException;
import org.generation.italy.model.repositories.ActivityRepository;
import org.generation.italy.model.repositories.DomainRepository;
import org.generation.italy.model.repositories.DoctorRepository;
import org.generation.italy.model.repositories.OperatorRepository;
import org.generation.italy.model.repositories.ProjectRepository;
import org.generation.italy.model.repositories.RegistrationRepository;
import org.generation.italy.model.repositories.SessionRepository;
import org.generation.italy.model.repositories.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private DomainRepository domainRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private OperatorRepository operatorRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private AuditLogService auditLogService;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(
                registrationRepository,
                projectRepository,
                domainRepository,
                sessionRepository,
                doctorRepository,
                operatorRepository,
                subjectRepository,
                activityRepository,
                auditLogService
        );
    }

    @Test
    void findAllDelegatesFiltersAndDeterministicPageableToRepository() {
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 1, 31);
        when(registrationRepository.findFiltered(
                eq(10), eq(fromDate), eq(toDate), eq(20), eq(30), eq(40), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 8));

        PagedResponse<RegistrationDto> response = registrationService.findAll(
                10, fromDate, toDate, 20, 30, 40, 1, 5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(registrationRepository).findFiltered(
                eq(10), eq(fromDate), eq(toDate), eq(20), eq(30), eq(40), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();

        assertEquals(1, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("activityDate").getDirection());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("id").getDirection());
        assertEquals(1, response.page());
        assertEquals(5, response.size());
        assertEquals(8, response.total());
        assertEquals(2, response.totalPages());
        assertFalse(response.hasNext());
    }

    @Test
    void findAllRejectsInvalidDateRangeBeforeQuerying() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> registrationService.findAll(
                        null,
                        LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 1, 31),
                        null,
                        null,
                        null,
                        0,
                        5
                )
        );

        assertEquals("INVALID_DATE_RANGE", exception.getErrorCode());
        verifyNoInteractions(registrationRepository);
    }

    @Test
    void findAllRejectsOutOfRangePagingBeforeQuerying() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> registrationService.findAll(null, null, null, null, null, null, -1, 101)
        );

        assertEquals("Invalid_pagination", exception.getErrorCode());
        verifyNoInteractions(registrationRepository);
    }
}
