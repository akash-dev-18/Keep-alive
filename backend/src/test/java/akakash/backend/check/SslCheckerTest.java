package akakash.backend.check;

import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SslCheckerTest {

    @Mock
    private MonitorRepository monitorRepository;

    @Test
    void checkReturnsDownWhenSslHandshakeCannotBeCompleted() {
        UUID monitorId = UUID.randomUUID();
        Monitor monitor = Monitor.builder()
                .id(monitorId)
                .url("https://localhost")
                .build();
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));

        CheckResult result = new SslChecker(monitorRepository).check(monitorId);

        assertThat(result.status()).isEqualTo("down");
        assertThat(result.errorMessage()).isNotBlank();
    }
}
