package akakash.backend.check;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface MonitorCheckRepository extends JpaRepository<MonitorCheck, UUID> {

    List<MonitorCheck> findByMonitorIdOrderByCheckedAtDesc(UUID monitorId);

    List<MonitorCheck> findByMonitorIdOrderByCheckedAtDesc(UUID monitorId, Pageable pageable);

    @Query("SELECT mc FROM MonitorCheck mc WHERE mc.monitorId IN :monitorIds ORDER BY mc.checkedAt DESC")
    List<MonitorCheck> findByMonitorIdInOrderByCheckedAtDesc(@Param("monitorIds") List<UUID> monitorIds, Pageable pageable);

    @Query("SELECT mc FROM MonitorCheck mc WHERE mc.monitorId IN :monitorIds AND mc.status = 'down' ORDER BY mc.checkedAt DESC")
    List<MonitorCheck> findFailuresByMonitorIdIn(@Param("monitorIds") List<UUID> monitorIds, Pageable pageable);

    @Query("SELECT mc FROM MonitorCheck mc WHERE mc.monitorId = :monitorId AND mc.checkedAt >= :from AND mc.checkedAt <= :to ORDER BY mc.checkedAt DESC")
    List<MonitorCheck> findByMonitorIdAndCheckedAtBetween(
            @Param("monitorId") UUID monitorId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Modifying
    @Query("DELETE FROM MonitorCheck mc WHERE mc.checkedAt < :cutoff")
    void deleteByCheckedAtBefore(@Param("cutoff") OffsetDateTime cutoff);
}