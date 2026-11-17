package dojo.rooms;

import static dojo.rooms.RoomsApi.createRoom;
import static dojo.rooms.RoomsApi.postJson;
import static dojo.rooms.RoomsApi.unique;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Other rules than the ones in application.properties, and a clock fixed on 2099-03-01 at noon. */
@SpringBootTest(properties = {"booking.opens=07:00", "booking.closes=19:00", "booking.max-duration=PT2H"})
@AutoConfigureMockMvc
class Stage3OpeningHoursTest {

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(Instant.parse("2099-03-01T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    MockMvc mvc;

    ResultActions book(long room, String start, String end) throws Exception {
        return postJson(mvc, "/rooms/" + room + "/bookings", "{\"title\": \"Meeting\", \"start\": \"" + start
                + "\", \"end\": \"" + end + "\", \"attendees\": 2}");
    }

    void refused(ResultActions result, String detail) throws Exception {
        result.andExpect(status().is(422))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(detail));
    }

    @Test
    void bookings_cannot_start_in_the_past() throws Exception {
        long room = createRoom(mvc, unique("Past"), 6);
        refused(book(room, "2099-03-01T11:00", "2099-03-01T11:30"), "a booking cannot start in the past");
        refused(book(room, "2099-02-27T09:00", "2099-02-27T10:00"), "a booking cannot start in the past");
        book(room, "2099-03-01T12:30", "2099-03-01T13:00").andExpect(status().isCreated());
    }

    @Test
    void bookings_stay_inside_the_configured_opening_hours() throws Exception {
        long room = createRoom(mvc, unique("Hours"), 6);
        String detail = "the building is open from 07:00 to 19:00";
        refused(book(room, "2099-03-02T06:30", "2099-03-02T07:30"), detail);
        refused(book(room, "2099-03-02T18:00", "2099-03-02T19:01"), detail);
        refused(book(room, "2099-03-02T18:30", "2099-03-03T07:30"), detail);
        book(room, "2099-03-02T07:00", "2099-03-02T08:00").andExpect(status().isCreated());
        book(room, "2099-03-02T18:00", "2099-03-02T19:00").andExpect(status().isCreated());
    }

    @Test
    void bookings_last_at_most_the_configured_duration() throws Exception {
        long room = createRoom(mvc, unique("Long"), 6);
        refused(book(room, "2099-03-02T09:00", "2099-03-02T11:01"), "a booking lasts at most 120 minutes");
        book(room, "2099-03-02T09:00", "2099-03-02T11:00").andExpect(status().isCreated());
    }

    @Test
    void the_rules_come_before_capacity_and_overlaps() throws Exception {
        long room = createRoom(mvc, unique("Order"), 2);
        book(room, "2099-03-02T09:00", "2099-03-02T10:00").andExpect(status().isCreated());
        refused(postJson(mvc, "/rooms/" + room + "/bookings", "{\"title\": \"Crowd\", \"start\": "
                + "\"2099-03-02T06:00\", \"end\": \"2099-03-02T10:00\", \"attendees\": 9}"),
                "the building is open from 07:00 to 19:00");
    }

    @Test
    void a_free_day_is_one_slot_from_opening_to_closing() throws Exception {
        long room = createRoom(mvc, unique("Empty"), 6);
        mvc.perform(get("/rooms/" + room + "/availability").param("date", "2099-03-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].start", startsWith("2099-03-05T07:00")))
                .andExpect(jsonPath("$[0].end", startsWith("2099-03-05T19:00")));
    }

    @Test
    void free_slots_are_the_gaps_between_bookings() throws Exception {
        long room = createRoom(mvc, unique("Gaps"), 6);
        book(room, "2099-03-02T12:00", "2099-03-02T13:00").andExpect(status().isCreated());
        book(room, "2099-03-02T09:00", "2099-03-02T10:00").andExpect(status().isCreated());
        book(room, "2099-03-02T10:00", "2099-03-02T11:30").andExpect(status().isCreated());
        book(room, "2099-03-02T17:00", "2099-03-02T19:00").andExpect(status().isCreated());
        book(room, "2099-03-03T09:00", "2099-03-03T10:00").andExpect(status().isCreated());
        long other = createRoom(mvc, unique("Elsewhere"), 6);
        book(other, "2099-03-02T07:00", "2099-03-02T09:00").andExpect(status().isCreated());

        mvc.perform(get("/rooms/" + room + "/availability").param("date", "2099-03-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].start", startsWith("2099-03-02T07:00")))
                .andExpect(jsonPath("$[0].end", startsWith("2099-03-02T09:00")))
                .andExpect(jsonPath("$[1].start", startsWith("2099-03-02T11:30")))
                .andExpect(jsonPath("$[1].end", startsWith("2099-03-02T12:00")))
                .andExpect(jsonPath("$[2].start", startsWith("2099-03-02T13:00")))
                .andExpect(jsonPath("$[2].end", startsWith("2099-03-02T17:00")));
    }

    @Test
    void a_full_day_has_no_free_slot() throws Exception {
        long room = createRoom(mvc, unique("Full"), 6);
        for (String[] slot : new String[][] {{"07:00", "09:00"}, {"09:00", "11:00"}, {"11:00", "13:00"},
                {"13:00", "15:00"}, {"15:00", "17:00"}, {"17:00", "19:00"}}) {
            book(room, "2099-03-04T" + slot[0], "2099-03-04T" + slot[1]).andExpect(status().isCreated());
        }
        mvc.perform(get("/rooms/" + room + "/availability").param("date", "2099-03-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void availability_needs_a_date_and_a_room() throws Exception {
        long room = createRoom(mvc, unique("Ask"), 6);
        mvc.perform(get("/rooms/" + room + "/availability"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(get("/rooms/987654/availability").param("date", "2099-03-02"))
                .andExpect(status().isNotFound());
    }
}
