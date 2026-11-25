package io.github.azaev.questjournal.common;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Business error that maps directly to an HTTP status and a ProblemDetail response.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, Object> properties;

    public ApiException(HttpStatus status, String message) {
        this(status, message, Map.of());
    }

    public ApiException(HttpStatus status, String message, Map<String, Object> properties) {
        super(message);
        this.status = status;
        this.properties = properties;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public static ApiException notFound(String what) {
        return new ApiException(HttpStatus.NOT_FOUND, what + " not found");
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }
}
