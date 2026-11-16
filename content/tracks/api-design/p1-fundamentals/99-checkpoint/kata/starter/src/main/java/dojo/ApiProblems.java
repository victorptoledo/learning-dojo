package dojo;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Every error as an RFC 9457 problem detail. */
@RestControllerAdvice
public class ApiProblems extends ResponseEntityExceptionHandler {
    /** The base of every problem type URI. */
    static final String TYPES = "https://api.dojo.example/problems/";
}
