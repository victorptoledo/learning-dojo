package dojo;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Problem details for every error. A missing X-User header is 401: the gateway did not authenticate the caller. */
@RestControllerAdvice
public class ApiErrors extends ResponseEntityExceptionHandler {
    @ExceptionHandler(StatementsException.class)
    public ResponseEntity<ProblemDetail> statements(StatementsException e) {
        var response = ResponseEntity.status(e.status());
        if (e.retryAfter() != null) {
            response.header(HttpHeaders.RETRY_AFTER, String.valueOf(e.retryAfter().toSeconds()));
        }
        return response.body(ProblemDetail.forStatusAndDetail(e.status(), e.getMessage()));
    }

    @ExceptionHandler(UnknownCurrencyException.class)
    public ProblemDetail unknownCurrency(UnknownCurrencyException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex instanceof MissingRequestHeaderException missing && "X-User".equals(missing.getHeaderName())) {
            var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "no authenticated user");
            return handleExceptionInternal(ex, problem, headers, HttpStatus.UNAUTHORIZED, request);
        }
        return super.handleServletRequestBindingException(ex, headers, status, request);
    }
}
