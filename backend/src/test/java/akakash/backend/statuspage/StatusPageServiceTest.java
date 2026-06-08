package akakash.backend.statuspage;

import akakash.backend.common.exception.PlanLimitException;
import akakash.backend.common.exception.ResourceNotFoundException;
import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import akakash.backend.user.Plan;
import akakash.backend.user.User;
import akakash.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusPageServiceTest {

    @Mock
    private StatusPageRepository statusPageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void createStatusPageValidatesPlanLimitMonitorOwnershipAndDefaultsColor() {
        UUID userId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();
        User user = User.builder().id(userId).plan(Plan.FREE).build();
        Monitor monitor = Monitor.builder().id(monitorId).user(user).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(statusPageRepository.countByUserId(userId)).thenReturn(0L);
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(statusPageRepository.save(org.mockito.ArgumentMatchers.any(StatusPage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StatusPage statusPage = service().createStatusPage(
                userId,
                "Public Status",
                "public-status",
                new UUID[]{monitorId},
                true,
                null,
                "Current service health",
                null,
                null
        );

        assertThat(statusPage.getUserId()).isEqualTo(userId);
        assertThat(statusPage.getMonitorIds()).containsExactly(monitorId);
        assertThat(statusPage.getPrimaryColor()).isEqualTo("#06b6d4");
    }

    @Test
    void createStatusPageRejectsFreePlanLimit() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).plan(Plan.FREE).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(statusPageRepository.countByUserId(userId)).thenReturn(1L);

        assertThatThrownBy(() -> service().createStatusPage(
                userId,
                "Status",
                "status",
                new UUID[0],
                true,
                null,
                null,
                null,
                null
        )).isInstanceOf(PlanLimitException.class);
    }

    @Test
    void updateStatusPageRejectsMonitorOwnedByDifferentUser() {
        UUID userId = UUID.randomUUID();
        UUID statusPageId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();
        StatusPage page = StatusPage.builder()
                .id(statusPageId)
                .userId(userId)
                .slug("old")
                .monitorIds(new UUID[0])
                .build();
        Monitor otherUsersMonitor = Monitor.builder()
                .id(monitorId)
                .user(User.builder().id(UUID.randomUUID()).build())
                .build();

        when(statusPageRepository.findById(statusPageId)).thenReturn(Optional.of(page));
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(otherUsersMonitor));

        assertThatThrownBy(() -> service().updateStatusPage(
                statusPageId,
                userId,
                null,
                null,
                new UUID[]{monitorId},
                true,
                null,
                null,
                null,
                null
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteStatusPageDeletesOwnedPageAndInvalidatesCache() {
        UUID userId = UUID.randomUUID();
        UUID statusPageId = UUID.randomUUID();
        StatusPage page = StatusPage.builder()
                .id(statusPageId)
                .userId(userId)
                .slug("status")
                .build();

        when(statusPageRepository.findById(statusPageId)).thenReturn(Optional.of(page));

        service().deleteStatusPage(statusPageId, userId);

        verify(statusPageRepository).delete(page);
        verify(redisTemplate).delete("cache:statuspage:" + page.getSlug());
    }

    private StatusPageService service() {
        return new StatusPageService(statusPageRepository, userRepository, monitorRepository, redisTemplate);
    }
}
