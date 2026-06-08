package akakash.backend.check;

import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SslChecker {

    private final MonitorRepository monitorRepository;

    public CheckResult check(UUID monitorId) {
        Monitor monitor = monitorRepository.findById(monitorId).orElseThrow();
        String host = URI.create(monitor.getUrl()).getHost();

        try (SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(host, 443)) {
            socket.startHandshake();
            X509Certificate cert = (X509Certificate) socket.getSession().getPeerCertificates()[0];

            Date expiryDate = cert.getNotAfter();
            String issuer = cert.getIssuerX500Principal().getName();
            long daysRemaining = ChronoUnit.DAYS.between(
                    OffsetDateTime.now().toLocalDate(),
                    expiryDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            );

            monitorRepository.updateSslDetails(monitorId, (int) daysRemaining,
                    expiryDate.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime(), issuer);

            if (daysRemaining <= 0) {
                return new CheckResult(monitorId, "down", null, null, "SSL certificate expired");
            }
            if (daysRemaining <= 30) {
                return new CheckResult(monitorId, "degraded", null, null,
                        "SSL certificate expires in " + daysRemaining + " days");
            }
            return new CheckResult(monitorId, "up", null, null, null);

        } catch (Exception e) {
            return new CheckResult(monitorId, "down", null, null, e.getMessage());
        }
    }
}
