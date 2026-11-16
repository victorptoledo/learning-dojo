package dojo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * The CSRF cookie as the browser sees it. Spring Security's MockMvc support swaps the token
 * repository for a test one, so this class has its own context and the real filter chain.
 */
@SpringBootTest(properties = "dojo.context=real-csrf")
class CsrfCookieGradingTest {
    @Autowired
    WebApplicationContext context;

    @Test
    void the_spa_reads_the_token_cookie_and_sends_it_back_in_a_header() throws Exception {
        MockMvc real = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
                .build();
        MockHttpSession session = new MockHttpSession();
        MvcResult primed = real.perform(get("/api/csrf").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        Cookie cookie = primed.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).as("the XSRF-TOKEN cookie").isNotNull();
        assertThat(cookie.isHttpOnly()).as("the SPA must be able to read XSRF-TOKEN").isFalse();
        real.perform(post("/api/login").session(session).cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"ada\", \"password\": \"ada-pass\"}"))
                .andExpect(status().isForbidden());
        real.perform(post("/api/login").session(session).cookie(cookie)
                        .header("X-XSRF-TOKEN", cookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"ada\", \"password\": \"ada-pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ada"));
    }
}
