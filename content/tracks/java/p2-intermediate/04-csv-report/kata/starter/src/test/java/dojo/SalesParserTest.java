package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class SalesParserTest {
    @Test
    void parses_one_line() {
        assertEquals(
                List.of(new Sale(LocalDate.of(2026, 11, 1), "north", "tea", 3, 250)),
                SalesParser.parse("2026-11-01,north,tea,3,250"));
    }
}
