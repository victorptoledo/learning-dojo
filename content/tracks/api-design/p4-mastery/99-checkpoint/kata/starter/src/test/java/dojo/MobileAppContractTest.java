package dojo;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * What the mobile app (and every partner integration) relies on: the contract the API had
 * before last month's refactoring. It is a consumer contract: every expectation must pass.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MobileAppContractTest {
    @Autowired MockMvc mvc;
    @Autowired Tenants tenants;
    @Autowired InvoiceStore store;

    @Test
    void an_invoice_has_money_in_cents_a_due_day_and_an_issue_instant_in_utc() throws Exception {
        String key = tenants.register("mobile-contract-1");
        Invoice invoice = store.add("mobile-contract-1", "Ada Ltd", 12_345, "EUR", LocalDate.of(2026, 6, 30));
        mvc.perform(get("/invoices/" + invoice.id()).header("Authorization", "Bearer " + key))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoice.id()))
                .andExpect(jsonPath("$.customer").value("Ada Ltd"))
                .andExpect(jsonPath("$.amountCents").value(12345))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.dueDate").value("2026-06-30"))
                .andExpect(jsonPath("$.issuedAt").value(endsWith("Z")));
    }

    @Test
    void an_invoice_is_created_with_an_amount_in_cents() throws Exception {
        String key = tenants.register("mobile-contract-2");
        mvc.perform(post("/invoices").header("Authorization", "Bearer " + key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customer": "Grace Ltd", "amountCents": 1990, "currency": "EUR", "dueDate": "2026-07-01"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.amountCents").value(1990))
                .andExpect(jsonPath("$.dueDate").value("2026-07-01"));
    }

    @Test
    void the_list_wraps_invoices_in_data() throws Exception {
        String key = tenants.register("mobile-contract-3");
        store.add("mobile-contract-3", "Linus Ltd", 500, "EUR", LocalDate.of(2026, 8, 1));
        mvc.perform(get("/invoices").header("Authorization", "Bearer " + key))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].customer").value("Linus Ltd"))
                .andExpect(jsonPath("$.data[0].amountCents").value(500));
    }
}
