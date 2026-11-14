package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

class ParallelFetchTest {
    @Test
    void returns_one_success_per_call() {
        Callable<String> hello = () -> "hello";
        assertEquals(List.of(new Outcome.Success<>("hello")), ParallelFetch.fetchAll(List.of(hello), Duration.ofSeconds(1)));
    }
}
