package dojo;

import dojo.Gateway.GatewayUnavailableException;
import dojo.IdempotencyExceptions.KeyMissingException;
import dojo.IdempotencyExceptions.KeyReusedException;
import dojo.IdempotencyExceptions.RequestInProgressException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Problem details for the idempotency errors and the gateway outage. Written for you. */
@RestControllerAdvice
public class ApiProblems extends ResponseEntityExceptionHandler {
    static final String TYPES = "https://api.dojo.example/problems/";

    @ExceptionHandler(KeyMissingException.class)
    ProblemDetail missing(KeyMissingException e, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "idempotency-key-missing", "Idempotency key missing", e, request);
    }

    @ExceptionHandler(KeyReusedException.class)
    ProblemDetail reused(KeyReusedException e, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "idempotency-key-reused", "Idempotency key reused", e,
                request);
    }

    /** 409 with Retry-After: the client should retry the same request a moment later. */
    @ExceptionHandler(RequestInProgressException.class)
    ResponseEntity<ProblemDetail> inProgress(RequestInProgressException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .header(HttpHeaders.RETRY_AFTER, "1")
                .body(problem(HttpStatus.CONFLICT, "request-in-progress", "Request in progress", e, request));
    }

    /** 503 with Retry-After: safe to retry with the same key. */
    @ExceptionHandler(GatewayUnavailableException.class)
    ResponseEntity<ProblemDetail> unavailable(GatewayUnavailableException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "2")
                .body(problem(HttpStatus.SERVICE_UNAVAILABLE, "gateway-unavailable", "Payment gateway unavailable",
                        e, request));
    }

    static ProblemDetail problem(HttpStatus status, String type, String title, Exception e,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setType(URI.create(TYPES + type));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
