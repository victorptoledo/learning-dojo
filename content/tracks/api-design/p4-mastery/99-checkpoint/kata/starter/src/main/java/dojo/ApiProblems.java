package dojo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Problem details (RFC 9457). Validation errors get Spring's default 400 problem. */
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

    /** Written for you, and correct: keep it. */
    @ExceptionHandler(Authentication.UnauthorizedException.class)
    ResponseEntity<ProblemDetail> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"")
                .body(problem(HttpStatus.UNAUTHORIZED, "unauthorized", "Unauthorized",
                        "a valid API key is required: Authorization: Bearer <key>"));
    }

    /** "So that support can debug from the screenshot" (added during an incident). */
    @ExceptionHandler(Exception.class)
    ProblemDetail anything(Exception e) {
        StringWriter trace = new StringWriter();
        e.printStackTrace(new PrintWriter(trace));
        ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal Server Error",
                e.toString());
        problem.setProperty("trace", trace.toString());
        return problem;
    }
}
