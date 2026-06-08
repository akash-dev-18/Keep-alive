package akakash.backend.alert;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ssl_alert_sent", uniqueConstraints = @UniqueConstraint(columnNames = {"monitor_id", "threshold_days"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SslAlertSent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "monitor_id", nullable = false)
    private UUID monitorId;

    @Column(name = "threshold_days", nullable = false)
    private Integer thresholdDays;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) sentAt = OffsetDateTime.now();
    }
}
