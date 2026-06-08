package akakash.backend.check;

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
class KeywordCheckerTest {

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @Test
    void checkReturnsDownWhenKeywordIsMissingFromConfiguration() {
        UUID monitorId = UUID.randomUUID();
        Monitor monitor = keywordMonitor(monitorId, "http://localhost", " ");
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));

        CheckResult result = new KeywordChecker(monitorRepository).check(monitorId);

        assertThat(result.status()).isEqualTo("down");
        assertThat(result.errorMessage()).isEqualTo("Expected keyword not configured");
    }

    @Test
    void checkReturnsUpWhenBodyContainsKeyword() throws Exception {
        UUID monitorId = UUID.randomUUID();
        Monitor monitor = keywordMonitor(monitorId, "https://example.com", "operational");
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("service status: operational");
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        KeywordChecker checker = new KeywordChecker(monitorRepository);
        ReflectionTestUtils.setField(checker, "httpClient", httpClient);
        CheckResult result = checker.check(monitorId);

        assertThat(result.status()).isEqualTo("up");
        assertThat(result.httpStatusCode()).isEqualTo(200);
    }

    @Test
    void checkReturnsDownWhenBodyDoesNotContainKeyword() throws Exception {
        UUID monitorId = UUID.randomUUID();
        Monitor monitor = keywordMonitor(monitorId, "https://example.com", "operational");
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("service status: outage");
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        KeywordChecker checker = new KeywordChecker(monitorRepository);
        ReflectionTestUtils.setField(checker, "httpClient", httpClient);
        CheckResult result = checker.check(monitorId);

        assertThat(result.status()).isEqualTo("down");
        assertThat(result.errorMessage()).contains("Keyword 'operational' not found");
    }

    private Monitor keywordMonitor(UUID monitorId, String url, String keyword) {
        return Monitor.builder()
                .id(monitorId)
                .url(url)
                .expectedKeyword(keyword)
                .timeoutSeconds(5)
                .build();
    }
}
