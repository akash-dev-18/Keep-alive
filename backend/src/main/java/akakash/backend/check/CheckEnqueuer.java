package akakash.backend.check;

import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckEnqueuer {

    private final MonitorRepository monitorRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void enqueueOne(Monitor monitor) {
        try {
            CheckJob job = new CheckJob(monitor.getId(), monitor.getType());

            redisTemplate.opsForList()
                    .rightPush("queue:checks", objectMapper.writeValueAsString(job));

            monitorRepository.updateNextCheckAt(
                    monitor.getId(),
                    OffsetDateTime.now().plusMinutes(monitor.getCheckIntervalMin())
            );

        } catch (Exception e) {
            log.error("Failed to enqueue check job for monitor {}: {}", monitor.getId(), e.getMessage());
            throw new RuntimeException("Failed to enqueue check job", e);
        }
    }
}