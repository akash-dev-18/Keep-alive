package akakash.backend.check;

import akakash.backend.config.SecretEncryptionService;
import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpCheckerTest {

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private SecretEncryptionService encryptionService;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @Test
    void checkReturnsUpWhenExpectedStatusMatches() throws Exception {
        UUID monitorId = UUID.randomUUID();
        when(monitorRepository.findById(monitorId))
                .thenReturn(Optional.of(httpMonitor(monitorId, "https://example.com/health", 204)));
        when(httpResponse.statusCode()).thenReturn(204);
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        HttpChecker checker = new HttpChecker(monitorRepository, encryptionService);
        ReflectionTestUtils.setField(checker, "httpClient", httpClient);
        CheckResult result = checker.check(monitorId);

        assertThat(result.status()).isEqualTo("up");
        assertThat(result.httpStatusCode()).isEqualTo(204);
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void checkReturnsDownWhenStatusDoesNotMatch() throws Exception {
        UUID monitorId = UUID.randomUUID();
        when(monitorRepository.findById(monitorId))
                .thenReturn(Optional.of(httpMonitor(monitorId, "https://example.com/health", 200)));
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        HttpChecker checker = new HttpChecker(monitorRepository, encryptionService);
        ReflectionTestUtils.setField(checker, "httpClient", httpClient);
        CheckResult result = checker.check(monitorId);

        assertThat(result.status()).isEqualTo("down");
        assertThat(result.httpStatusCode()).isEqualTo(500);
        assertThat(result.errorMessage()).contains("Expected status 200 but got 500");
    }

    private Monitor httpMonitor(UUID monitorId, String url, int expectedStatus) {
        return Monitor.builder()
                .id(monitorId)
                .url(url)
                .httpMethod("GET")
                .expectedStatus(expectedStatus)
                .timeoutSeconds(5)
                .build();
    }
}
