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

/** The same application with other settings: the code must read them, not hard-code them. */
@SpringBootTest(properties = {"catalog.currency=USD", "catalog.max-page-size=2", "catalog.max-discount-percent=10"})
@AutoConfigureMockMvc
class CatalogSettingsGradingTest {
    @Autowired
    MockMvc mvc;

    ResultActions add(String sku, String name, long price, String category) throws Exception {
        return mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(
                "{\"sku\": \"%s\", \"name\": \"%s\", \"priceCents\": %d, \"category\": \"%s\"}"
                        .formatted(sku, name, price, category)));
    }

    @Test
    void other_settings_change_the_limits_and_the_currency() throws Exception {
        add("CUP-001", "Cup", 1000, "kitchen").andExpect(status().isCreated());
        mvc.perform(get("/products").param("size", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("page size 3 is larger than 2"));
        mvc.perform(get("/products").param("size", "2")).andExpect(jsonPath("$.currency").value("USD"));
        mvc.perform(post("/products/CUP-001/discount").contentType(MediaType.APPLICATION_JSON).content("{\"percent\": 11}"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(post("/products/CUP-001/discount").contentType(MediaType.APPLICATION_JSON).content("{\"percent\": 10}"))
                .andExpect(jsonPath("$.discountedCents").value(900))
                .andExpect(jsonPath("$.currency").value("USD"));
    }
}
