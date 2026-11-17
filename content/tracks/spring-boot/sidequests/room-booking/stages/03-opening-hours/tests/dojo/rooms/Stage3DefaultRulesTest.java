package dojo.rooms;

import static dojo.rooms.RoomsApi.createRoom;
import static dojo.rooms.RoomsApi.postJson;
import static dojo.rooms.RoomsApi.unique;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** The rules of application.properties, with the real clock. */
@SpringBootTest
@AutoConfigureMockMvc
class Stage3DefaultRulesTest {
    @Autowired
    MockMvc mvc;

    @Test
    void the_rules_come_from_application_properties() throws Exception {
        long room = createRoom(mvc, unique("Defaults"), 6);
        postJson(mvc, "/rooms/" + room + "/bookings", "{\"title\": \"Early bird\", \"start\": \"2099-03-02T07:30\", "
                + "\"end\": \"2099-03-02T08:30\", \"attendees\": 2}")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("the building is open from 08:00 to 20:00"));
        postJson(mvc, "/rooms/" + room + "/bookings", "{\"title\": \"Marathon\", \"start\": \"2099-03-02T09:00\", "
                + "\"end\": \"2099-03-02T13:30\", \"attendees\": 2}")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("a booking lasts at most 240 minutes"));
        postJson(mvc, "/rooms/" + room + "/bookings", "{\"title\": \"Fine\", \"start\": \"2099-03-02T16:00\", "
                + "\"end\": \"2099-03-02T20:00\", \"attendees\": 2}")
                .andExpect(status().isCreated());
    }
}
