package akakash.backend.check;

import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckEnqueuerTest {

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void enqueueOnePushesRedisJobAndMovesNextCheckForward() throws Exception {
        UUID monitorId = UUID.randomUUID();
        Monitor monitor = Monitor.builder()
                .id(monitorId)
                .type("http")
                .checkIntervalMin(5)
                .build();

        when(redisTemplate.opsForList()).thenReturn(listOperations);

        new CheckEnqueuer(monitorRepository, redisTemplate, objectMapper).enqueueOne(monitor);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(listOperations).rightPush(eq("queue:checks"), payloadCaptor.capture());
        CheckJob job = objectMapper.readValue(payloadCaptor.getValue(), CheckJob.class);
        assertThat(job.monitorId()).isEqualTo(monitorId);
        assertThat(job.type()).isEqualTo("http");

        ArgumentCaptor<OffsetDateTime> nextCheckCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(monitorRepository).updateNextCheckAt(eq(monitorId), nextCheckCaptor.capture());
        assertThat(nextCheckCaptor.getValue()).isAfter(OffsetDateTime.now().plusMinutes(4));
        verify(redisTemplate).opsForList();
    }
}
