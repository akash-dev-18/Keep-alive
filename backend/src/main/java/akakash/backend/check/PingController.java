package akakash.backend.check;

import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequiredArgsConstructor
public class PingController {

    private final MonitorRepository monitorRepository;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @GetMapping({"/api/ping/{pingKey}", "/api/v1/heartbeat/{pingKey}"})
    @Transactional
    public ResponseEntity<String> receivePing(@PathVariable String pingKey) {
        String rateLimitKey = "ratelimit:ping:" + pingKey;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(rateLimitKey, "1", java.time.Duration.ofSeconds(1));
        if (Boolean.FALSE.equals(success)) {
            return ResponseEntity.status(429).body("Too many requests");
        }

        Monitor monitor = monitorRepository.findByPingKey(pingKey).orElse(null);
        if (monitor == null) {
            return ResponseEntity.notFound().build();
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expectedNext = now.plusMinutes(monitor.getCheckIntervalMin());
        monitorRepository.updateHeartbeatPing(monitor.getId(), now, expectedNext);

        return ResponseEntity.ok("pong");
    }
}
