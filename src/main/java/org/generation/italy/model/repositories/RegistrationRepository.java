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
    """)
    Page<Registration> findFiltered(
           @Param("projectId") Integer projectId,
           @Param("fromDate") LocalDate fromDate,
           @Param("toDate") LocalDate toDate,
           @Param("operatorId") Integer operatorId,
           @Param("activityId") Integer activityId,
           @Param("domainId") Integer domainId,
           Pageable pageable
    );

    @Query("""
        SELECT o.id as operatorId, COUNT(r.id) as registrationCount, COALESCE(SUM(r.durationMinutes),0) as totalMinutes, CONCAT(o.firstName,' ',o.lastName) as operatorName

        FROM Registration r JOIN r.operators o
        WHERE (:projectId IS NULL OR r.project.id = :projectId)
          AND r.activityDate >= COALESCE(:fromDate, r.activityDate)
          AND r.activityDate <= COALESCE(:toDate, r.activityDate)
        GROUP BY o.id, o.firstName, o.lastName
        ORDER BY o.lastName, o.firstName
    """)
    List<OperatorMetricsProjection> findOperationMatricsFiltered(
           @Param("projectId") Integer projectId,
           @Param("fromDate") LocalDate fromDate,
           @Param("toDate") LocalDate toDate
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
          AND r.activityDate >= COALESCE(:fromDate, r.activityDate)
          AND r.activityDate <= COALESCE(:toDate, r.activityDate)
        GROUP BY a.id, a.name
        ORDER BY a.name
    """)
    List<ActivityMetricsProjection> findActivityMatricsFiltered(
           @Param("projectId") Integer projectId,
           @Param("fromDate") LocalDate fromDate,
           @Param("toDate") LocalDate toDate
    );

    @Query("""
        SELECT a.id as activityId, a.name as activityName, COUNT(r.id) as registrationCount, COALESCE(SUM(r.durationMinutes),0) as totalMinutes

        FROM Registration r JOIN r.activities a
        GROUP BY a.id, a.name
        ORDER BY a.name
    """)
    List<ActivityMetricsProjection> findActivityMatrics();

    long countByProject_Id (Integer projectId);

    long countByActivityDateBetween(LocalDate fromDate, LocalDate toDate);

    long countByProject_IdAndActivityDateBetween(Integer projectId, LocalDate fromDate, LocalDate toDate);

    long countByProject_IdAndActivityDateGreaterThanEqual(Integer projectId, LocalDate fromDate);

    long countByProject_IdAndActivityDateLessThanEqual(Integer projectId, LocalDate toDate);

    long countByActivityDateGreaterThanEqual(LocalDate fromDate);

    long countByActivityDateLessThanEqual(LocalDate toDate);
}