package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GreeterTest {
    @Test
    void greets_by_name() {
        assertEquals("Welcome to the dojo, Ada!", Greeter.greet("Ada"));
    }
}
