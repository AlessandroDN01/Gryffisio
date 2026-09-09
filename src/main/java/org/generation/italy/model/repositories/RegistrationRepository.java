package org.generation.italy.model.repositories;

import org.generation.italy.model.entities.Registration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    @EntityGraph(attributePaths = {
            "domain",
            "project",
            "session",
            "doctor",
            "subjects",
            "subjects.subjectType",
            "operators",
            "activities"
    })
    List<Registration> findAllByOrderByIdAsc();
    @Query("""
        SELECT r
        FROM Registration r
        WHERE (:projectId IS NULL OR r.project.id = :projectId)
          AND r.activityDate >= COALESCE(:fromDate, r.activityDate)
          AND r.activityDate <= COALESCE(:toDate, r.activityDate)
          AND (:domainId IS NULL OR r.domain.id = :domainId)
          AND (:operatorId IS NULL OR EXISTS (
              SELECT op
              FROM r.operators op
              WHERE op.id = :operatorId
           ))
          AND (:activityId IS NULL OR EXISTS (
              SELECT a
              FROM r.activities a
              WHERE a.id = :activityId
           ))
               AND (
                    :search IS NULL
                    OR LOWER(r.project.name) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
                    OR LOWER(r.domain.name) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
                    OR LOWER(r.session.session) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
                    OR LOWER(CONCAT(r.doctor.firstName, ' ', r.doctor.lastName))
                       LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
                    OR EXISTS (
                        SELECT opSearch
                        FROM r.operators opSearch
                        WHERE LOWER(CONCAT(opSearch.firstName, ' ', opSearch.lastName))
                              LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
                    )
                    OR EXISTS (
                        SELECT subSearch
                        FROM r.subjects subSearch
                        WHERE CAST(subSearch.id AS string)
                              LIKE CONCAT('%', CAST(:search AS string), '%')
                    )
                )
    """)
    Page<Registration> findFiltered(
           @Param("projectId") Integer projectId,
           @Param("fromDate") LocalDate fromDate,
           @Param("toDate") LocalDate toDate,
           @Param("operatorId") Integer operatorId,
           @Param("activityId") Integer activityId,
           @Param("domainId") Integer domainId,
           @Param("search") String search,
           Pageable pageable
    );

    @Query("""
        SELECT o.id as operatorId, COUNT(r.id) as registrationCount, COALESCE(SUM(r.durationMinutes),0) as totalMinutes, CONCAT(o.firstName,' ',o.lastName) as operatorName

        FROM Registration r JOIN r.operators o
        WHERE (:projectId IS NULL OR r.project.id = :projectId)
          AND (:domainId IS NULL OR r.domain.id = :domainId)
          AND (:operatorId IS NULL OR o.id = :operatorId)
          AND (:activityId IS NULL OR EXISTS (
             SELECT a
             FROM r.activities a
             WHERE a.id = :activityId
          ))
          AND (:fromDate IS NULL OR r.activityDate >= :fromDate)
          AND (:toDate IS NULL OR r.activityDate <= :toDate)
        GROUP BY o.id, o.firstName, o.lastName
        ORDER BY o.lastName, o.firstName
    """)
    List<OperatorMetricsProjection> findOperationMatricsFiltered(
           @Param("projectId") Integer projectId,
           @Param("fromDate") LocalDate fromDate,
           @Param("toDate") LocalDate toDate,
           @Param("operatorId") Integer operatorId,
           @Param("activityId") Integer activityId,
           @Param("domainId") Integer domainId
    );

    @Query("""
        SELECT o.id as operatorId, COUNT(r.id) as registrationCount, COALESCE(SUM(r.durationMinutes),0) as totalMinutes, CONCAT(o.firstName,' ',o.lastName) as operatorName

        FROM Registration r JOIN r.operators o
        GROUP BY o.id, o.firstName, o.lastName
        ORDER BY o.lastName, o.firstName
    """)
    List<OperatorMetricsProjection> findOperationMatrics();

    @Query("""
        SELECT a.id as activityId, a.name as activityName, COUNT(r.id) as registrationCount, COALESCE(SUM(r.durationMinutes),0) as totalMinutes

        FROM Registration r JOIN r.activities a
        WHERE (:projectId IS NULL OR r.project.id = :projectId)
          AND (:domainId IS NULL OR r.domain.id = :domainId)
          AND (:operatorId IS NULL OR EXISTS (
             SELECT op
             FROM r.operators op
             WHERE op.id = :operatorId
          ))
          AND (:activityId IS NULL OR a.id = :activityId)
          AND (:fromDate IS NULL OR r.activityDate >= :fromDate)
          AND (:toDate IS NULL OR r.activityDate <= :toDate)
        GROUP BY a.id, a.name
        ORDER BY a.name
    """)
    List<ActivityMetricsProjection> findActivityMatricsFiltered(
           @Param("projectId") Integer projectId,
           @Param("fromDate") LocalDate fromDate,
           @Param("toDate") LocalDate toDate,
           @Param("operatorId") Integer operatorId,
           @Param("activityId") Integer activityId,
           @Param("domainId") Integer domainId
    );

    @Query("""
        SELECT a.id as activityId, a.name as activityName, COUNT(r.id) as registrationCount, COALESCE(SUM(r.durationMinutes),0) as totalMinutes

        FROM Registration r JOIN r.activities a
        GROUP BY a.id, a.name
        ORDER BY a.name
    """)
    List<ActivityMetricsProjection> findActivityMatrics();

    @Query("""
        SELECT COUNT(r)
        FROM Registration r
        WHERE (:projectId IS NULL OR r.project.id = :projectId)
          AND (:domainId IS NULL OR r.domain.id = :domainId)
          AND (:operatorId IS NULL OR EXISTS (
              SELECT op
              FROM r.operators op
              WHERE op.id = :operatorId
          ))
          AND (:activityId IS NULL OR EXISTS (
              SELECT a
              FROM r.activities a
              WHERE a.id = :activityId
          ))
          AND (:fromDate IS NULL OR r.activityDate >= :fromDate)
          AND (:toDate IS NULL OR r.activityDate <= :toDate)
    """)
    long countFiltered(
            @Param("projectId") Integer projectId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("operatorId") Integer operatorId,
            @Param("activityId") Integer activityId,
            @Param("domainId") Integer domainId
    );

    @Query("""
        SELECT COALESCE(SUM(r.durationMinutes), 0)
        FROM Registration r
        WHERE (:projectId IS NULL OR r.project.id = :projectId)
          AND (:domainId IS NULL OR r.domain.id = :domainId)
          AND (:operatorId IS NULL OR EXISTS (
              SELECT op
              FROM r.operators op
              WHERE op.id = :operatorId
          ))
          AND (:activityId IS NULL OR EXISTS (
              SELECT a
              FROM r.activities a
              WHERE a.id = :activityId
          ))
          AND (:fromDate IS NULL OR r.activityDate >= :fromDate)
          AND (:toDate IS NULL OR r.activityDate <= :toDate)
    """)
    long sumDurationMinutesFiltered(
            @Param("projectId") Integer projectId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("operatorId") Integer operatorId,
            @Param("activityId") Integer activityId,
            @Param("domainId") Integer domainId
    );

    long countByProject_Id (Integer projectId);

    long countByActivityDateBetween(LocalDate fromDate, LocalDate toDate);

    long countByProject_IdAndActivityDateBetween(Integer projectId, LocalDate fromDate, LocalDate toDate);

    long countByProject_IdAndActivityDateGreaterThanEqual(Integer projectId, LocalDate fromDate);

    long countByProject_IdAndActivityDateLessThanEqual(Integer projectId, LocalDate toDate);

    long countByActivityDateGreaterThanEqual(LocalDate fromDate);

    long countByActivityDateLessThanEqual(LocalDate toDate);
}