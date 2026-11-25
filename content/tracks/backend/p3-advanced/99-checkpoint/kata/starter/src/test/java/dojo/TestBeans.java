package dojo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** A clock the tests move by hand and a sleeper that records the waits instead of sleeping. */
@TestConfiguration
class TestBeans {
    static final class RecordingSleeper implements Sleeper {
        private final List<Duration> waits = new ArrayList<>();

        @Override
        public synchronized void sleep(Duration duration) {
            waits.add(duration);
        }

        synchronized List<Duration> waits() {
            return List.copyOf(waits);
        }
    }

    @Bean
    @Primary
    TestClock testClock() {
        return new TestClock();
    }

    @Bean
    @Primary
    RecordingSleeper recordingSleeper() {
        return new RecordingSleeper();
    }
}
