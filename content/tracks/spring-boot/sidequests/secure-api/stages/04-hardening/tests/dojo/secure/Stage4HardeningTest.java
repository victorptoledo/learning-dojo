package dojo.secure;

import static dojo.secure.SecureApi.PASSWORD;
import static dojo.secure.SecureApi.signup;
import static dojo.secure.SecureApi.unique;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Three attempts and ten minutes instead of the settings in application.properties. */
@SpringBootTest(properties = {"secure.admin.username=chief", "secure.admin.password=chief-password-from-test",
        "secure.lockout.attempts=3", "secure.lockout.duration=PT10M"})
@AutoConfigureMockMvc
class Stage4HardeningTest {
    static final RequestPostProcessor CHIEF = httpBasic("chief", "chief-password-from-test");
    static final String WRONG = "not-the-password-1";

    @TestConfiguration
    static class Clocks {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    MutableClock clock;

    String user() throws Exception {
        String username = unique("ada");
        signup(mvc, username, PASSWORD).andExpect(status().isCreated());
        return username;
    }

    ResultActions me(String username, String password) throws Exception {
        return mvc.perform(get("/api/me").with(httpBasic(username, password)));
    }

    ResultActions formLogin(String username, String password) throws Exception {
        return mvc.perform(post("/api/login").with(csrf()).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", username).param("password", password));
    }

    ResultActions audit(String username) throws Exception {
        return mvc.perform(get("/api/admin/audit").param("username", username).with(CHIEF));
    }

    ResultActions changePassword(String username, String current, String next) throws Exception {
        return mvc.perform(put("/api/me/password").with(httpBasic(username, current)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\": \"" + current + "\", \"newPassword\": \"" + next + "\"}"));
    }

    @Test
    void too_many_wrong_passwords_lock_the_account_for_a_while() throws Exception {
        String ada = user();
        me(ada, WRONG).andExpect(status().isUnauthorized());
        formLogin(ada, WRONG).andExpect(status().isUnauthorized());
        me(ada, WRONG).andExpect(status().isUnauthorized());

        me(ada, PASSWORD).andExpect(status().isUnauthorized());
        formLogin(ada, PASSWORD).andExpect(status().isUnauthorized());
        clock.advance(Duration.ofMinutes(9));
        me(ada, PASSWORD).andExpect(status().isUnauthorized());
        clock.advance(Duration.ofMinutes(1));
        me(ada, PASSWORD).andExpect(status().isOk());
        formLogin(ada, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void a_good_login_starts_the_count_again() throws Exception {
        String ada = user();
        me(ada, WRONG).andExpect(status().isUnauthorized());
        me(ada, WRONG).andExpect(status().isUnauthorized());
        me(ada, PASSWORD).andExpect(status().isOk());
        me(ada, WRONG).andExpect(status().isUnauthorized());
        me(ada, WRONG).andExpect(status().isUnauthorized());
        me(ada, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void unknown_users_lock_nothing() throws Exception {
        String ghost = unique("ghost");
        for (int i = 0; i < 4; i++) {
            me(ghost, WRONG).andExpect(status().isUnauthorized());
        }
        signup(mvc, ghost, PASSWORD).andExpect(status().isCreated());
        me(ghost, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void admins_read_the_audit_trail() throws Exception {
        String ada = user();
        for (int i = 0; i < 3; i++) {
            me(ada, WRONG).andExpect(status().isUnauthorized());
        }
        audit(ada)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].type", contains("ACCOUNT_LOCKED", "LOGIN_FAILURE", "LOGIN_FAILURE",
                        "LOGIN_FAILURE")))
                .andExpect(jsonPath("$[*].username", everyItem(is(ada))))
                .andExpect(jsonPath("$[0].at").isString());
        mvc.perform(get("/api/admin/audit").with(CHIEF)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("ACCOUNT_LOCKED"));
        String bob = user();
        mvc.perform(get("/api/admin/audit").with(httpBasic(bob, PASSWORD))).andExpect(status().isForbidden());
    }

    @Test
    void a_new_password_replaces_the_old_one() throws Exception {
        String ada = user();
        String next = "a-brand-new-passphrase";
        changePassword(ada, PASSWORD, next).andExpect(status().isNoContent());
        me(ada, PASSWORD).andExpect(status().isUnauthorized());
        me(ada, next).andExpect(status().isOk());
        audit(ada).andExpect(jsonPath("$[*].type", contains("LOGIN_FAILURE", "PASSWORD_CHANGED")));
    }

    @Test
    void a_password_change_checks_both_passwords() throws Exception {
        String ada = user();
        mvc.perform(put("/api/me/password").with(httpBasic(ada, PASSWORD)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"" + WRONG + "\", \"newPassword\": \"a-brand-new-passphrase\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.currentPassword").isString());
        changePassword(ada, PASSWORD, "short").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").isString());
        changePassword(ada, PASSWORD, "still-" + ada + "-inside").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").isString());
        me(ada, PASSWORD).andExpect(status().isOk());
        mvc.perform(put("/api/me/password").contentType(MediaType.APPLICATION_JSON).with(csrf())
                        .content("{\"currentPassword\": \"x\", \"newPassword\": \"y\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void every_response_has_the_security_headers() throws Exception {
        String ada = user();
        for (ResultActions response : new ResultActions[] {me(ada, PASSWORD), me(ada, WRONG),
                mvc.perform(get("/api/csrf"))}) {
            response.andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("Referrer-Policy", "no-referrer"))
                    .andExpect(header().string("Cache-Control", containsString("no-store")));
        }
    }
}
