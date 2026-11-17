package dojo.secure;

import static dojo.secure.SecureApi.PASSWORD;
import static dojo.secure.SecureApi.signup;
import static dojo.secure.SecureApi.unique;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class Stage2SessionsAndCsrfTest {
    @Autowired
    MockMvc mvc;

    String token(MockHttpSession session) throws Exception {
        String body = mvc.perform(get("/api/csrf").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    /** What a browser does: it names the session it already has, as its cookie would. */
    static RequestPostProcessor sessionCookie(MockHttpSession session) {
        return request -> {
            request.setSession(session);
            request.setRequestedSessionId(session.getId());
            request.setRequestedSessionIdValid(true);
            return request;
        };
    }

    ResultActions login(MockHttpSession session, String username, String password) throws Exception {
        return login(session, token(session), username, password);
    }

    ResultActions login(MockHttpSession session, String token, String username, String password) throws Exception {
        return mvc.perform(post("/api/login").with(sessionCookie(session)).header("X-CSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", username).param("password", password));
    }

    /** Logs in through the form and returns the session the server keeps after the login. */
    MockHttpSession loggedIn(String username) throws Exception {
        MockHttpSession session = new MockHttpSession();
        return (MockHttpSession) login(session, username, PASSWORD).andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    String newUser() throws Exception {
        String username = unique("ada");
        signup(mvc, username, PASSWORD).andExpect(status().isCreated());
        return username;
    }

    @Test
    void anyone_gets_a_csrf_token() throws Exception {
        assertNotNull(token(new MockHttpSession()));
    }

    @Test
    void a_form_login_opens_a_session() throws Exception {
        String username = newUser();
        MockHttpSession session = new MockHttpSession();
        login(session, username, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
        MockHttpSession after = loggedIn(username);
        mvc.perform(get("/api/me").session(after))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void a_login_changes_the_session_id() throws Exception {
        String username = newUser();
        MockHttpSession session = new MockHttpSession();
        token(session);
        String before = session.getId();
        MockHttpSession after = (MockHttpSession) login(session, username, PASSWORD).andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
        assertNotNull(after);
        assertNotEquals(before, after.getId(), "the session id is the same after the login (session fixation)");
    }

    @Test
    void wrong_credentials_are_401_and_log_nobody_in() throws Exception {
        String username = newUser();
        MockHttpSession session = new MockHttpSession();
        login(session, username, "wrong-password-123").andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").session(session)).andExpect(status().isUnauthorized());
        login(new MockHttpSession(), unique("nobody"), PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void no_login_is_401_not_a_redirect() throws Exception {
        mvc.perform(get("/api/me").accept(MediaType.TEXT_HTML))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void a_logout_ends_the_session() throws Exception {
        MockHttpSession session = loggedIn(newUser());
        mvc.perform(post("/api/logout").session(session).header("X-CSRF-TOKEN", token(session)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void changes_without_the_right_token_are_403() throws Exception {
        MockHttpSession session = loggedIn(newUser());
        mvc.perform(post("/api/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/logout").session(session).header("X-CSRF-TOKEN", "forged"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/me").session(session)).andExpect(status().isOk());

        mvc.perform(post("/api/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"" + unique("csrf") + "\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "root").param("password", PASSWORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void the_token_changes_at_the_login() throws Exception {
        String username = newUser();
        MockHttpSession session = new MockHttpSession();
        String before = token(session);
        MockHttpSession after = (MockHttpSession) login(session, before, username, PASSWORD)
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
        mvc.perform(post("/api/logout").session(after).header("X-CSRF-TOKEN", before))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/logout").session(after).header("X-CSRF-TOKEN", token(after)))
                .andExpect(status().isNoContent());
    }
}
