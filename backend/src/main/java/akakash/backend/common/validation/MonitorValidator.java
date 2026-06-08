package akakash.backend.common.validation;

import akakash.backend.common.enums.MonitorType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public final class MonitorValidator {

    private static final Set<String> ALLOWED_HTTP_METHODS = Set.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD"
    );

    private MonitorValidator() {}

    public static void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL is required");
        }
        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("URL must use http or https scheme");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("URL must have a valid host");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL format");
        }
    }

    public static void validateInterval(int intervalMin) {
        if (intervalMin < 1) {
            throw new IllegalArgumentException("Check interval must be at least 1 minute");
        }
    }

    public static void validateTimeout(int timeoutSeconds) {
        if (!MonitorType.ALLOWED_TIMEOUTS.contains(timeoutSeconds)) {
            throw new IllegalArgumentException("Timeout must be one of: 5, 10, 20, 30, 60 seconds");
        }
    }

    public static void validateAlertAfterFailures(int alertAfterFailures) {
        if (!MonitorType.ALLOWED_ALERT_AFTER.contains(alertAfterFailures)) {
            throw new IllegalArgumentException("Alert after failures must be one of: 0, 1, 2, 3, 5");
        }
    }

    public static void validateHttpMethod(String method) {
        if (method == null || !ALLOWED_HTTP_METHODS.contains(method.toUpperCase())) {
            throw new IllegalArgumentException("Invalid HTTP method");
        }
    }

    public static void validateKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Expected keyword is required for keyword monitors");
        }
    }
}
