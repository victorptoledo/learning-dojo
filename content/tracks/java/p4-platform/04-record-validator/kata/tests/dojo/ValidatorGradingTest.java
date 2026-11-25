package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidatorGradingTest {
    record Address(@NotBlank String city, @MaxLength(8) String postcode) {}

    record Line(@NotBlank String sku, @Range(min = 1, max = 99) int quantity) {}

    record Order(@NotBlank @MaxLength(5) String id, @Range(min = 0, max = 1_000_000) long cents,
            @Range(min = 1, max = 10) Integer priority, @Valid Address address, @Valid List<Line> lines,
            String note) {}

    record Plain(String anything, int number) {}

    record WrongRange(@Range(min = 1) String name) {}

    record WrongBlank(@NotBlank int count) {}

    record WrongValid(@Valid String text) {}

    record Grumpy(String name) {
        @Override
        public String name() {
            throw new IllegalStateException("name is not ready");
        }
    }

    private static Order order(String id, long cents, Integer priority, Address address, List<Line> lines) {
        return new Order(id, cents, priority, address, lines, null);
    }

    private static final Address LISBON = new Address("Lisbon", "1000-001");

    @Test
    void a_valid_order_has_no_violations() {
        assertEquals(List.of(), Validator.validate(order("A1", 500, 3, LISBON, List.of(new Line("ABC", 2)))));
        assertEquals(List.of(), Validator.validate(new Plain(null, -5)));
    }

    @Test
    void not_blank_refuses_null_empty_and_spaces() {
        for (String bad : Arrays.asList(null, "", "   ")) {
            assertEquals(List.of(new Violation("city", "must not be blank")),
                    Validator.validate(new Address(bad, null)), "city = '" + bad + "'");
        }
    }

    @Test
    void max_length_counts_characters_and_allows_null() {
        assertEquals(List.of(new Violation("postcode", "must be at most 8 characters")),
                Validator.validate(new Address("Porto", "4000-0001")));
        assertEquals(List.of(), Validator.validate(new Address("Porto", "4000-000")));
        assertEquals(List.of(), Validator.validate(new Address("Porto", null)));
    }

    @Test
    void range_works_on_primitives_and_boxes() {
        var violations = Validator.validate(order("A1", 2_000_000, 11, LISBON, List.of()));
        assertEquals(List.of(
                new Violation("cents", "must be between 0 and 1000000"),
                new Violation("priority", "must be between 1 and 10")), violations,
                "is @Range ignored? Look at how it is declared");
        assertEquals(List.of(), Validator.validate(order("A1", 0, null, LISBON, List.of())), "null skips @Range");
        assertEquals(List.of(), Validator.validate(order("A1", 1_000_000, 10, LISBON, List.of())), "the limits are allowed");
    }

    @Test
    void violations_follow_component_order_and_rule_order() {
        var violations = Validator.validate(order("   ", -1, 0, LISBON, List.of()));
        assertEquals(List.of(
                new Violation("id", "must not be blank"),
                new Violation("cents", "must be between 0 and 1000000"),
                new Violation("priority", "must be between 1 and 10")), violations);
        assertEquals(List.of(new Violation("id", "must not be blank"), new Violation("id", "must be at most 5 characters")),
                Validator.validate(order("       ", 0, 1, LISBON, List.of())), "@NotBlank, then @MaxLength");
    }

    @Test
    void valid_goes_into_records_and_lists_with_paths() {
        var violations = Validator.validate(order("A1", 5, 1, new Address("", "12345678901"),
                List.of(new Line("ABC", 1), new Line(" ", 100))));
        assertEquals(List.of(
                new Violation("address.city", "must not be blank"),
                new Violation("address.postcode", "must be at most 8 characters"),
                new Violation("lines[1].sku", "must not be blank"),
                new Violation("lines[1].quantity", "must be between 1 and 99")), violations);
        assertEquals(List.of(), Validator.validate(order("A1", 5, 1, null, null)), "a null @Valid component is skipped");
    }

    @Test
    void misplaced_annotations_fail_even_when_the_value_is_null() {
        var range = assertThrows(IllegalStateException.class, () -> Validator.validate(new WrongRange(null)));
        assertTrue(range.getMessage().contains("WrongRange.name"), "name the class and component: " + range.getMessage());
        var blank = assertThrows(IllegalStateException.class, () -> Validator.validate(new WrongBlank(1)));
        assertTrue(blank.getMessage().contains("WrongBlank.count"), blank.getMessage());
        var valid = assertThrows(IllegalStateException.class, () -> Validator.validate(new WrongValid("x")));
        assertTrue(valid.getMessage().contains("WrongValid.text"), valid.getMessage());
    }

    @Test
    void an_accessor_failure_comes_out_unwrapped() {
        var e = assertThrows(IllegalStateException.class, () -> Validator.validate(new Grumpy("x")));
        assertEquals("name is not ready", e.getMessage(), "unwrap InvocationTargetException");
        assertSame(IllegalStateException.class, e.getClass());
    }

    @Test
    void refuses_null_and_objects_that_are_not_records() {
        assertThrows(IllegalArgumentException.class, () -> Validator.validate(null));
        assertThrows(IllegalArgumentException.class, () -> Validator.validate("not a record"));
    }

    @Test
    void the_result_cannot_be_changed() {
        var violations = Validator.validate(new Address("", null));
        assertThrows(UnsupportedOperationException.class, () -> violations.add(new Violation("x", "y")));
    }
}
