package dojo;

import dojo.ApiExceptions.ExportNotFoundException;
import dojo.ApiExceptions.InsufficientScopeException;
import dojo.ApiExceptions.KeyInQueryException;
import dojo.ApiExceptions.RateLimitedException;
import dojo.ApiExceptions.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Problem details (RFC 9457) for every problem of the exports API. */
@RestControllerAdvice
public class ApiProblems extends ResponseEntityExceptionHandler {
    static final String TYPES = "https://api.dojo.example/problems/";

    /** A problem of type {@code TYPES + type}. */
    static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPES + type));
        problem.setTitle(title);
        return problem;
    }

    @ExceptionHandler(KeyInQueryException.class)
    ProblemDetail keyInQuery() {
        return problem(HttpStatus.BAD_REQUEST, "key-in-query", "API key in the query string",
                "send the API key in the Authorization header, never in the URL");
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ProblemDetail> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"")
                .body(problem(HttpStatus.UNAUTHORIZED, "unauthorized", "Unauthorized",
                        "a valid API key is required: Authorization: Bearer <key>"));
    }

    @ExceptionHandler(InsufficientScopeException.class)
    ResponseEntity<ProblemDetail> insufficientScope(InsufficientScopeException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .header(HttpHeaders.WWW_AUTHENTICATE,
                        "Bearer error=\"insufficient_scope\", scope=\"" + e.scope + "\"")
                .body(problem(HttpStatus.FORBIDDEN, "insufficient-scope", "Insufficient scope",
                        "this API key needs the scope " + e.scope));
    }

    @ExceptionHandler(RateLimitedException.class)
    ResponseEntity<ProblemDetail> rateLimited(RateLimitedException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.retryAfterSeconds))
                .body(problem(HttpStatus.TOO_MANY_REQUESTS, "rate-limited", "Too many requests",
                        "this API key may send 10 requests in a burst, refilled at one per second"));
    }

    @ExceptionHandler(ExportNotFoundException.class)
    ProblemDetail notFound(ExportNotFoundException e, HttpServletRequest request) {
        ProblemDetail problem = problem(HttpStatus.NOT_FOUND, "export-not-found", "Export not found", e.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
