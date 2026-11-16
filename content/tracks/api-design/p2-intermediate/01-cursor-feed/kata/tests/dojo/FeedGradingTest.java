package dojo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class FeedGradingTest {
    static final String TYPES = "https://api.dojo.example/problems/";

    @Autowired
    MockMvc mvc;

    @Autowired
    EventStore store;

    /** The seeded ids (1 to 50) newest first, computed from how EventStore seeds them. */
    static List<Long> seededNewestFirst(String type) {
        return IntStream.rangeClosed(1, 50)
                .filter(i -> type == null || EventStore.TYPES[i % 3].equals(type))
                .boxed()
                .sorted(Comparator.comparingInt((Integer i) -> ((i * 7) % 50) / 2).thenComparingInt(i -> i).reversed())
                .map(Integer::longValue)
                .toList();
    }

    MockHttpServletResponse page(String type, String cursor, int limit) throws Exception {
        // A real query string, as a client would send it (cursors are base64url: no escaping needed).
        StringBuilder url = new StringBuilder("/events?limit=" + limit);
        if (type != null) {
            url.append("&type=").append(type);
        }
        if (cursor != null) {
            url.append("&cursor=").append(cursor);
        }
        return mvc.perform(get(url.toString())).andExpect(status().isOk()).andReturn().getResponse();
    }

    static List<Long> ids(MockHttpServletResponse response) throws Exception {
        List<Number> ids = JsonPath.read(response.getContentAsString(), "$.data[*].id");
        return ids.stream().map(Number::longValue).toList();
    }

    static String next(MockHttpServletResponse response) throws Exception {
        return JsonPath.read(response.getContentAsString(), "$.nextCursor");
    }

    /** Every id of a walk from the first page to the last. */
    List<Long> walk(String type, int limit, Runnable betweenFirstAndSecondPage) throws Exception {
        List<Long> seen = new ArrayList<>();
        MockHttpServletResponse response = page(type, null, limit);
        seen.addAll(ids(response));
        betweenFirstAndSecondPage.run();
        String cursor = next(response);
        int guard = 0;
        while (cursor != null && guard++ < 200) {
            response = page(type, cursor, limit);
            seen.addAll(ids(response));
            cursor = next(response);
        }
        return seen;
    }

    static List<Long> seededOnly(List<Long> ids) {
        return ids.stream().filter(id -> id <= 50).toList();
    }

    static void assertNoDuplicates(List<Long> ids) {
        Set<Long> unique = new HashSet<>(ids);
        assertEquals(unique.size(), ids.size(), "an event came twice: " + ids);
    }

    @Test
    void the_feed_is_newest_first_by_time_then_id() throws Exception {
        List<Long> all = walk(null, 100, () -> {});
        assertEquals(seededNewestFirst(null), seededOnly(all));
    }

    @Test
    void walking_small_pages_visits_every_event_once_in_order() throws Exception {
        List<Long> all = walk("order.paid", 3, () -> {});
        assertNoDuplicates(all);
        assertEquals(seededNewestFirst("order.paid"), seededOnly(all));
    }

    @Test
    void events_that_arrive_between_two_pages_neither_repeat_nor_hide_older_ones() throws Exception {
        List<Long> all = walk(null, 7, () -> {
            store.append("order.created", "order-9001");
            store.append("order.paid", "order-9001");
            store.append("order.created", "order-9002");
        });
        assertNoDuplicates(all);
        assertEquals(seededNewestFirst(null), seededOnly(all));
    }

    @Test
    void the_cursor_is_opaque_and_comes_with_a_next_link() throws Exception {
        MockHttpServletResponse first = page("order.shipped", null, 4);
        String cursor = next(first);
        assertThat(cursor.matches("\\d+"), is(false));
        assertThat(cursor, not(containsString("offset")));
        assertThat(first.getHeader("Link"), containsString("rel=\"next\""));
        assertThat(first.getHeader("Link"), containsString("cursor=" + cursor));
        assertThat(first.getHeader("Link"), containsString("type=order.shipped"));
    }

    @Test
    void the_last_page_has_a_null_cursor_and_no_link() throws Exception {
        MockHttpServletResponse last = page("order.shipped", null, 100);
        assertThat(next(last), nullValue());
        assertThat(last.getHeader("Link"), nullValue());
        mvc.perform(get("/events").param("type", "order.shipped").param("limit", "100"))
                .andExpect(jsonPath("$.nextCursor").value(nullValue()))
                .andExpect(jsonPath("$.data.length()").value(seededNewestFirst("order.shipped").size()));
    }

    ResultActions isProblem(ResultActions actions, String type) throws Exception {
        return actions
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(TYPES + type));
    }

    @Test
    void a_cursor_only_works_with_the_filter_it_was_issued_for() throws Exception {
        String cursor = next(page("order.paid", null, 2));
        isProblem(mvc.perform(get("/events").param("type", "order.shipped").param("cursor", cursor)),
                "invalid-cursor");
        isProblem(mvc.perform(get("/events").param("cursor", cursor)), "invalid-cursor");
        page("order.paid", cursor, 2);
    }

    @Test
    void a_garbage_or_tampered_cursor_is_a_400_problem() throws Exception {
        isProblem(mvc.perform(get("/events").param("cursor", "%%%")), "invalid-cursor");
        isProblem(mvc.perform(get("/events").param("cursor", "aGVsbG8")), "invalid-cursor");
        isProblem(mvc.perform(get("/events").param("cursor", "12")), "invalid-cursor");
    }

    @Test
    void the_limit_is_between_1_and_100_with_a_default_of_20() throws Exception {
        assertEquals(20, ids(page(null, null, 20)).size());
        mvc.perform(get("/events")).andExpect(jsonPath("$.data.length()").value(20));
        isProblem(mvc.perform(get("/events").param("limit", "101")), "invalid-parameter")
                .andExpect(jsonPath("$.parameter").value("limit"));
        isProblem(mvc.perform(get("/events").param("limit", "0")), "invalid-parameter")
                .andExpect(header().doesNotExist("Link"));
    }
}
