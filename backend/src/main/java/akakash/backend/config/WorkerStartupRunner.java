package akakash.backend.config;

import akakash.backend.alert.AlertWorker;
import akakash.backend.check.CheckWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkerStartupRunner implements CommandLineRunner {

    private final CheckWorker checkWorker;
    private final AlertWorker alertWorker;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting background queue workers...");
        
        // Spin up 5 check workers
        for (int i = 0; i < 5; i++) {
            Executors.newVirtualThreadPerTaskExecutor().submit(checkWorker::processQueue);
        }

        for (int i = 0; i < 2; i++) {
            Executors.newVirtualThreadPerTaskExecutor().submit(alertWorker::processQueue);
        }
    }
}
