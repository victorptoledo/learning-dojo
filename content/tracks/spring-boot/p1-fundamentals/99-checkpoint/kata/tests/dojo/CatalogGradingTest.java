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

import static org.hamcrest.Matchers.endsWith;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogGradingTest {
    @Autowired
    MockMvc mvc;

    ResultActions add(String sku, String name, long price, String category) throws Exception {
        return mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(
                "{\"sku\": \"%s\", \"name\": \"%s\", \"priceCents\": %d, \"category\": \"%s\"}"
                        .formatted(sku, name, price, category)));
    }

    @Autowired
    CatalogProperties catalog;

    @Test
    void the_properties_record_reads_application_properties() {
        assertEquals(new CatalogProperties("EUR", 20, 50), catalog);
    }

    @Test
    void adds_a_product_and_reads_it_back() throws Exception {
        add("TEA-001", " Green tea ", 450, " Drinks ")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/products/TEA-001")))
                .andExpect(jsonPath("$.name").value("Green tea"))
                .andExpect(jsonPath("$.category").value("drinks"));
        mvc.perform(get("/products/TEA-001")).andExpect(status().isOk()).andExpect(jsonPath("$.priceCents").value(450));
    }

    @Test
    void invalid_products_are_400_with_field_errors() throws Exception {
        add("tea-1", "", -5, "x")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.sku").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.priceCents").exists())
                .andExpect(jsonPath("$.errors.category").doesNotExist());
    }

    @Test
    void duplicates_are_409_and_unknown_skus_404() throws Exception {
        add("MUG-001", "Mug", 1200, "kitchen").andExpect(status().isCreated());
        add("MUG-001", "Other mug", 900, "kitchen")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("product MUG-001 already exists"));
        mvc.perform(get("/products/NOP-999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("product NOP-999 not found"));
    }

    @Test
    void lists_a_category_in_pages_sorted_by_sku() throws Exception {
        add("BKS-003", "Book three", 300, "books");
        add("BKS-001", "Book one", 100, "books");
        add("BKS-002", "Book two", 200, "Books");
        mvc.perform(get("/products").param("category", "books").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sku").value("BKS-001"))
                .andExpect(jsonPath("$.items[1].sku").value("BKS-002"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.currency").value("EUR"));
        mvc.perform(get("/products").param("category", "books").param("page", "1").param("size", "2"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].sku").value("BKS-003"))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void a_page_larger_than_the_configured_maximum_is_400() throws Exception {
        mvc.perform(get("/products").param("size", "21"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("page size 21 is larger than 20"));
        mvc.perform(get("/products").param("size", "20")).andExpect(status().isOk());
    }

    @Test
    void negative_pages_and_sizes_are_400() throws Exception {
        mvc.perform(get("/products").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/products").param("size", "0")).andExpect(status().isBadRequest());
    }

    @Test
    void a_discount_quotes_the_rounded_down_price() throws Exception {
        add("PEN-001", "Pen", 999, "office");
        mvc.perform(post("/products/PEN-001/discount").contentType(MediaType.APPLICATION_JSON).content("{\"percent\": 15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceCents").value(999))
                .andExpect(jsonPath("$.discountedCents").value(849))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void a_discount_above_the_configured_maximum_is_422() throws Exception {
        add("BAG-001", "Bag", 5000, "office");
        mvc.perform(post("/products/BAG-001/discount").contentType(MediaType.APPLICATION_JSON).content("{\"percent\": 60}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("discount 60% is higher than the maximum of 50%"));
        mvc.perform(post("/products/BAG-001/discount").contentType(MediaType.APPLICATION_JSON).content("{\"percent\": 0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/products/NOP-001/discount").contentType(MediaType.APPLICATION_JSON).content("{\"percent\": 10}"))
                .andExpect(status().isNotFound());
    }
}
