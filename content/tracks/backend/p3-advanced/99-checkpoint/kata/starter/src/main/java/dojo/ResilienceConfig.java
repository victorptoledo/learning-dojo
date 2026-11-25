package dojo;

import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** One breaker for the rates service, shared by every request. The tests replace the clock and the sleeper. */
@Configuration
public class ResilienceConfig {
    @Bean
    RetryPolicy ratesRetry() {
        return new RetryPolicy(3, Duration.ofMillis(100), 2.0, Duration.ofSeconds(1));
    }

    @Bean
    CircuitBreaker ratesBreaker(Clock clock) {
        return new CircuitBreaker(5, Duration.ofSeconds(30), clock);
    }

    @Bean
    Sleeper sleeper() {
        return duration -> {
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        };
    }
}
