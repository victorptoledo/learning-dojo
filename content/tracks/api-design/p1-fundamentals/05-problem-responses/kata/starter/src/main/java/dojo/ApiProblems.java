package dojo;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into an RFC 9457 problem detail. The base class already answers Spring's
 * own errors (malformed JSON, unknown paths, wrong methods) as problem details.
 */
@RestControllerAdvice
public class ApiProblems extends ResponseEntityExceptionHandler {
    /** The base of every problem type URI. */
    static final String TYPES = "https://api.dojo.example/problems/";
}
