package dojo;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Errors as problem details. todo: answer a refused request body (@Valid) with a 400 problem
 * whose "errors" property maps each field to its message.
 */
@RestControllerAdvice
public class ApiErrors extends ResponseEntityExceptionHandler {
}
