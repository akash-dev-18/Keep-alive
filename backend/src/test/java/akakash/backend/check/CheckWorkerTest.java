package akakash.backend.check;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckWorkerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private HttpChecker httpChecker;

    @Mock
    private SslChecker sslChecker;

    @Mock
    private CronChecker cronChecker;

    @Mock
    private KeywordChecker keywordChecker;

    @Mock
    private CheckResultWriter checkResultWriter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void processQueueRoutesHttpJobAndWritesResult() throws Exception {
        UUID monitorId = UUID.randomUUID();
        CheckJob job = new CheckJob(monitorId, "http");
        CheckResult result = new CheckResult(monitorId, "up", 42, 200, null);
        AtomicReference<CheckWorker> workerRef = new AtomicReference<>();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPop(eq("queue:checks"), any(Duration.class)))
                .thenReturn(objectMapper.writeValueAsString(job));
        when(httpChecker.check(monitorId)).thenReturn(result);
        doAnswer(invocation -> {
            workerRef.get().stop();
            return null;
        }).when(checkResultWriter).write(result);

        CheckWorker worker = new CheckWorker(
                redisTemplate,
                httpChecker,
                sslChecker,
                cronChecker,
                keywordChecker,
                checkResultWriter,
                objectMapper
        );
        workerRef.set(worker);

        worker.processQueue();

        verify(httpChecker).check(monitorId);
        verify(checkResultWriter).write(result);
    }

    @Test
    void processQueueIgnoresInvalidJobType() throws Exception {
        UUID monitorId = UUID.randomUUID();
        CheckJob job = new CheckJob(monitorId, "bad-type");
        AtomicReference<CheckWorker> workerRef = new AtomicReference<>();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPop(eq("queue:checks"), any(Duration.class)))
                .thenAnswer(invocation -> {
                    workerRef.get().stop();
                    return objectMapper.writeValueAsString(job);
                });

        CheckWorker worker = new CheckWorker(
                redisTemplate,
                httpChecker,
                sslChecker,
                cronChecker,
                keywordChecker,
                checkResultWriter,
                objectMapper
        );
        workerRef.set(worker);

        worker.processQueue();

        verify(checkResultWriter, never()).write(any());
    }
}
