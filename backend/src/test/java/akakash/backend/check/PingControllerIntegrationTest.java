package akakash.backend.check;

import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PingController.class)
class PingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonitorRepository monitorRepository;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private ValueOperations<String, Object> valueOperations;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void receivePingUpdatesHeartbeatAndReturnsPong() throws Exception {
        String pingKey = "abc123";
        UUID monitorId = UUID.randomUUID();
        Monitor monitor = Monitor.builder()
                .id(monitorId)
                .pingKey(pingKey)
                .checkIntervalMin(5)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("ratelimit:ping:" + pingKey), eq("1"), any(Duration.class)))
                .thenReturn(true);
        when(monitorRepository.findByPingKey(pingKey)).thenReturn(Optional.of(monitor));

        mockMvc.perform(get("/api/ping/{pingKey}", pingKey))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));

        verify(monitorRepository).updateHeartbeatPing(eq(monitorId), any(OffsetDateTime.class), any(OffsetDateTime.class));
    }

    @Test
    void receivePingReturnsTooManyRequestsWhenRateLimited() throws Exception {
        String pingKey = "abc123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("ratelimit:ping:" + pingKey), eq("1"), any(Duration.class)))
                .thenReturn(false);

        mockMvc.perform(get("/api/v1/heartbeat/{pingKey}", pingKey))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string("Too many requests"));

        verify(monitorRepository, never()).findByPingKey(pingKey);
    }

    @Test
    void receivePingReturnsNotFoundForUnknownPingKey() throws Exception {
        String pingKey = "missing";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("ratelimit:ping:" + pingKey), eq("1"), any(Duration.class)))
                .thenReturn(true);
        when(monitorRepository.findByPingKey(pingKey)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/ping/{pingKey}", pingKey))
                .andExpect(status().isNotFound());
    }
}
