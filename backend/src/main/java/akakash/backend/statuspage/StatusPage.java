package akakash.backend.statuspage;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "status_pages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusPage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(name = "monitor_ids", nullable = false)
    private UUID[] monitorIds;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Column(name = "custom_domain")
    private String customDomain;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 2048)
    private String logoUrl;

    @Column(name = "primary_color")
    private String primaryColor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (isPublic == null) isPublic = true;
        if (monitorIds == null) monitorIds = new UUID[0];
        if (primaryColor == null) primaryColor = "#06b6d4";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}