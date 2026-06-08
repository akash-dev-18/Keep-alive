package akakash.backend.alert;

import akakash.backend.incident.IncidentRepository;
import akakash.backend.monitor.MonitorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.http.*;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertWorker {

    private final StringRedisTemplate redisTemplate;
    private final MonitorRepository monitorRepository;
    private final IncidentRepository incidentRepository;
    private final AlertLogRepository alertLogRepository;
    private final ObjectMapper objectMapper;

    private final AtomicBoolean running = new AtomicBoolean(true);

    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${resend.from-email}")
    private String fromEmail;

    private final org.springframework.web.client.RestTemplate restTemplate =
            new org.springframework.web.client.RestTemplate();

    @PreDestroy
    public void stop() {
        running.set(false);
    }

    @Async("virtualThreadExecutor")
    public void processQueue() {
        log.info("📧 Alert Worker Thread Started: {}", Thread.currentThread());

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                String raw = redisTemplate.opsForList()
                        .leftPop("queue:alerts", Duration.ofSeconds(5));


                if (raw == null || raw.trim().isEmpty() || "null".equalsIgnoreCase(raw.trim())) {
                    continue;
                }

                AlertJob job;
                try {
                    job = objectMapper.readValue(raw, AlertJob.class);
                } catch (Exception e) {
                    log.error(" Invalid AlertJob JSON parsing failed. Raw data: {}", raw);
                    continue;
                }

                try {
                    sendEmail(job);
                    saveAlertLog(job, "sent", null);
                } catch (Exception e) {
                    log.error(" Email send failed for job: {}. Error: {}", job.monitorName(), e.getMessage());
                    saveAlertLog(job, "failed", e.getMessage());
                }

            } catch (Exception e) {
                log.error(" Worker loop global error: {}", e.getMessage());
                try {

                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }


    private void sendEmail(AlertJob job) {

        String to = job.recipientEmail();

        if (to == null || !to.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            log.error("INVALID EMAIL DROPPED: {}", to);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + resendApiKey);

        String subject = buildSubject(job);
        String html = buildHtml(job, subject);

        Map<String, Object> payload = Map.of(
                "from", "KeepAlive <" + fromEmail + ">",
                "to", to,
                "subject", subject,
                "html", html
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(payload, headers);

        try {
            restTemplate.postForEntity(
                    "https://api.resend.com/emails",
                    request,
                    Map.class
            );
        } catch (Exception e) {
            log.error("Resend API failed for {}: {}", to, e.getMessage());
            throw e;
        }
    }

    private String buildHtml(AlertJob job, String subject) {

        String color = switch (job.alertType()) {
            case "down", "cron_missed" -> "#ef4444";
            case "recovery" -> "#10b981";
            case "ssl_expiry" -> "#f59e0b";
            default -> "#06b6d4";
        };

        String body = buildBody(job);

        return """
            <div style="font-family:sans-serif;max-width:520px;margin:auto;background:#0a0a0a;color:#e5e5e5;border-radius:12px;border:1px solid #1f1f1f">
              <div style="padding:24px;border-bottom:1px solid #1f1f1f">
                <b style="color:#06b6d4">KeepAlive</b>
              </div>
              <div style="padding:24px">
                <div style="padding:6px 10px;border-radius:20px;background:%s22;color:%s;border:1px solid %s44;display:inline-block">
                  %s
                </div>
                <p style="margin-top:16px">%s</p>
              </div>
            </div>
            """.formatted(color, color, color, subject, body);
    }

    private String buildBody(AlertJob job) {
        return switch (job.alertType()) {
            case "down" ->
                    "Monitor <b>" + job.monitorName() + "</b> is DOWN";
            case "recovery" ->
                    "Monitor <b>" + job.monitorName() + "</b> is UP again";
            case "ssl_expiry" ->
                    "SSL expiring for <b>" + job.monitorName() + "</b>";
            case "cron_missed" ->
                    "Heartbeat missed for <b>" + job.monitorName() + "</b>";
            default ->
                    "Alert for <b>" + job.monitorName() + "</b>";
        };
    }

    private String buildSubject(AlertJob job) {
        return switch (job.alertType()) {
            case "down" -> job.monitorName() + " is DOWN";
            case "recovery" -> job.monitorName() + " recovered";
            case "ssl_expiry" -> job.monitorName() + " SSL expiring";
            case "cron_missed" -> job.monitorName() + " missed heartbeat";
            default -> job.monitorName() + " alert";
        };
    }

    private void saveAlertLog(AlertJob job, String status, String error) {
        alertLogRepository.save(AlertLog.builder()
                .monitorId(job.monitorId())
                .incidentId(job.incidentId())
                .alertType(job.alertType())
                .channel("email")
                .sentTo(job.recipientEmail())
                .status(status)
                .errorMessage(error)
                .build());
    }
}