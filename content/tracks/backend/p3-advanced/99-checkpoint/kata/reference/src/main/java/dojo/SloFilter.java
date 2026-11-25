package dojo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Records each request to /accounts as good (below 500) or bad (5xx, or an exception that escaped). */
@Component
public class SloFilter extends OncePerRequestFilter {
    private final SloTracker tracker;

    public SloFilter(SloTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/accounts");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean good = false;
        try {
            chain.doFilter(request, response);
            good = response.getStatus() < 500;
        } finally {
            tracker.record(good);
        }
    }
}
