package akakash.backend.alert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SslAlertSentRepository extends JpaRepository<SslAlertSent, UUID> {

    Optional<SslAlertSent> findByMonitorIdAndThresholdDays(UUID monitorId, int thresholdDays);

    boolean existsByMonitorIdAndThresholdDays(UUID monitorId, int thresholdDays);
}
