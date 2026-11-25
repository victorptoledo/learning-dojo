package dojo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class RatesHealthIndicator implements HealthIndicator {
    private final RateRepository rates;
    private final Clock clock;
    private final Duration maxAge;

    public RatesHealthIndicator(RateRepository rates, Clock clock, @Value("${desk.max-age}") Duration maxAge) {
        this.rates = rates;
        this.clock = clock;
        this.maxAge = maxAge;
    }

    @Override
    public Health health() {
        Instant oldestAllowed = clock.instant().minus(maxAge);
        List<String> stale = rates.findAll().stream()
                .filter(rate -> rate.getUpdatedAt().isBefore(oldestAllowed))
                .map(Rate::getCode)
                .sorted()
                .toList();
        if (stale.isEmpty()) {
            return Health.up().build();
        }
        return Health.down().withDetail("stale", stale).build();
    }
}
