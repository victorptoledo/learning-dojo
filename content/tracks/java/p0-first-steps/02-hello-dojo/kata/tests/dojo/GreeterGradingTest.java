package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GreeterGradingTest {
    @Test
    void greets_another_name() {
        assertEquals("Welcome to the dojo, Duke!", Greeter.greet("Duke"));
    }

    @Test
    void ignores_surrounding_spaces() {
        assertEquals("Welcome to the dojo, Ada!", Greeter.greet("  Ada \t"));
    }

    @Test
    void greets_a_stranger_when_the_name_is_empty() {
        assertEquals("Welcome to the dojo, stranger!", Greeter.greet(""));
    }

    @Test
    void greets_a_stranger_when_the_name_is_blank() {
        assertEquals("Welcome to the dojo, stranger!", Greeter.greet("   "));
    }

    @Test
    void greets_a_stranger_when_the_name_is_null() {
        assertEquals("Welcome to the dojo, stranger!", Greeter.greet(null));
    }
}
