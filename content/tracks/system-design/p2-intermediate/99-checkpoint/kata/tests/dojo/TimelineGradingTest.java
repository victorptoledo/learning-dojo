package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimelineGradingTest {
    private final TestClock clock = new TestClock();

    private Post postLater(TimelineService timelines, String author) {
        clock.advance(Duration.ofSeconds(1));
        return timelines.post(author);
    }

    @Test
    void a_normal_post_is_pushed_once_per_follower() {
        var timelines = new TimelineService(10, 100, clock);
        timelines.follow("ada", "linus");
        timelines.follow("grace", "linus");
        Post post = postLater(timelines, "linus");
        assertEquals(2, timelines.inboxWrites());
        assertEquals(List.of(post), timelines.timeline("ada", 10));
        assertEquals(List.of(post), timelines.timeline("grace", 10));
        assertEquals(List.of(), timelines.timeline("linus", 10), "your own posts are not in your home timeline");
    }

    @Test
    void a_celebrity_post_is_pulled_not_pushed() {
        var timelines = new TimelineService(3, 100, clock);
        for (int i = 0; i < 500; i++) {
            timelines.follow("fan" + i, "star");
        }
        Post post = postLater(timelines, "star");
        assertEquals(0, timelines.inboxWrites(), "a celebrity post must not fan out");
        assertEquals(List.of(post), timelines.timeline("fan7", 10));
        assertEquals(List.of(post), timelines.timeline("fan499", 10));
    }

    @Test
    void pushed_and_pulled_posts_are_merged_newest_first() {
        var timelines = new TimelineService(2, 100, clock);
        timelines.follow("ada", "star");
        timelines.follow("bob", "star");
        timelines.follow("ada", "linus");
        Post p1 = postLater(timelines, "linus");
        Post p2 = postLater(timelines, "star");
        Post p3 = postLater(timelines, "linus");
        Post p4 = postLater(timelines, "star");
        assertEquals(List.of(p4, p3, p2, p1), timelines.timeline("ada", 10));
        assertEquals(List.of(p4, p3), timelines.timeline("ada", 2));
        assertEquals(List.of(), timelines.timeline("ada", 0));
    }

    @Test
    void posts_at_the_same_instant_are_ordered_by_id() {
        var timelines = new TimelineService(2, 100, clock);
        timelines.follow("ada", "linus");
        timelines.follow("ada", "star");
        timelines.follow("bob", "star");
        Post a = timelines.post("linus");
        Post b = timelines.post("star");
        Post c = timelines.post("linus");
        assertEquals(List.of(c, b, a), timelines.timeline("ada", 10));
    }

    @Test
    void unfollowing_hides_pushed_and_pulled_posts_at_read_time() {
        var timelines = new TimelineService(2, 100, clock);
        timelines.follow("ada", "linus");
        timelines.follow("ada", "star");
        timelines.follow("bob", "star");
        postLater(timelines, "linus");
        postLater(timelines, "star");
        long writes = timelines.inboxWrites();
        timelines.unfollow("ada", "linus");
        timelines.unfollow("ada", "star");
        assertEquals(List.of(), timelines.timeline("ada", 10));
        assertEquals(writes, timelines.inboxWrites(), "unfollowing rewrites nothing");
        timelines.follow("ada", "linus");
        assertEquals(1, timelines.timeline("ada", 10).size(), "following again shows what is still in the inbox");
    }

    @Test
    void inboxes_keep_only_the_newest_posts() {
        var timelines = new TimelineService(10, 3, clock);
        timelines.follow("ada", "linus");
        for (int i = 0; i < 5; i++) {
            postLater(timelines, "linus");
        }
        var ids = timelines.timeline("ada", 10).stream().map(Post::id).toList();
        assertEquals(List.of(5L, 4L, 3L), ids);
    }

    @Test
    void an_author_crossing_the_threshold_appears_once_per_post() {
        var timelines = new TimelineService(3, 100, clock);
        timelines.follow("ada", "rising");
        timelines.follow("bob", "rising");
        Post before = postLater(timelines, "rising");
        timelines.follow("cid", "rising");
        Post after = postLater(timelines, "rising");
        assertEquals(2, timelines.inboxWrites(), "only the first post was pushed");
        assertEquals(List.of(after, before), timelines.timeline("ada", 10));
        assertEquals(List.of(after), timelines.timeline("cid", 10), "cid followed after the pushed post");
        timelines.unfollow("cid", "rising");
        timelines.unfollow("bob", "rising");
        Post later = postLater(timelines, "rising");
        assertEquals(List.of(later, after, before), timelines.timeline("ada", 10),
                "celebrity posts stay visible after the author drops below the threshold");
        var ids = new HashSet<Long>();
        timelines.timeline("ada", 10).forEach(p -> assertTrue(ids.add(p.id()), "duplicate post " + p.id()));
    }

    @Test
    void following_does_not_backfill_pushed_posts_but_shows_pulled_ones() {
        var timelines = new TimelineService(2, 100, clock);
        postLater(timelines, "quiet");
        timelines.follow("x", "star");
        timelines.follow("y", "star");
        Post starPost = postLater(timelines, "star");
        timelines.follow("ada", "quiet");
        timelines.follow("ada", "star");
        assertEquals(List.of(starPost), timelines.timeline("ada", 10));
    }

    @Test
    void the_pull_reads_only_the_newest_posts_it_needs() {
        var timelines = new TimelineService(1, 100, clock);
        timelines.follow("ada", "star");
        for (int i = 0; i < 50; i++) {
            postLater(timelines, "star");
        }
        var ids = timelines.timeline("ada", 3).stream().map(Post::id).toList();
        assertEquals(List.of(50L, 49L, 48L), ids);
    }

    @Test
    void invalid_calls_are_refused() {
        assertThrows(IllegalArgumentException.class, () -> new TimelineService(0, 10, clock));
        assertThrows(IllegalArgumentException.class, () -> new TimelineService(10, 0, clock));
        var timelines = new TimelineService(10, 10, clock);
        assertThrows(IllegalArgumentException.class, () -> timelines.follow("ada", "ada"));
        assertThrows(IllegalArgumentException.class, () -> timelines.timeline("ada", -1));
        timelines.follow("ada", "linus");
        timelines.follow("ada", "linus");
        assertEquals(1, timelines.followers("linus"));
        timelines.unfollow("ada", "nobody");
        assertEquals(0, timelines.followers("nobody"));
    }
}
