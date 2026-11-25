package dojo;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Problem details (RFC 9457). Validation errors already get Spring's 400 problem; add an
 * {@code @ExceptionHandler} for each problem the rules need.
 */
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
}
