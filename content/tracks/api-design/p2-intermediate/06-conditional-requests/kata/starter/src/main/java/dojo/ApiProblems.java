package dojo;

import dojo.DocumentExceptions.DocumentNotFoundException;
import dojo.DocumentExceptions.PreconditionFailedException;
import dojo.DocumentExceptions.PreconditionRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Problem details for the documents API. Written for you. */
@RestControllerAdvice
public class ApiProblems extends ResponseEntityExceptionHandler {
    static final String TYPES = "https://api.dojo.example/problems/";

    @ExceptionHandler(DocumentNotFoundException.class)
    ProblemDetail notFound(DocumentNotFoundException e, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "document-not-found", "Document not found", e, request);
    }

    @ExceptionHandler(PreconditionRequiredException.class)
    ProblemDetail required(PreconditionRequiredException e, HttpServletRequest request) {
        return problem(HttpStatus.PRECONDITION_REQUIRED, "precondition-required", "Precondition required", e, request);
    }

    @ExceptionHandler(PreconditionFailedException.class)
    ProblemDetail failed(PreconditionFailedException e, HttpServletRequest request) {
        return problem(HttpStatus.PRECONDITION_FAILED, "precondition-failed", "Precondition failed", e, request);
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, Exception e,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setType(URI.create(TYPES + type));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
