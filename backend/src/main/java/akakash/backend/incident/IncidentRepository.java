package akakash.backend.incident;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    })
    @Query("SELECT i FROM Incident i WHERE i.monitorId = :monitorId AND i.status = 'open'")
    Optional<Incident> findOpenByMonitorId(@Param("monitorId") UUID monitorId);

    List<Incident> findByMonitorIdOrderByStartedAtDesc(UUID monitorId);

    @Query("SELECT i FROM Incident i WHERE i.status = 'open'")
    List<Incident> findAllOpenIncidents();

    @Query("SELECT i FROM Incident i WHERE i.monitorId IN :monitorIds ORDER BY i.startedAt DESC")
    List<Incident> findByMonitorIdInOrderByStartedAtDesc(
            @Param("monitorIds") List<UUID> monitorIds,
            Pageable pageable
    );
}