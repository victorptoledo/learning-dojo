package dojo;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Problem Details for every error (see the README). */
@RestControllerAdvice
public class ApiErrors extends ResponseEntityExceptionHandler {
}
