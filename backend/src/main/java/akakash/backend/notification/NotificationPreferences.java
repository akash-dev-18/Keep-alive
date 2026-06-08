package akakash.backend.notification;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferences {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email_on_down", nullable = false)
    private boolean emailOnDown;

    @Column(name = "email_on_up", nullable = false)
    private boolean emailOnUp;

    @Column(name = "email_on_ssl_expiry", nullable = false)
    private boolean emailOnSslExpiry;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onSave() {
        updatedAt = OffsetDateTime.now();

        // optional safety (only needed if builder is used)
        emailOnDown = true;
        emailOnUp = true;
        emailOnSslExpiry = true;
    }
}