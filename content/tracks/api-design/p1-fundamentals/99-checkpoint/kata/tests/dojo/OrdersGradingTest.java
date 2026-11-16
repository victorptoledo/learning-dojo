package dojo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class OrdersGradingTest {
    static final String TYPES = "https://api.dojo.example/problems/";

    @Autowired
    MockMvc mvc;

    ResultActions list(String query) throws Exception {
        return mvc.perform(get("/orders?" + query));
    }

    static ResultActions isProblem(ResultActions actions, int status, String type) throws Exception {
        return actions
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.type").value(TYPES + type));
    }

    long place(String customer, long total) throws Exception {
        String body = mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customer\": \"" + customer + "\", \"totalCents\": " + total + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /** rel → URL, parsed from a Link header. */
    static Map<String, String> links(String header) {
        return Arrays.stream(header.split(",\\s*"))
                .map(part -> part.split(";\\s*rel="))
                .collect(Collectors.toMap(
                        pair -> pair[1].replace("\"", "").trim(),
                        pair -> pair[0].trim().replaceAll("^<|>$", "")));
    }

    @Test
    void the_default_page_is_page_1_of_10_by_id_with_totals() throws Exception {
        list("")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(10))
                .andExpect(jsonPath("$.items[*].id", contains(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalItems").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    void the_collection_is_filtered_by_status_and_customer() throws Exception {
        list("status=SHIPPED")
                .andExpect(jsonPath("$.items[*].status", everyItem(is("SHIPPED"))))
                .andExpect(jsonPath("$.totalItems").value(8))
                .andExpect(jsonPath("$.totalPages").value(1));
        list("status=SHIPPED&customer=cust-1")
                .andExpect(jsonPath("$.items[*].id", contains(6, 18, 30)))
                .andExpect(jsonPath("$.totalItems").value(3));
    }

    @Test
    void sort_descending_with_a_minus_and_page_through_the_result() throws Exception {
        list("status=SHIPPED&sort=-createdAt&size=3")
                .andExpect(jsonPath("$.items[*].id", contains(2, 10, 18)))
                .andExpect(jsonPath("$.totalPages").value(3));
        list("status=SHIPPED&sort=-createdAt&size=3&page=2")
                .andExpect(jsonPath("$.items[*].id", contains(26, 6, 14)));
        list("status=SHIPPED&sort=-createdAt&size=3&page=3")
                .andExpect(jsonPath("$.items[*].id", contains(22, 30)));
    }

    @Test
    void sort_by_several_fields_in_order() throws Exception {
        list("status=SHIPPED&sort=totalCents,-id")
                .andExpect(jsonPath("$.items[*].id", contains(30, 10, 26, 6, 22, 2, 18, 14)));
    }

    @Test
    void a_page_past_the_end_is_an_empty_page_not_an_error() throws Exception {
        list("status=SHIPPED&size=5&page=7")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalItems").value(8));
    }

    @Test
    void the_link_header_points_to_first_prev_next_and_last_keeping_the_filters() throws Exception {
        String header = list("status=PAID&size=3&page=2")
                .andExpect(status().isOk())
                .andExpect(header().exists("Link"))
                .andReturn().getResponse().getHeader("Link");
        Map<String, String> links = links(header);
        assertThat(links.get("next"), containsString("page=3"));
        assertThat(links.get("next"), containsString("status=PAID"));
        assertThat(links.get("next"), containsString("size=3"));
        assertThat(links.get("prev"), containsString("page=1"));
        assertThat(links.get("first"), containsString("page=1"));
        assertThat(links.get("last"), containsString("page=3"));
    }

    @Test
    void the_last_page_has_no_next_link_and_the_first_no_prev() throws Exception {
        String last = list("status=PAID&size=3&page=3").andReturn().getResponse().getHeader("Link");
        assertThat(links(last).containsKey("next"), is(false));
        String first = list("status=PAID&size=3").andReturn().getResponse().getHeader("Link");
        assertThat(links(first).containsKey("prev"), is(false));
        assertThat(links(first).get("next"), containsString("page=2"));
    }

    @Test
    void bad_query_parameters_are_400_problems_naming_the_parameter() throws Exception {
        isProblem(list("sort=price"), 400, "invalid-parameter")
                .andExpect(jsonPath("$.title").value("Invalid query parameter"))
                .andExpect(jsonPath("$.parameter").value("sort"));
        isProblem(list("sort=-secret"), 400, "invalid-parameter")
                .andExpect(jsonPath("$.parameter").value("sort"));
        isProblem(list("size=101"), 400, "invalid-parameter")
                .andExpect(jsonPath("$.parameter").value("size"));
        isProblem(list("size=0"), 400, "invalid-parameter")
                .andExpect(jsonPath("$.parameter").value("size"));
        isProblem(list("page=0"), 400, "invalid-parameter")
                .andExpect(jsonPath("$.parameter").value("page"));
        isProblem(list("status=LOST"), 400, "invalid-parameter")
                .andExpect(jsonPath("$.parameter").value("status"));
    }

    @Test
    void one_order_and_a_missing_one() throws Exception {
        mvc.perform(get("/orders/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.customer").value("cust-2"));
        isProblem(mvc.perform(get("/orders/999999")), 404, "order-not-found")
                .andExpect(jsonPath("$.title").value("Order not found"))
                .andExpect(jsonPath("$.detail").value("order 999999 does not exist"))
                .andExpect(jsonPath("$.instance").value("/orders/999999"));
    }

    @Test
    void placing_an_order_is_201_with_location() throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customer": "cust-9", "totalCents": 4200}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/orders/")))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.customer").value("cust-9"));
    }

    @Test
    void an_invalid_order_is_a_400_validation_problem() throws Exception {
        isProblem(mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customer": " ", "totalCents": 0}
                        """)), 400, "validation-error")
                .andExpect(jsonPath("$.detail").value("the request has 2 invalid fields"))
                .andExpect(jsonPath("$.errors[0].pointer").value("#/customer"))
                .andExpect(jsonPath("$.errors[1].pointer").value("#/totalCents"));
    }

    @Test
    void cancelling_is_a_state_change_on_its_own_resource_and_can_be_repeated() throws Exception {
        long id = place("cust-8", 1500);
        mvc.perform(post("/orders/" + id + "/cancellation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/orders/" + id + "/cancellation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(get("/orders/" + id)).andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void a_shipped_order_cannot_be_cancelled() throws Exception {
        isProblem(mvc.perform(post("/orders/2/cancellation")), 409, "order-not-cancellable")
                .andExpect(jsonPath("$.title").value("Order cannot be cancelled"))
                .andExpect(jsonPath("$.detail").value("order 2 is SHIPPED and can no longer be cancelled"));
        mvc.perform(get("/orders/2")).andExpect(jsonPath("$.status").value("SHIPPED"));
    }
}
