package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Characterisation tests: they pin today's behaviour of Dispatcher, so they pass before and
 * after the refactoring. Keep them green after every small step.
 */
class DispatcherTest {
    private final Dispatcher dispatcher = new Dispatcher();

    @Test
    void email_goes_out_at_any_hour_for_free() {
        assertTrue(dispatcher.send("email", "ana", "Your invoice is ready", 23));
        assertEquals(List.of("EMAIL ana: Your invoice is ready"), dispatcher.sent());
        assertEquals(0, dispatcher.costCents());
    }

    @Test
    void sms_and_push_wait_for_the_morning() {
        assertFalse(dispatcher.send("sms", "bo", "Code 1234", 23));
        assertFalse(dispatcher.send("push", "bo", "New message", 6));
        assertEquals(List.of("SMS bo: Code 1234", "PUSH bo: New message"), dispatcher.held());
        assertEquals(0, dispatcher.releaseHeld(5));
        assertEquals(2, dispatcher.releaseHeld(9));
        assertEquals(List.of("SMS bo: Code 1234", "PUSH bo: New message"), dispatcher.sent());
        assertEquals(6, dispatcher.costCents());
    }

    @Test
    void unknown_channels_are_refused() {
        var error = assertThrows(IllegalArgumentException.class, () -> dispatcher.send("fax", "cy", "hi", 10));
        assertEquals("unknown channel: fax", error.getMessage());
    }
}
