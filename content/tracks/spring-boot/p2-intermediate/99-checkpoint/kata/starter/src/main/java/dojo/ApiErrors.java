package dojo;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiErrors extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ShopException.class)
    public ProblemDetail shop(ShopException e) {
        return ProblemDetail.forStatusAndDetail(e.status(), e.getMessage());
    }
}
