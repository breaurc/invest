package ro.valuation.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handle(ResponseStatusException e) {
        log.warn("API error {}: {}", e.getStatusCode(), e.getReason());
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason() == null ? "error" : e.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handle(Exception e) {
        // Spring's own HTTP errors (404 for missing static resources such as favicon.ico, 405, 400 on bad JSON...)
        if (e instanceof ErrorResponse er) {
            log.debug("HTTP error {}: {}", er.getStatusCode(), er.getBody().getDetail());
            return ResponseEntity.status(er.getStatusCode()).body(Map.of("error", String.valueOf(er.getBody().getDetail())));
        }
        log.error("Unhandled API error", e);
        return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage() == null ? "error" : e.getMessage()));
    }
}
