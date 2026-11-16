package dojo;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogGradingTest {
    @Autowired
    MockMvc mvc;

    ResultActions create(String json) throws Exception {
        return mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    long createdId(String name) throws Exception {
        String body = create("{\"name\": \"" + name + "\", \"category\": \"garden\", \"priceCents\": 999}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void the_collection_lists_every_product() throws Exception {
        mvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[*].name", hasItems("Kettle", "Toaster", "Desk lamp")));
    }

    @Test
    void the_collection_is_filtered_with_a_query_parameter() throws Exception {
        mvc.perform(get("/products").param("category", "kitchen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].category", everyItem(is("kitchen"))))
                .andExpect(jsonPath("$[*].name", hasItems("Kettle", "Toaster")));
        mvc.perform(get("/products").param("category", "no-such-category"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void one_product_is_read_by_its_id() throws Exception {
        mvc.perform(get("/products/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Desk lamp"))
                .andExpect(jsonPath("$.category").value("lighting"))
                .andExpect(jsonPath("$.priceCents").value(1999));
    }

    @Test
    void an_unknown_product_is_404_not_200_with_an_error_flag() throws Exception {
        mvc.perform(get("/products/987654")).andExpect(status().isNotFound());
    }

    @Test
    void creating_answers_201_with_the_location_of_the_new_resource() throws Exception {
        MockHttpServletResponse response = create("""
                        {"name": "Watering can", "category": "garden", "priceCents": 1250}
                        """)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("http")))
                .andExpect(jsonPath("$.name").value("Watering can"))
                .andReturn().getResponse();
        long id = ((Number) JsonPath.read(response.getContentAsString(), "$.id")).longValue();
        String location = response.getHeader("Location");
        assertTrue(location.endsWith("/products/" + id),
                "Location should be the URL of the new product, got " + location);
        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Watering can"));
    }

    @Test
    void invalid_input_is_400() throws Exception {
        create("""
                {"name": "", "category": "garden", "priceCents": -1}
                """).andExpect(status().isBadRequest());
        create("{}").andExpect(status().isBadRequest());
    }

    @Test
    void put_replaces_the_whole_product() throws Exception {
        long id = createdId("Rake");
        mvc.perform(put("/products/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Leaf rake", "category": "garden", "priceCents": 1799}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Leaf rake"))
                .andExpect(jsonPath("$.priceCents").value(1799));
        mvc.perform(get("/products/" + id)).andExpect(jsonPath("$.name").value("Leaf rake"));
    }

    @Test
    void put_is_a_full_replacement_so_a_missing_field_is_400() throws Exception {
        long id = createdId("Hose");
        mvc.perform(put("/products/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Garden hose"}
                        """))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/products/" + id)).andExpect(jsonPath("$.name").value("Hose"));
    }

    @Test
    void put_on_an_unknown_product_is_404() throws Exception {
        mvc.perform(put("/products/987654").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ghost", "category": "garden", "priceCents": 1}
                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_answers_204_and_the_product_is_gone() throws Exception {
        long id = createdId("Shovel");
        mvc.perform(delete("/products/" + id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mvc.perform(get("/products/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete("/products/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void a_method_the_collection_does_not_support_is_405() throws Exception {
        mvc.perform(delete("/products")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void the_rpc_style_endpoints_are_gone() throws Exception {
        mvc.perform(post("/getProducts")).andExpect(status().is4xxClientError());
        mvc.perform(post("/createProduct").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Old", "category": "garden", "priceCents": 1}
                        """))
                .andExpect(status().is4xxClientError());
    }
}
