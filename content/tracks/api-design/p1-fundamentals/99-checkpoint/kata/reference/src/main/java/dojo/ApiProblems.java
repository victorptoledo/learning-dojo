package dojo;

import dojo.OrderExceptions.InvalidParameterException;
import dojo.OrderExceptions.OrderNotCancellableException;
import dojo.OrderExceptions.OrderNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Every error as an RFC 9457 problem detail. */
@RestControllerAdvice
public class ApiProblems extends ResponseEntityExceptionHandler {
    /** The base of every problem type URI. */
    static final String TYPES = "https://api.dojo.example/problems/";

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail notFound(OrderNotFoundException e, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "order-not-found", "Order not found", e.getMessage(), request);
    }

    @ExceptionHandler(OrderNotCancellableException.class)
    ProblemDetail notCancellable(OrderNotCancellableException e, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "order-not-cancellable", "Order cannot be cancelled", e.getMessage(),
                request);
    }

    @ExceptionHandler(InvalidParameterException.class)
    ProblemDetail invalidParameter(InvalidParameterException e, HttpServletRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "invalid-parameter", "Invalid query parameter",
                e.getMessage(), request);
        problem.setProperty("parameter", e.parameter());
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("pointer", "#/" + error.getField(),
                        "detail", String.valueOf(error.getDefaultMessage())))
                .sorted(Comparator.comparing((Map<String, String> error) -> error.get("pointer")))
                .toList();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation-error", "Invalid request",
                "the request has " + errors.size() + " invalid field" + (errors.size() == 1 ? "" : "s"),
                ((ServletWebRequest) request).getRequest());
        problem.setProperty("errors", errors);
        return handleExceptionInternal(e, problem, headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPES + type));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
