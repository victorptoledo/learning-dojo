package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AnagramGroupsTest {
    @Test
    void key_sorts_the_lowercased_letters() {
        assertEquals("eilnst", AnagramGroups.key("Listen"));
    }
}
