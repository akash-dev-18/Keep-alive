package akakash.backend.check;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckWorker {

    private final StringRedisTemplate redisTemplate;
    private final HttpChecker httpChecker;
    private final SslChecker sslChecker;
    private final CronChecker cronChecker;
    private final KeywordChecker keywordChecker;
    private final CheckResultWriter checkResultWriter;
    private final ObjectMapper objectMapper;

    private final AtomicBoolean running = new AtomicBoolean(true);

    @PreDestroy
    public void stop() {
        running.set(false);
    }

    @Async("virtualThreadExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void processQueue() {

        log.info("CheckWorker started");

        while (running.get()) {
            try {

                String raw = redisTemplate.opsForList().rightPop("queue:checks", Duration.ofSeconds(5));

                if (raw == null) continue;
                CheckJob job = objectMapper.readValue(raw, CheckJob.class);
                try {
                    CheckResult result = switch (job.type()) {
                        case "http" -> httpChecker.check(job.monitorId());
                        case "ssl" -> sslChecker.check(job.monitorId());
                        case "cron" -> cronChecker.evaluate(job.monitorId());
                        case "keyword" -> keywordChecker.check(job.monitorId());
                        default -> {
                            log.error("Invalid job type: {}", job.type());
                            yield null;
                        }
                    };

                    if (result != null) {
                        checkResultWriter.write(result);
                    }

                } catch (Exception ex) {
                    log.error("Check execution failed for job {}", job, ex);
                }

            } catch (Exception e) {
                log.error("Worker loop error: {}", e.getMessage(), e);
            }
        }

        log.info("CheckWorker stopped");
    }
}