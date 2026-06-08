package akakash.backend.statuspage;

import akakash.backend.incident.IncidentRepository;
import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import akakash.backend.monitor.MonitorService;
import akakash.backend.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicStatusPageController.class)
class PublicStatusPageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatusPageService statusPageService;

    @MockitoBean
    private MonitorRepository monitorRepository;

    @MockitoBean
    private MonitorService monitorService;

    @MockitoBean
    private IncidentRepository incidentRepository;

    @MockitoBean
    private UptimeCalculator uptimeCalculator;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private ValueOperations<String, Object> valueOperations;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getPublicStatusPageReturnsCachedPayloadWithoutLoadingDependencies() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("cache:statuspage:public"))
                .thenReturn(Map.of("cached", true));

        mockMvc.perform(get("/api/status/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cached").value(true));

        verify(statusPageService, never()).getPublicStatusPage("public");
    }

    @Test
    void getPublicStatusPageReturnsNotFoundWhenPageIsPrivate() throws Exception {
        StatusPage privatePage = StatusPage.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .name("Private")
                .slug("private")
                .monitorIds(new UUID[0])
                .isPublic(false)
                .build();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("cache:statuspage:private")).thenReturn(null);
        when(statusPageService.getPublicStatusPage("private")).thenReturn(privatePage);

        mockMvc.perform(get("/api/status/private"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPublicStatusPageBuildsMonitorsUptimeAndIncidentsThenCachesResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();
        StatusPage page = StatusPage.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Public Status")
                .slug("public")
                .monitorIds(new UUID[]{monitorId})
                .isPublic(true)
                .primaryColor("#06b6d4")
                .build();
        Monitor monitor = Monitor.builder()
                .id(monitorId)
                .user(User.builder().id(userId).build())
                .name("API")
                .type("http")
                .url("https://example.com")
                .checkIntervalMin(2)
                .httpMethod("GET")
                .expectedStatus(200)
                .timeoutSeconds(10)
                .status("up")
                .isActive(true)
                .consecutiveFailures(0)
                .alertAfterFailures(1)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("cache:statuspage:public")).thenReturn(null);
        when(statusPageService.getPublicStatusPage("public")).thenReturn(page);
        when(monitorRepository.findAllById(List.of(monitorId))).thenReturn(List.of(monitor));
        when(monitorService.mapToResponse(monitor)).thenCallRealMethod();
        when(uptimeCalculator.calculateUptimePercent(monitorId, 30)).thenReturn(99.5);
        when(incidentRepository.findByMonitorIdInOrderByStartedAtDesc(eq(List.of(monitorId)), any(Pageable.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/status/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.slug").value("public"))
                .andExpect(jsonPath("$.monitors[0].name").value("API"))
                .andExpect(jsonPath("$.uptime." + monitorId).value(99.5))
                .andExpect(jsonPath("$.incidents").isArray());

        verify(valueOperations).set(eq("cache:statuspage:public"), any(Map.class), any(Duration.class));
    }
}
