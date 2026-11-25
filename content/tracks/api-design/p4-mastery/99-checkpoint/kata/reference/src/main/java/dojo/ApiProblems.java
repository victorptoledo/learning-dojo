package dojo;

import dojo.ApiExceptions.AlreadyPaidException;
import dojo.ApiExceptions.AmountMismatchException;
import dojo.ApiExceptions.InvalidCursorException;
import dojo.ApiExceptions.InvalidParameterException;
import dojo.ApiExceptions.InvoiceNotFoundException;
import dojo.ApiExceptions.KeyMissingException;
import dojo.ApiExceptions.KeyReusedException;
import dojo.PaymentGateway.GatewayUnavailableException;
import java.net.URI;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Problem details (RFC 9457): precise for the client's mistakes, silent about our own. */
@RestControllerAdvice
public class ApiProblems extends ResponseEntityExceptionHandler {
    static final String TYPES = "https://api.dojo.example/problems/";
    private static final Logger LOG = LoggerFactory.getLogger(ApiProblems.class);

    /** A problem of type {@code TYPES + type}. */
    static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPES + type));
        problem.setTitle(title);
        return problem;
    }

    @ExceptionHandler(Authentication.UnauthorizedException.class)
    ResponseEntity<ProblemDetail> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"")
                .body(problem(HttpStatus.UNAUTHORIZED, "unauthorized", "Unauthorized",
                        "a valid API key is required: Authorization: Bearer <key>"));
    }

    @ExceptionHandler(InvoiceNotFoundException.class)
    ProblemDetail notFound(InvoiceNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "invoice-not-found", "Invoice not found", e.getMessage());
    }

    @ExceptionHandler(InvalidParameterException.class)
    ProblemDetail invalidParameter(InvalidParameterException e) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-parameter", "Invalid parameter", e.getMessage());
    }

    @ExceptionHandler(InvalidCursorException.class)
    ProblemDetail invalidCursor(InvalidCursorException e) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-cursor", "Invalid cursor", e.getMessage());
    }

    @ExceptionHandler(KeyMissingException.class)
    ProblemDetail keyMissing(KeyMissingException e) {
        return problem(HttpStatus.BAD_REQUEST, "idempotency-key-missing", "Idempotency key missing", e.getMessage());
    }

    @ExceptionHandler(KeyReusedException.class)
    ProblemDetail keyReused(KeyReusedException e) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "idempotency-key-reused", "Idempotency key reused",
                e.getMessage());
    }

    @ExceptionHandler(AlreadyPaidException.class)
    ProblemDetail alreadyPaid(AlreadyPaidException e) {
        return problem(HttpStatus.CONFLICT, "invoice-already-paid", "Invoice already paid", e.getMessage());
    }

    @ExceptionHandler(AmountMismatchException.class)
    ProblemDetail amountMismatch(AmountMismatchException e) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "amount-mismatch", "Amount mismatch", e.getMessage());
    }

    @ExceptionHandler(GatewayUnavailableException.class)
    ResponseEntity<ProblemDetail> providerDown() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "30")
                .body(problem(HttpStatus.SERVICE_UNAVAILABLE, "payment-provider-unavailable",
                        "Payment provider unavailable",
                        "the payment provider is not answering; nothing was charged, retry later with the same key"));
    }

    /** Our own bugs: log everything, tell the client nothing but an id to quote. */
    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e) {
        URI incident = URI.create("urn:uuid:" + UUID.randomUUID());
        LOG.error("unexpected error {}", incident, e);
        ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal Server Error",
                "something went wrong on our side; quote the instance if you contact support");
        problem.setInstance(incident);
        return problem;
    }
}
