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
class OrdersTest {
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
    void an_order_is_placed() throws Exception {
        stock("v-tea", 300, 5);
        placed("""
                {"lines": [{"sku": "v-tea", "quantity": 1}]}
                """);
        assertEquals(4, stockOf("v-tea"));
    }
}
