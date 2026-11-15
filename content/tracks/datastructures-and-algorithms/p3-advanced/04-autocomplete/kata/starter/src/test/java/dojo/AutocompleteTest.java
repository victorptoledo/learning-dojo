package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AutocompleteTest {
    @Test
    void the_example_of_the_readme() {
        var ac = new Autocomplete();
        ac.add("card");
        ac.add("care", 3);
        ac.add("cat", 5);
        ac.add("car", 3);
        assertEquals(3, ac.countWithPrefix("car"));
        assertEquals(List.of("cat", "car", "care"), ac.suggest("ca", 3));
        assertTrue(ac.remove("card"));
        assertEquals(5, ac.nodeCount());
    }
}
