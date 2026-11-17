package dojo.tickets;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * What every contract test shares: one application on a random port, a clock moved by hand,
 * and other settings than application.properties (holds last 7 minutes, at most 4 seats).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"tickets.hold-ttl=PT7M", "tickets.max-seats-per-hold=4"})
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
abstract class TicketsContractTest {
    static final RequestPostProcessor ADA = httpBasic("ada", "ada-pass");
    static final RequestPostProcessor LINUS = httpBasic("linus", "linus-pass");
    static final RequestPostProcessor GRACE = httpBasic("grace", "grace-pass");
    static final RequestPostProcessor BOSS = httpBasic("boss", "boss-pass");
    static final String STARTS = "2099-06-01T20:00:00Z";

    @Autowired
    MockMvc mvc;

    @Autowired
    MutableClock clock;

    @LocalServerPort
    int port;

    final HttpClient http = HttpClient.newHttpClient();

    ResultActions createEvent(RequestPostProcessor who, String json) throws Exception {
        return mvc.perform(post("/events").with(who).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    long event(int rows, int seatsPerRow, long priceCents) throws Exception {
        String json = "{\"name\": \"Show\", \"startsAt\": \"" + STARTS + "\", \"rows\": " + rows
                + ", \"seatsPerRow\": " + seatsPerRow + ", \"priceCents\": " + priceCents + "}";
        return idOf(createEvent(BOSS, json).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    static String seatsJson(String... seats) {
        return "{\"seats\": [" + Stream.of(seats).map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")) + "]}";
    }

    ResultActions hold(RequestPostProcessor who, long event, String... seats) throws Exception {
        return mvc.perform(post("/events/" + event + "/holds").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(seatsJson(seats)));
    }

    long held(RequestPostProcessor who, long event, String... seats) throws Exception {
        return idOf(hold(who, event, seats).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    static long idOf(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    /** A response read over a real connection. */
    record Reply(int status, String body) {
    }

    /** A POST over HTTP to the running server, as `user` (password `<user>-pass`). */
    Reply send(String user, String path, String json, String... headers) throws Exception {
        String credentials = Base64.getEncoder()
                .encodeToString((user + ":" + user + "-pass").getBytes(StandardCharsets.UTF_8));
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (headers.length > 0) {
            request.headers(headers);
        }
        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body());
    }

    /** A DELETE over HTTP to the running server, as `user`. */
    Reply sendDelete(String user, String path) throws Exception {
        String credentials = Base64.getEncoder()
                .encodeToString((user + ":" + user + "-pass").getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Authorization", "Basic " + credentials).DELETE().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body());
    }

    /** Runs every call at once, on as many threads as calls, and returns the replies in order. */
    static List<Reply> together(List<Callable<Reply>> calls) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(calls.size(), 24));
        try {
            List<Reply> replies = new ArrayList<>();
            for (Future<Reply> future : pool.invokeAll(calls)) {
                replies.add(future.get());
            }
            return replies;
        } finally {
            pool.shutdownNow();
        }
    }

    static long count(List<Reply> replies, int status) {
        return replies.stream().filter(r -> r.status() == status).count();
    }
}
