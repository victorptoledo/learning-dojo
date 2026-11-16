package dojo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SessionApiGradingTest {
    static final String SPA = "https://app.example.com";

    @Autowired
    MockMvc mvc;

    @Autowired
    Environment environment;


    MockHttpSession login(String user) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/login").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"" + user + "\", \"password\": \"" + user + "-pass\"}"))
                .andExpect(status().isOk());
        return session;
    }

    @Test
    void wrong_credentials_are_a_json_401_and_log_nobody_in() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/login").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"ada\", \"password\": \"nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("bad credentials"));
        mvc.perform(get("/api/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void the_session_is_saved_and_tells_who_is_logged_in() throws Exception {
        MockHttpSession session = login("linus");
        mvc.perform(get("/api/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("linus"))
                .andExpect(jsonPath("$.displayName").value("linus"))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("USER")));
    }

    @Test
    void no_login_page_and_no_http_basic() throws Exception {
        mvc.perform(get("/api/me").header("Authorization", "Basic YWRhOmFkYS1wYXNz"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(header().doesNotExist("WWW-Authenticate"));
        mvc.perform(get("/login")).andExpect(status().is4xxClientError());
    }

    @Test
    void the_login_itself_needs_the_csrf_token() throws Exception {
        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"ada\", \"password\": \"ada-pass\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void a_change_without_the_csrf_header_is_403_even_when_logged_in() throws Exception {
        MockHttpSession session = login("ada");
        mvc.perform(put("/api/me").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\": \"Countess\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/me").session(session)).andExpect(jsonPath("$.displayName").value("ada"));
    }

    @Test
    void a_user_renames_themself_with_a_trimmed_name() throws Exception {
        MockHttpSession session = login("ada");
        mvc.perform(put("/api/me").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\": \"  Countess Lovelace \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Countess Lovelace"));
        mvc.perform(get("/api/me").session(session)).andExpect(jsonPath("$.displayName").value("Countess Lovelace"));
        mvc.perform(put("/api/me").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\": \"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_ends_the_session() throws Exception {
        MockHttpSession session = login("ada");
        mvc.perform(post("/api/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void the_spa_origin_passes_the_preflight_with_credentials() throws Exception {
        mvc.perform(options("/api/me").header("Origin", SPA)
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", SPA))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void other_origins_are_refused() throws Exception {
        mvc.perform(options("/api/me").header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mvc.perform(options("/api/me").header("Origin", SPA)
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isForbidden());
    }

    @Test
    void real_requests_from_the_spa_carry_the_cors_headers() throws Exception {
        MockHttpSession session = login("ada");
        mvc.perform(get("/api/me").session(session).header("Origin", SPA))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", SPA))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void the_session_cookie_is_http_only_secure_and_same_site_lax() {
        assertThat(environment.getProperty("server.servlet.session.cookie.http-only")).isEqualToIgnoringCase("true");
        assertThat(environment.getProperty("server.servlet.session.cookie.secure")).isEqualToIgnoringCase("true");
        assertThat(environment.getProperty("server.servlet.session.cookie.same-site")).isEqualToIgnoringCase("lax");
    }
}
