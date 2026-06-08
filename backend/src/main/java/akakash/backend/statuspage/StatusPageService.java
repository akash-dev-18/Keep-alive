package akakash.backend.statuspage;

import akakash.backend.common.exception.PlanLimitException;
import akakash.backend.common.exception.ResourceNotFoundException;
import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import akakash.backend.user.Plan;
import akakash.backend.user.User;
import akakash.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StatusPageService {

    private static final int FREE_STATUS_PAGE_LIMIT = 1;
    private static final int PRO_STATUS_PAGE_LIMIT = 10;

    private final StatusPageRepository statusPageRepository;
    private final UserRepository userRepository;
    private final MonitorRepository monitorRepository;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public StatusPage createStatusPage(UUID userId, String name, String slug, UUID[] monitorIds,
                                       boolean isPublic, String customDomain, String description,
                                       String logoUrl, String primaryColor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        int limit = user.getPlan() == Plan.PRO ? PRO_STATUS_PAGE_LIMIT : FREE_STATUS_PAGE_LIMIT;
        if (statusPageRepository.countByUserId(userId) >= limit) {
            throw new PlanLimitException("You've reached the " + user.getPlan().name().toLowerCase()
                    + " plan limit of " + limit + " status pages.");
        }

        validateMonitorOwnership(userId, monitorIds);

        StatusPage statusPage = StatusPage.builder()
                .userId(userId)
                .name(name)
                .slug(slug)
                .monitorIds(monitorIds)
                .isPublic(isPublic)
                .customDomain(customDomain)
                .description(description)
                .logoUrl(logoUrl)
                .primaryColor(primaryColor != null ? primaryColor : "#06b6d4")
                .build();

        return statusPageRepository.save(statusPage);
    }

    public StatusPage getPublicStatusPage(String slug) {
        return statusPageRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Status page not found: " + slug));
    }

    public List<StatusPage> getStatusPagesByUser(UUID userId) {
        return statusPageRepository.findByUserId(userId);
    }

    @Transactional
    public StatusPage updateStatusPage(UUID statusPageId, UUID userId, String name, String slug,
                                       UUID[] monitorIds, boolean isPublic, String customDomain,
                                       String description, String logoUrl, String primaryColor) {
        StatusPage statusPage = statusPageRepository.findById(statusPageId)
                .orElseThrow(() -> new ResourceNotFoundException("Status page not found: " + statusPageId));

        if (!statusPage.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Status page not found: " + statusPageId);
        }

        if (monitorIds != null) {
            validateMonitorOwnership(userId, monitorIds);
        }

        String oldSlug = statusPage.getSlug();
        if (name != null) statusPage.setName(name);
        if (slug != null) statusPage.setSlug(slug);
        if (monitorIds != null) statusPage.setMonitorIds(monitorIds);
        statusPage.setIsPublic(isPublic);
        if (customDomain != null) statusPage.setCustomDomain(customDomain);
        if (description != null) statusPage.setDescription(description);
        if (logoUrl != null) statusPage.setLogoUrl(logoUrl);
        if (primaryColor != null) statusPage.setPrimaryColor(primaryColor);

        StatusPage updated = statusPageRepository.save(statusPage);

        try {
            redisTemplate.delete("cache:statuspage:" + oldSlug);
            if (slug != null) redisTemplate.delete("cache:statuspage:" + slug);
        } catch (Exception ignored) {}

        return updated;
    }

    @Transactional
    public void deleteStatusPage(UUID statusPageId, UUID userId) {
        StatusPage statusPage = statusPageRepository.findById(statusPageId)
                .orElseThrow(() -> new ResourceNotFoundException("Status page not found: " + statusPageId));

        if (!statusPage.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Status page not found: " + statusPageId);
        }

        statusPageRepository.delete(statusPage);

        try {
            redisTemplate.delete("cache:statuspage:" + statusPage.getSlug());
        } catch (Exception ignored) {}
    }

    private void validateMonitorOwnership(UUID userId, UUID[] monitorIds) {
        for (UUID monitorId : monitorIds) {
            Monitor monitor = monitorRepository.findById(monitorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Monitor not found: " + monitorId));
            if (!monitor.getUser().getId().equals(userId)) {
                throw new ResourceNotFoundException("Monitor not found: " + monitorId);
            }
        }
    }
}
