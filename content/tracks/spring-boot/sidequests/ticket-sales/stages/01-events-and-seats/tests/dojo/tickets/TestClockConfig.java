package dojo.tickets;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Puts the hand-moved clock in place of the application one. */
@TestConfiguration
class TestClockConfig {
    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock();
    }
}
