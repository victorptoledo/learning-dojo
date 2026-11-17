package dojo.secure;

import static dojo.secure.SecureApi.PASSWORD;
import static dojo.secure.SecureApi.signup;
import static dojo.secure.SecureApi.together;
import static dojo.secure.SecureApi.unique;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class Stage1UsersAndPasswordsTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    UserDetailsService users;

    @Autowired
    PasswordEncoder encoder;

    @Test
    void anyone_can_sign_up_as_a_user() throws Exception {
        String username = unique("ada");
        signup(mvc, username, PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.roles", contains("USER")))
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/me").with(httpBasic(username, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.roles", contains("USER")));
    }

    @Test
    void bad_sign_ups_are_400_with_the_bad_fields() throws Exception {
        signup(mvc, "A!", "short")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.username").isString())
                .andExpect(jsonPath("$.errors.password").isString());
        signup(mvc, unique("x"), "p".repeat(73))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").isString())
                .andExpect(jsonPath("$.errors.username").doesNotExist());
        String username = unique("grace");
        signup(mvc, username, "my-name-is-" + username.toUpperCase())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").isString());
    }

    @Test
    void usernames_are_unique_even_when_sign_ups_race() throws Exception {
        String username = unique("twin");
        List<Callable<Integer>> calls = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            calls.add(() -> signup(mvc, username, PASSWORD).andReturn().getResponse().getStatus());
        }
        List<Integer> statuses = together(calls);
        assertEquals(1, statuses.stream().filter(s -> s == 201).count(), "statuses: " + statuses);
        assertEquals(7, statuses.stream().filter(s -> s == 409).count(), "statuses: " + statuses);
    }

    @Test
    void the_password_is_stored_as_a_salted_bcrypt_hash() throws Exception {
        String first = unique("hash");
        String second = unique("hash");
        signup(mvc, first, PASSWORD).andExpect(status().isCreated());
        signup(mvc, second, PASSWORD).andExpect(status().isCreated());

        String stored = users.loadUserByUsername(first).getPassword();
        assertFalse(stored.contains(PASSWORD), "the password is stored as it is");
        assertTrue(stored.startsWith("{bcrypt}$2") || stored.startsWith("$2"), "not a BCrypt hash: " + stored);
        assertTrue(encoder.matches(PASSWORD, stored), "the stored hash does not match the password");
        assertNotEquals(stored, users.loadUserByUsername(second).getPassword(), "the same password, the same hash: no salt");
    }

    @Test
    void the_rest_of_the_api_needs_a_right_login() throws Exception {
        String username = unique("linus");
        signup(mvc, username, PASSWORD).andExpect(status().isCreated());
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").with(httpBasic(username, "wrong-password-123"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").with(httpBasic(unique("nobody"), PASSWORD))).andExpect(status().isUnauthorized());
    }
}
