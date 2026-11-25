package dojo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class RatesTest {
    @Autowired
    MockMvc mvc;

    @Test
    void a_trader_sets_a_rate_and_anyone_reads_it() throws Exception {
        mvc.perform(put("/rates/JPY").with(httpBasic("trader", "trader-pass"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"perEuro\": 162.5}"))
                .andExpect(status().isOk());
        mvc.perform(get("/rates/JPY")).andExpect(status().isOk()).andExpect(jsonPath("$.perEuro").value(162.5));
    }

    @Test
    void a_viewer_cannot_change_a_rate() throws Exception {
        mvc.perform(put("/rates/JPY").with(httpBasic("viewer", "viewer-pass"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"perEuro\": 1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void the_health_check_is_public() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(jsonPath("$.components.rates").exists());
    }
}
