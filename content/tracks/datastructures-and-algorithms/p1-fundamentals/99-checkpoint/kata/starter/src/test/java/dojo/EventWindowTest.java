package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class EventWindowTest {
    @Test
    void the_example_of_the_readme() {
        var window = new EventWindow(1_000);
        window.record(100, "ada", 30);
        window.record(400, "bob", 90);
        window.record(900, "ada", 50);
        assertEquals(3, window.count());
        assertEquals(2, window.distinctUsers());
        assertEquals(OptionalInt.of(90), window.maxLatency());
        window.advanceTo(1_400);
        assertEquals(1, window.count());
        assertEquals(OptionalInt.of(50), window.maxLatency());
    }
}
