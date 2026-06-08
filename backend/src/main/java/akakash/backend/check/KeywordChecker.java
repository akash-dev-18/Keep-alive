package akakash.backend.check;

import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KeywordChecker {

    private final MonitorRepository monitorRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public CheckResult check(UUID monitorId) {
        Monitor monitor = monitorRepository.findById(monitorId).orElseThrow();
        long start = System.currentTimeMillis();

        if (monitor.getExpectedKeyword() == null || monitor.getExpectedKeyword().isBlank()) {
            return new CheckResult(monitorId, "down", null, null, "Expected keyword not configured");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(monitor.getUrl()))
                    .GET()
                    .timeout(Duration.ofSeconds(monitor.getTimeoutSeconds()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long responseTime = System.currentTimeMillis() - start;
            String body = response.body() != null ? response.body() : "";
            boolean found = body.contains(monitor.getExpectedKeyword());

            if (!found) {
                return new CheckResult(monitorId, "down", (int) responseTime, response.statusCode(),
                        "Keyword '" + monitor.getExpectedKeyword() + "' not found in response");
            }
            return new CheckResult(monitorId, "up", (int) responseTime, response.statusCode(), null);

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - start;
            return new CheckResult(monitorId, "down", (int) responseTime, null, e.getMessage());
        }
    }
}
