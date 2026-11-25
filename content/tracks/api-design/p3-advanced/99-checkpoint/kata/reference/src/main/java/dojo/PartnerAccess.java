package dojo;

import dojo.ApiExceptions.InsufficientScopeException;
import dojo.ApiExceptions.KeyInQueryException;
import dojo.ApiExceptions.RateLimitedException;
import dojo.ApiExceptions.UnauthorizedException;
import dojo.Partners.Partner;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** The key check, authentication, rate limiting and scopes, in that order, before every handler. */
@Component
public class PartnerAccess implements HandlerInterceptor {
    static final String PARTNER = "dojo.partner";

    private final Partners partners;
    private final RateLimiter limiter;

    public PartnerAccess(Partners partners, RateLimiter limiter) {
        this.partners = partners;
        this.limiter = limiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getParameter("apiKey") != null) {
            throw new KeyInQueryException();
        }
        Partner partner = partners.authenticate(bearer(request)).orElseThrow(UnauthorizedException::new);
        RateLimiter.Decision decision = limiter.take(partner.id());
        response.setHeader("RateLimit-Policy", RateLimiter.POLICY);
        response.setHeader("RateLimit", decision.header());
        if (!decision.allowed()) {
            throw new RateLimitedException(decision.retryAfterSeconds());
        }
        if (handler instanceof HandlerMethod method && method.hasMethodAnnotation(RequiresScope.class)) {
            String scope = method.getMethodAnnotation(RequiresScope.class).value();
            if (!partner.has(scope)) {
                throw new InsufficientScopeException(scope);
            }
        }
        request.setAttribute(PARTNER, partner);
        return true;
    }

    private static String bearer(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring("Bearer ".length()).trim();
    }
}
