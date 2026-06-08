package akakash.backend.statuspage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StatusPageRepository extends JpaRepository<StatusPage, UUID> {

    Optional<StatusPage> findBySlug(String slug);

    long countByUserId(UUID userId);

    List<StatusPage> findByUserId(UUID userId);
}