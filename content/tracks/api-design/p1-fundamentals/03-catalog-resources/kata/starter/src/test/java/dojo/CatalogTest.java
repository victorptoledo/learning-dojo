package dojo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogTest {
    @Autowired
    MockMvc mvc;

    @Test
    void one_product_is_a_resource_of_its_own() throws Exception {
        mvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kettle"));
    }

    @Test
    void creating_a_product_answers_201() throws Exception {
        mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Teapot", "category": "kitchen", "priceCents": 1599}
                        """))
                .andExpect(status().isCreated());
    }
}
