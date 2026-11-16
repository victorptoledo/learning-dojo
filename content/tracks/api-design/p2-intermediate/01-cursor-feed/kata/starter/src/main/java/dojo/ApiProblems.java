package dojo;

import dojo.FeedExceptions.InvalidCursorException;
import dojo.FeedExceptions.InvalidParameterException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Problem details for the feed's errors. Written for you. */
@RestControllerAdvice
public class ApiProblems extends ResponseEntityExceptionHandler {
    static final String TYPES = "https://api.dojo.example/problems/";

    @ExceptionHandler(InvalidCursorException.class)
    ProblemDetail invalidCursor(InvalidCursorException e, HttpServletRequest request) {
        return problem("invalid-cursor", "Invalid cursor", e.getMessage(), request);
    }

    @ExceptionHandler(InvalidParameterException.class)
    ProblemDetail invalidParameter(InvalidParameterException e, HttpServletRequest request) {
        ProblemDetail problem = problem("invalid-parameter", "Invalid query parameter", e.getMessage(), request);
        problem.setProperty("parameter", e.parameter());
        return problem;
    }

    private static ProblemDetail problem(String type, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create(TYPES + type));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
