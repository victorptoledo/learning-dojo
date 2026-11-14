package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
    @Autowired
    MockMvc mvc;

    @Autowired
    ProductRepository products;

    void stock(String sku, long price, int units) {
        products.save(new Product(sku, price, units));
    }

    int stockOf(String sku) {
        return products.findAll().stream().filter(p -> p.getSku().equals(sku)).findFirst().orElseThrow().getStock();
    }

    ResultActions order(String json) throws Exception {
        return mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    long placed(String json) throws Exception {
        String body = order(json).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void placing_an_order_takes_the_stock_and_prices_the_lines() throws Exception {
        stock("g-tea", 300, 10);
        stock("g-mug", 1200, 2);
        order("""
                {"lines": [{"sku": "g-tea", "quantity": 3}, {"sku": "g-mug", "quantity": 1}]}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.totalCents").value(2100))
                .andExpect(jsonPath("$.lines[0].sku").value("g-tea"))
                .andExpect(jsonPath("$.lines[0].unitCents").value(300));
        assertEquals(7, stockOf("g-tea"));
        assertEquals(1, stockOf("g-mug"));
    }

    @Test
    void if_any_line_is_out_of_stock_nothing_changes() throws Exception {
        stock("r-tea", 300, 10);
        stock("r-mug", 1200, 1);
        order("""
                {"lines": [{"sku": "r-tea", "quantity": 4}, {"sku": "r-mug", "quantity": 2}]}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("only 1 of r-mug left, 2 wanted"));
        assertEquals(10, stockOf("r-tea"), "the first line must be rolled back too");
        assertEquals(1, stockOf("r-mug"));
    }

    @Test
    void an_unknown_product_is_404_and_rolls_back_the_other_lines() throws Exception {
        stock("u-tea", 300, 5);
        order("""
                {"lines": [{"sku": "u-tea", "quantity": 2}, {"sku": "u-nope", "quantity": 1}]}
                """)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("product u-nope not found"));
        assertEquals(5, stockOf("u-tea"));
    }

    @Test
    void an_order_can_be_read_with_its_lines() throws Exception {
        stock("o-pen", 150, 20);
        stock("o-ink", 400, 20);
        long id = placed("""
                {"lines": [{"sku": "o-pen", "quantity": 2}, {"sku": "o-ink", "quantity": 1}]}
                """);
        mvc.perform(get("/orders/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.lines[1].sku").value("o-ink"))
                .andExpect(jsonPath("$.totalCents").value(700));
        mvc.perform(get("/orders/987654")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("order 987654 not found"));
    }

    @Test
    void cancelling_puts_the_stock_back_once() throws Exception {
        stock("c-cup", 500, 3);
        long id = placed("""
                {"lines": [{"sku": "c-cup", "quantity": 3}]}
                """);
        assertEquals(0, stockOf("c-cup"));
        mvc.perform(post("/orders/" + id + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals(3, stockOf("c-cup"));
        mvc.perform(post("/orders/" + id + "/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("order " + id + " is already cancelled"));
        assertEquals(3, stockOf("c-cup"), "a second cancel must not add stock again");
    }

    @Test
    void invalid_orders_are_400() throws Exception {
        order("{\"lines\": []}").andExpect(status().isBadRequest());
        order("{\"lines\": [{\"sku\": \" \", \"quantity\": 1}]}").andExpect(status().isBadRequest());
        order("{\"lines\": [{\"sku\": \"x\", \"quantity\": 0}]}").andExpect(status().isBadRequest());
    }
}
