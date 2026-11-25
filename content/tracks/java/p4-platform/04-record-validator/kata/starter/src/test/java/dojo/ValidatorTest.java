package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ValidatorTest {
    record Customer(@NotBlank String name) {}

    @Test
    void a_valid_record_has_no_violations() {
        assertEquals(List.of(), Validator.validate(new Customer("Ada")));
    }

    @Test
    void a_blank_name_is_a_violation() {
        assertEquals(List.of(new Violation("name", "must not be blank")), Validator.validate(new Customer(" ")));
    }
}
