package akakash.backend.check;

import akakash.backend.config.SecretEncryptionService;
import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HttpChecker {

    private final MonitorRepository monitorRepository;
    private final SecretEncryptionService encryptionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public CheckResult check(UUID monitorId) {
        Monitor monitor = monitorRepository.findById(monitorId).orElseThrow();
        long start = System.currentTimeMillis();

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(monitor.getUrl()))
                    .timeout(Duration.ofSeconds(monitor.getTimeoutSeconds()));

            if (monitor.getRequestHeaders() != null && !monitor.getRequestHeaders().isBlank()) {
                try {
                    Map<String, String> headers = objectMapper.readValue(
                            monitor.getRequestHeaders(),
                            new TypeReference<Map<String, String>>() {}
                    );
                    headers.forEach(builder::header);
                } catch (Exception ignored) {}
            }

            if (monitor.getBasicAuthUsername() != null && monitor.getBasicAuthPasswordEnc() != null) {
                String password = encryptionService.decrypt(monitor.getBasicAuthPasswordEnc());
                String credentials = Base64.getEncoder().encodeToString(
                        (monitor.getBasicAuthUsername() + ":" + password).getBytes(StandardCharsets.UTF_8));
                builder.header("Authorization", "Basic " + credentials);
            } else if (monitor.getBearerTokenEnc() != null) {
                String token = encryptionService.decrypt(monitor.getBearerTokenEnc());
                builder.header("Authorization", "Bearer " + token);
            }

            String method = monitor.getHttpMethod() != null ? monitor.getHttpMethod().toUpperCase() : "GET";
            HttpRequest.BodyPublisher bodyPublisher;
            if (monitor.getRequestBody() != null && !monitor.getRequestBody().isBlank()
                    && ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method))) {
                bodyPublisher = HttpRequest.BodyPublishers.ofString(monitor.getRequestBody());
            } else {
                bodyPublisher = HttpRequest.BodyPublishers.noBody();
            }

            builder.method(method, bodyPublisher);
            HttpRequest request = builder.build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long responseTime = System.currentTimeMillis() - start;
            boolean isUp = response.statusCode() == monitor.getExpectedStatus();

            return new CheckResult(monitorId, isUp ? "up" : "down", (int) responseTime, response.statusCode(),
                    isUp ? null : "Expected status " + monitor.getExpectedStatus() + " but got " + response.statusCode());

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - start;
            return new CheckResult(monitorId, "down", (int) responseTime, null, e.getMessage());
        }
    }
}
