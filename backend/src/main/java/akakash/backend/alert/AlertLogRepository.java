package akakash.backend.alert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertLogRepository extends JpaRepository<AlertLog, UUID> {

    List<AlertLog> findByMonitorIdOrderBySentAtDesc(UUID monitorId);

    List<AlertLog> findByMonitorIdInOrderBySentAtDesc(List<UUID> monitorIds);
}