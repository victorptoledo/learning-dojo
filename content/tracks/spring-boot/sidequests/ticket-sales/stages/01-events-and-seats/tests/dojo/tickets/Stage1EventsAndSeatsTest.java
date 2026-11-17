package dojo.tickets;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class Stage1EventsAndSeatsTest extends TicketsContractTest {

    static String eventJson(String name, String startsAt, int rows, int seatsPerRow, long price) {
        return "{\"name\": \"" + name + "\", \"startsAt\": \"" + startsAt + "\", \"rows\": " + rows
                + ", \"seatsPerRow\": " + seatsPerRow + ", \"priceCents\": " + price + "}";
    }

    @Test
    void an_organizer_puts_an_event_on_sale() throws Exception {
        String location = createEvent(BOSS, eventJson("Opening night", STARTS, 3, 4, 4500))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Opening night"))
                .andExpect(jsonPath("$.priceCents").value(4500))
                .andExpect(jsonPath("$.seats").value(12))
                .andExpect(jsonPath("$.available").value(12))
                .andReturn().getResponse().getHeader("Location");
        assertTrue(location != null && location.matches(".*/events/\\d+"), "Location was " + location);
        String startsAt = JsonPath.read(
                mvc.perform(get(location).with(ADA)).andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), "$.startsAt");
        assertTrue(Instant.parse(startsAt).equals(Instant.parse(STARTS)), "startsAt was " + startsAt);
    }

    @Test
    void the_seats_come_row_by_row() throws Exception {
        long id = event(2, 3, 1000);
        mvc.perform(get("/events/" + id + "/seats").with(LINUS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].seat", contains("A1", "A2", "A3", "B1", "B2", "B3")))
                .andExpect(jsonPath("$[*].status", everyItem(is("AVAILABLE"))));
        long big = event(26, 50, 1000);
        mvc.perform(get("/events/" + big + "/seats").with(LINUS))
                .andExpect(jsonPath("$", hasSize(1300)))
                .andExpect(jsonPath("$[1299].seat").value("Z50"));
    }

    @Test
    void a_bad_event_is_400() throws Exception {
        createEvent(BOSS, "{\"name\": \" \", \"rows\": 0, \"seatsPerRow\": 51, \"priceCents\": -1}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.name").isString())
                .andExpect(jsonPath("$.errors.startsAt").isString())
                .andExpect(jsonPath("$.errors.rows").isString())
                .andExpect(jsonPath("$.errors.seatsPerRow").isString())
                .andExpect(jsonPath("$.errors.priceCents").isString());
        createEvent(BOSS, eventJson("Too many rows", STARTS, 27, 1, 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rows").isString());
    }

    @Test
    void an_event_starts_in_the_future_of_the_application_clock() throws Exception {
        clock.advance(Duration.ofSeconds(1));
        String past = Instant.parse("2099-01-01T09:00:00Z").toString();
        createEvent(BOSS, eventJson("Yesterday", past, 1, 1, 0))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("an event must start in the future"));
    }

    @Test
    void only_organizers_create_events() throws Exception {
        createEvent(ADA, eventJson("Mine", STARTS, 1, 1, 0)).andExpect(status().isForbidden());
        mvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON).content(eventJson("Anyone", STARTS, 1, 1, 0)))
                .andExpect(status().isUnauthorized());
        long id = event(1, 1, 0);
        mvc.perform(get("/events/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/events/" + id).with(GRACE)).andExpect(status().isOk());
    }

    @Test
    void an_unknown_event_is_404() throws Exception {
        mvc.perform(get("/events/987654").with(ADA))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("event 987654 not found"));
        mvc.perform(get("/events/987654/seats").with(ADA)).andExpect(status().isNotFound());
    }
}
