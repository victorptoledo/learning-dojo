package dojo;

import static dojo.GradingSupport.allClasses;
import static dojo.GradingSupport.bodyLengths;
import static dojo.GradingSupport.code;
import static dojo.GradingSupport.count;
import static dojo.GradingSupport.instanceFieldTypes;
import static dojo.GradingSupport.rawSource;
import static dojo.GradingSupport.type;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DispatcherGradingTest {
    private final Dispatcher dispatcher = new Dispatcher();

    private static String repeat(char c, int times) {
        return String.valueOf(c).repeat(times);
    }

    // ---- behaviour ------------------------------------------------------------------

    @Test
    void email_is_sent_at_any_hour_and_costs_nothing() {
        for (int hour : new int[] {0, 6, 7, 21, 22, 23}) {
            assertTrue(dispatcher.send("email", "ana", "hello " + hour, hour), "hour " + hour);
        }
        assertEquals(6, dispatcher.sent().size());
        assertEquals("EMAIL ana: hello 0", dispatcher.sent().get(0));
        assertEquals(0, dispatcher.costCents());
        assertEquals(List.of(), dispatcher.held());
    }

    @Test
    void sms_is_cut_at_160_characters_and_costs_five_cents() {
        String exact = repeat('a', 160);
        String longer = repeat('b', 161);
        assertTrue(dispatcher.send("sms", "bo", exact, 12));
        assertTrue(dispatcher.send("sms", "bo", longer, 12));
        assertEquals("SMS bo: " + exact, dispatcher.sent().get(0), "160 characters fit");
        assertEquals("SMS bo: " + repeat('b', 157) + "...", dispatcher.sent().get(1));
        assertEquals(10, dispatcher.costCents());
    }

    @Test
    void push_keeps_the_first_40_characters_and_costs_one_cent() {
        assertTrue(dispatcher.send("push", "cy", repeat('p', 45), 9));
        assertTrue(dispatcher.send("push", "cy", "short", 9));
        assertEquals(List.of("PUSH cy: " + repeat('p', 40), "PUSH cy: short"), dispatcher.sent());
        assertEquals(2, dispatcher.costCents());
    }

    @Test
    void quiet_hours_run_from_22_to_7() {
        assertTrue(dispatcher.send("sms", "d", "at 21", 21));
        assertFalse(dispatcher.send("sms", "d", "at 22", 22));
        assertFalse(dispatcher.send("push", "d", "at 0", 0));
        assertFalse(dispatcher.send("push", "d", "at 6", 6));
        assertTrue(dispatcher.send("push", "d", "at 7", 7));
        assertEquals(List.of("SMS d: at 21", "PUSH d: at 7"), dispatcher.sent());
        assertEquals(List.of("SMS d: at 22", "PUSH d: at 0", "PUSH d: at 6"), dispatcher.held());
        assertEquals(6, dispatcher.costCents(), "held messages cost nothing until they are sent");
    }

    @Test
    void held_messages_go_out_in_order_once_quiet_hours_end() {
        dispatcher.send("push", "e", "first", 23);
        dispatcher.send("sms", "e", "second", 23);
        dispatcher.send("email", "e", "third", 23);
        assertEquals(0, dispatcher.releaseHeld(23));
        assertEquals(0, dispatcher.releaseHeld(6));
        assertEquals(List.of("PUSH e: first", "SMS e: second"), dispatcher.held());
        assertEquals(2, dispatcher.releaseHeld(7));
        assertEquals(List.of("EMAIL e: third", "PUSH e: first", "SMS e: second"), dispatcher.sent());
        assertEquals(List.of(), dispatcher.held());
        assertEquals(6, dispatcher.costCents());
        assertEquals(0, dispatcher.releaseHeld(12), "nothing left to release");
    }

    @Test
    void bad_input_is_refused_with_its_message() {
        assertEquals("unknown channel: fax",
                assertThrows(IllegalArgumentException.class, () -> dispatcher.send("fax", "f", "hi", 10)).getMessage());
        assertEquals("unknown channel: SMS",
                assertThrows(IllegalArgumentException.class, () -> dispatcher.send("SMS", "f", "hi", 10)).getMessage(),
                "channel names are exact");
        assertEquals("text is required",
                assertThrows(IllegalArgumentException.class, () -> dispatcher.send("sms", "f", "   ", 10)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> dispatcher.send("email", "f", null, 10));
        assertEquals("hour must be 0-23",
                assertThrows(IllegalArgumentException.class, () -> dispatcher.send("email", "f", "hi", 24)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> dispatcher.send("email", "f", "hi", -1));
        assertThrows(IllegalArgumentException.class, () -> dispatcher.releaseHeld(24));
        assertEquals(List.of(), dispatcher.sent(), "nothing is sent on a refusal");
    }

    @Test
    void the_lists_handed_out_are_copies() {
        dispatcher.send("email", "g", "one", 10);
        dispatcher.send("sms", "g", "two", 23);
        List<String> sent = new ArrayList<>(dispatcher.sent());
        try {
            dispatcher.sent().clear();
            dispatcher.held().clear();
        } catch (UnsupportedOperationException readOnly) {
            // an unmodifiable copy is fine too
        }
        assertEquals(sent, dispatcher.sent());
        assertEquals(List.of("SMS g: two"), dispatcher.held());
    }

    // ---- the sealed channel ---------------------------------------------------------

    @Test
    void channel_is_a_sealed_type_with_three_records() {
        assertTrue(Channel.class.isSealed(), "Channel should be a sealed interface");
        Class<?>[] permitted = Channel.class.getPermittedSubclasses();
        assertEquals(3, permitted.length, "email, sms and push");
        for (Class<?> channel : permitted) {
            assertTrue(channel.isRecord(), channel.getSimpleName() + " should be a record");
        }
    }

    @Test
    void each_channel_owns_its_rules() {
        Channel email = Channel.parse("email");
        Channel sms = Channel.parse("sms");
        Channel push = Channel.parse("push");
        assertEquals(List.of(0L, 5L, 1L), List.of(email.costCents(), sms.costCents(), push.costCents()));
        assertEquals(List.of(false, true, true),
                List.of(email.waitsForQuietHours(), sms.waitsForQuietHours(), push.waitsForQuietHours()));
        assertEquals("EMAIL h: hi", email.format("h", "hi"));
        assertEquals("PUSH h: " + repeat('x', 40), push.format("h", repeat('x', 41)));
        assertEquals("unknown channel: pigeon",
                assertThrows(IllegalArgumentException.class, () -> Channel.parse("pigeon")).getMessage());
    }

    // ---- structure ------------------------------------------------------------------

    private static List<Class<?>> everyType() {
        List<Class<?>> types = new ArrayList<>();
        for (String name : allClasses()) {
            collect(type(name), types);
        }
        return types;
    }

    private static void collect(Class<?> type, List<Class<?>> into) {
        into.add(type);
        for (Class<?> nested : type.getDeclaredClasses()) {
            collect(nested, into);
        }
    }

    @Test
    void the_needless_indirection_is_gone() {
        for (Class<?> type : everyType()) {
            String name = type.getSimpleName();
            for (String suffix : List.of("Factory", "Provider", "Strategy")) {
                assertFalse(name.endsWith(suffix), "remove " + type.getName() + ": one sealed type replaces the chain");
            }
            for (Field field : type.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                assertFalse(Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers),
                        type.getName() + "." + field.getName() + " is global mutable state");
            }
        }
        for (String name : allClasses()) {
            assertFalse(code(name).contains("getInstance"), name + " still has a singleton");
        }
    }

    @Test
    void each_channel_name_is_written_once() {
        StringBuilder all = new StringBuilder();
        for (String name : allClasses()) {
            all.append(rawSource(name)).append('\n');
        }
        for (String channel : List.of("email", "sms", "push")) {
            assertTrue(count(all.toString(), "\"" + channel + "\"") <= 1,
                    "the literal \"" + channel + "\" should appear at most once (in Channel.parse)");
        }
    }

    @Test
    void methods_are_short_and_the_dispatcher_is_small() {
        for (String name : allClasses()) {
            for (int length : bodyLengths(code(name))) {
                assertTrue(length <= 15, name + " has a method of " + length + " lines; keep each under 16");
            }
        }
        String dispatcher = code("dojo.Dispatcher");
        long lines = dispatcher.lines().filter(line -> !line.isBlank()).count();
        assertTrue(lines <= 70, "Dispatcher has " + lines + " non-blank lines; keep it to 70");
        assertFalse(dispatcher.contains("switch"), "Dispatcher should not switch on the channel");
        assertEquals(0, count(dispatcher, "\\.equals\\(\"\"\\)"), "Dispatcher should not compare channel names");
        long lists = instanceFieldTypes(Dispatcher.class).stream().filter(List.class::isAssignableFrom).count();
        assertTrue(lists <= 2, "no parallel lists: keep each held message with its channel");
    }
}
