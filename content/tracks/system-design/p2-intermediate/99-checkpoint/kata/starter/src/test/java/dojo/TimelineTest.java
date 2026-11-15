package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TimelineTest {
    @Test
    void a_follower_sees_a_new_post() {
        var timelines = new TimelineService(1000, 800, new TestClock());
        timelines.follow("ada", "linus");
        Post post = timelines.post("linus");
        assertEquals(List.of(post), timelines.timeline("ada", 20));
    }
}
