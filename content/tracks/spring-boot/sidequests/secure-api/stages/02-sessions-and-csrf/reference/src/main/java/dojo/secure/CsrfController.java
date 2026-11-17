package dojo.secure;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Gives the page the token it must send back on every change. */
@RestController
public class CsrfController {
    public record TokenView(String headerName, String token) {
    }

    @GetMapping("/api/csrf")
    public TokenView csrf(CsrfToken token) {
        return new TokenView(token.getHeaderName(), token.getToken());
    }
}
