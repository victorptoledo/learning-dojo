package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OutboxTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    FakeBroker broker;

    @Autowired
    OutboxRelay relay;

    @Autowired
    OutboxRepository outbox;

    @Autowired
    OrderRepository orders;

    long place(String customer, long cents) throws Exception {
        String json = mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customer\": \"" + customer + "\", \"totalCents\": " + cents + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    @Test
    void an_order_is_placed() throws Exception {
        place("visible", 100);
    }
}
