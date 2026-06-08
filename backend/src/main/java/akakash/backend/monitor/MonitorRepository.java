package akakash.backend.monitor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonitorRepository extends JpaRepository<Monitor, UUID> {


    List<Monitor> findByUserId(UUID userId);

    long countByUserId(UUID userId);

    Optional<Monitor> findByPingKey(String pingKey);

    @Query("SELECT m FROM Monitor m WHERE m.isActive = true AND m.nextCheckAt <= :now")
    List<Monitor> findDueForCheck(@Param("now") OffsetDateTime now, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Monitor m SET m.status = :status WHERE m.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") String status);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Monitor m SET m.nextCheckAt = :nextCheckAt WHERE m.id = :id")
    void updateNextCheckAt(@Param("id") UUID id, @Param("nextCheckAt") OffsetDateTime nextCheckAt);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Monitor m SET m.lastCheckedAt = :lastCheckedAt WHERE m.id = :id")
    void updateLastCheckedAt(@Param("id") UUID id, @Param("lastCheckedAt") OffsetDateTime lastCheckedAt);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Monitor m SET m.sslDaysRemaining = :days, m.sslExpiresAt = :expiresAt, m.sslIssuer = :issuer WHERE m.id = :id")
    void updateSslDetails(@Param("id") UUID id, @Param("days") Integer days, @Param("expiresAt") OffsetDateTime expiresAt, @Param("issuer") String issuer);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Monitor m SET m.consecutiveFailures = :failures WHERE m.id = :id")
    void updateConsecutiveFailures(@Param("id") UUID id, @Param("failures") int failures);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Monitor m SET m.lastResponseTimeMs = :ms WHERE m.id = :id")
    void updateLastResponseTimeMs(@Param("id") UUID id, @Param("ms") Integer ms);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Monitor m SET m.expectedNextHeartbeatAt = :at WHERE m.id = :id")
    void updateExpectedNextHeartbeatAt(@Param("id") UUID id, @Param("at") OffsetDateTime at);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Monitor m SET m.lastPingedAt = :lastPingedAt, m.expectedNextHeartbeatAt = :expectedNext WHERE m.id = :id")
    void updateHeartbeatPing(@Param("id") UUID id, @Param("lastPingedAt") OffsetDateTime lastPingedAt, @Param("expectedNext") OffsetDateTime expectedNext);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Monitor m SET m.isActive = :isActive WHERE m.id = :id")
    void updateIsActive(@Param("id") UUID id, @Param("isActive") Boolean isActive);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Monitor m SET m.lastPingedAt = :lastPingedAt WHERE m.id = :id")
    void updateLastPingedAt(@Param("id") UUID id, @Param("lastPingedAt") OffsetDateTime lastPingedAt);

    List<Monitor> findByTypeAndIsActiveTrue(String type);
}