package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LogIndexTest {
    @Test
    void the_example_of_the_readme() {
        var log = new LogIndex();
        log.watch("ERROR");
        log.append("09:00 INFO start");
        log.append("09:01 ERROR disk ERROR retry");
        log.append("09:02 INFO ok");
        log.append("09:03 ERROR net");
        log.watch("aa");
        log.append("aaaa");
        assertEquals(3, log.count("ERROR", 0, 5));
        assertEquals(3, log.count("aa", 0, 5));
        assertEquals(1, log.firstLineWith("ERROR", 0));
        assertEquals(3, log.firstLineWith("ERROR", 2));
        assertEquals(1, log.busiestLine("ERROR", 0, 5));
        log.replace(1, "09:01 INFO fixed");
        assertEquals(1, log.count("ERROR", 0, 5));
        assertEquals(3, log.busiestLine("ERROR", 0, 5));
        assertEquals(-1, log.firstLineWith("ERROR", 4));
    }

    @Test
    void questions_need_a_watched_pattern() {
        var log = new LogIndex();
        log.append("x");
        assertThrows(IllegalArgumentException.class, () -> log.count("x", 0, 1));
    }
}
