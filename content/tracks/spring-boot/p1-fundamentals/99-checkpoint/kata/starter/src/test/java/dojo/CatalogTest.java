package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogTest {
    @Autowired
    MockMvc mvc;

    ResultActions add(String sku, String name, long price, String category) throws Exception {
        return mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(
                "{\"sku\": \"%s\", \"name\": \"%s\", \"priceCents\": %d, \"category\": \"%s\"}"
                        .formatted(sku, name, price, category)));
    }

    @Test
    void a_valid_product_is_created() throws Exception {
        add("ABC-123", "Notebook", 350, "office").andExpect(status().isCreated());
    }
}
