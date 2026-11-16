package dojo;

import dojo.BankExceptions.AccountFrozenException;
import dojo.BankExceptions.AccountNotFoundException;
import dojo.BankExceptions.InsufficientFundsException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Turns every error into an RFC 9457 problem detail. The base class already answers Spring's
 * own errors (malformed JSON, unknown paths, wrong methods) as problem details.
 */
@RestControllerAdvice
public class ApiProblems extends ResponseEntityExceptionHandler {
    /** The base of every problem type URI. */
    static final String TYPES = "https://api.dojo.example/problems/";

    private static final Logger log = LoggerFactory.getLogger(ApiProblems.class);

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail notFound(AccountNotFoundException e, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "account-not-found", "Account not found", e.getMessage(), request);
    }

    @ExceptionHandler(AccountFrozenException.class)
    ProblemDetail frozen(AccountFrozenException e, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "account-frozen", "Account frozen", e.getMessage(), request);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    ProblemDetail insufficientFunds(InsufficientFundsException e, HttpServletRequest request) {
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_CONTENT, "insufficient-funds", "Insufficient funds",
                e.getMessage(), request);
        problem.setProperty("balance", e.balanceCents());
        problem.setProperty("requested", e.requestedCents());
        return problem;
    }

    /** Anything else is our bug: log it, and tell the client nothing about our internals. */
    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e, HttpServletRequest request) {
        log.error("unexpected error on {}", request.getRequestURI(), e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "an unexpected error occurred");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("pointer", "#/" + error.getField(),
                        "detail", String.valueOf(error.getDefaultMessage())))
                .sorted(Comparator.comparing((Map<String, String> error) -> error.get("pointer"))
                        .thenComparing(error -> error.get("detail")))
                .toList();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "the request has " + errors.size() + " invalid field" + (errors.size() == 1 ? "" : "s"));
        problem.setType(URI.create(TYPES + "validation-error"));
        problem.setTitle("Invalid request");
        problem.setInstance(URI.create(((ServletWebRequest) request).getRequest().getRequestURI()));
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
