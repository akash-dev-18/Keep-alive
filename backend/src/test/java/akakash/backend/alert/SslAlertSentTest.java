package akakash.backend.alert;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SslAlertSentTest {

    @Test
    void onCreateDefaultsSentAtWhenMissing() {
        SslAlertSent alertSent = SslAlertSent.builder()
                .monitorId(UUID.randomUUID())
                .thresholdDays(30)
                .build();

        alertSent.onCreate();

        assertThat(alertSent.getSentAt()).isNotNull();
    }
}
