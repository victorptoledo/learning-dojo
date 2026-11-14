package dojo;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into Problem Details. ResponseEntityExceptionHandler already answers
 * Spring's own errors (bad JSON, 404, 405...) as Problem Details: override its
 * handleMethodArgumentNotValid for validation, and add one @ExceptionHandler per domain exception.
 */
@RestControllerAdvice
public class ApiErrors extends ResponseEntityExceptionHandler {
}
