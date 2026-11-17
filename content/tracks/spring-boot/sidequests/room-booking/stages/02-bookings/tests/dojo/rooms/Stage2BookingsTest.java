package dojo.rooms;

import static dojo.rooms.RoomsApi.createRoom;
import static dojo.rooms.RoomsApi.idOf;
import static dojo.rooms.RoomsApi.postJson;
import static dojo.rooms.RoomsApi.unique;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class Stage2BookingsTest {
    static final String DAY = "2099-03-02";

    @Autowired
    MockMvc mvc;

    ResultActions book(long room, String title, String from, String to, int attendees) throws Exception {
        String json = "{\"title\": \"" + title + "\", \"start\": \"" + DAY + "T" + from + "\", \"end\": \""
                + DAY + "T" + to + "\", \"attendees\": " + attendees + "}";
        return postJson(mvc, "/rooms/" + room + "/bookings", json);
    }

    long booked(long room, String title, String from, String to) throws Exception {
        return idOf(book(room, title, from, to, 2).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    @Test
    void a_booking_answers_201_with_its_location() throws Exception {
        long room = createRoom(mvc, unique("Ada"), 8);
        String location = book(room, "Standup", "09:00", "09:30", 5)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.roomId").value(room))
                .andExpect(jsonPath("$.title").value("Standup"))
                .andExpect(jsonPath("$.start", startsWith(DAY + "T09:00")))
                .andExpect(jsonPath("$.end", startsWith(DAY + "T09:30")))
                .andExpect(jsonPath("$.attendees").value(5))
                .andReturn().getResponse().getHeader("Location");
        assertTrue(location != null && location.matches(".*/bookings/\\d+"), "Location was " + location);
        mvc.perform(get(location)).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Standup"));
    }

    @Test
    void a_bad_booking_is_400_with_the_bad_fields() throws Exception {
        long room = createRoom(mvc, unique("Bad"), 8);
        postJson(mvc, "/rooms/" + room + "/bookings", "{\"title\": \" \", \"attendees\": 0}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.title").isString())
                .andExpect(jsonPath("$.errors.start").isString())
                .andExpect(jsonPath("$.errors.end").isString())
                .andExpect(jsonPath("$.errors.attendees").isString());
        book(room, "Backwards", "10:00", "09:00", 2)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.end").isString())
                .andExpect(jsonPath("$.errors.title").doesNotExist());
        book(room, "Empty", "10:00", "10:00", 2)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.end").isString());
    }

    @Test
    void the_room_must_exist_and_fit_everyone() throws Exception {
        book(987654, "Nowhere", "09:00", "10:00", 2)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("room 987654 not found"));
        String name = unique("Small");
        long room = createRoom(mvc, name, 4);
        book(room, "Crowd", "09:00", "10:00", 5)
                .andExpect(status().is(422))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("room " + name + " holds 4 people"));
        book(room, "Just right", "09:00", "10:00", 4).andExpect(status().isCreated());
    }

    @Test
    void an_overlap_is_409_and_names_the_other_booking() throws Exception {
        long room = createRoom(mvc, unique("Busy"), 8);
        long first = booked(room, "Planning", "10:00", "11:00");
        for (String[] slot : List.of(
                new String[] {"10:00", "11:00"},
                new String[] {"09:30", "10:01"},
                new String[] {"10:59", "12:00"},
                new String[] {"10:15", "10:45"},
                new String[] {"09:00", "12:00"})) {
            book(room, "Clash", slot[0], slot[1], 2)
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.conflictsWith").value(first));
        }
        booked(room, "Before", "09:00", "10:00");
        booked(room, "After", "11:00", "12:00");
        long other = createRoom(mvc, unique("Quiet"), 8);
        booked(other, "Same time, other room", "10:00", "11:00");
    }

    @Test
    void a_room_lists_its_bookings_by_start_and_by_day() throws Exception {
        long room = createRoom(mvc, unique("Agenda"), 8);
        booked(room, "Late", "15:00", "16:00");
        booked(room, "Early", "08:30", "09:00");
        String nextDay = "{\"title\": \"Tomorrow\", \"start\": \"2099-03-03T09:00\", \"end\": \"2099-03-03T10:00\", "
                + "\"attendees\": 2}";
        postJson(mvc, "/rooms/" + room + "/bookings", nextDay).andExpect(status().isCreated());

        mvc.perform(get("/rooms/" + room + "/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", contains("Early", "Late", "Tomorrow")));
        mvc.perform(get("/rooms/" + room + "/bookings").param("date", DAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", contains("Early", "Late")));
        mvc.perform(get("/rooms/987654/bookings")).andExpect(status().isNotFound());
    }

    @Test
    void a_cancelled_booking_frees_its_time() throws Exception {
        long room = createRoom(mvc, unique("Free"), 8);
        long id = booked(room, "Maybe", "13:00", "14:00");
        mvc.perform(delete("/bookings/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/bookings/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("booking " + id + " not found"));
        mvc.perform(delete("/bookings/" + id)).andExpect(status().isNotFound());
        booked(room, "Surely", "13:00", "14:00");
    }

    @Test
    void a_room_with_bookings_cannot_be_deleted() throws Exception {
        long room = createRoom(mvc, unique("Kept"), 8);
        long id = booked(room, "Review", "16:00", "17:00");
        mvc.perform(delete("/rooms/" + room)).andExpect(status().isConflict());
        mvc.perform(delete("/bookings/" + id)).andExpect(status().isNoContent());
        mvc.perform(delete("/rooms/" + room)).andExpect(status().isNoContent());
    }

    @Test
    void only_one_of_many_requests_for_the_same_slot_wins() throws Exception {
        long room = createRoom(mvc, unique("Race"), 8);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Integer>> calls = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                calls.add(() -> book(room, "Race", "11:00", "12:00", 2).andReturn().getResponse().getStatus());
            }
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : pool.invokeAll(calls)) {
                statuses.add(future.get());
            }
            assertEquals(1, statuses.stream().filter(s -> s == 201).count(), "statuses: " + statuses);
            assertEquals(15, statuses.stream().filter(s -> s == 409).count(), "statuses: " + statuses);
        } finally {
            pool.shutdownNow();
        }
    }
}
