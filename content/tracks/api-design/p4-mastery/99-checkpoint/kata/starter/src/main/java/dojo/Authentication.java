package dojo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Authentication, written for you and correct: every request to /invoices needs
 * {@code Authorization: Bearer <key>}, and the tenant of the key is put in the request
 * attribute {@link #TENANT} (read it with {@code @RequestAttribute(Authentication.TENANT)}).
 * A missing or unknown key throws {@link UnauthorizedException}, which ApiProblems answers.
 */
@Configuration
public class Authentication implements WebMvcConfigurer {
    public static final String TENANT = "dojo.tenant";

    public static final class UnauthorizedException extends RuntimeException {}

    private final Tenants tenants;

    public Authentication(Tenants tenants) {
        this.tenants = tenants;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                String header = request.getHeader(HttpHeaders.AUTHORIZATION);
                String key = header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : null;
                request.setAttribute(TENANT, tenants.authenticate(key).orElseThrow(UnauthorizedException::new));
                return true;
            }
        }).addPathPatterns("/invoices", "/invoices/**");
    }
}
