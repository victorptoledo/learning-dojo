package dojo.secure;

import static dojo.secure.SecureApi.PASSWORD;
import static dojo.secure.SecureApi.signup;
import static dojo.secure.SecureApi.unique;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Another first administrator than the one in application.properties. */
@SpringBootTest(properties = {"secure.admin.username=chief", "secure.admin.password=chief-password-from-test"})
@AutoConfigureMockMvc
class Stage3RolesAndOwnershipTest {
    static final RequestPostProcessor CHIEF = httpBasic("chief", "chief-password-from-test");

    @Autowired
    MockMvc mvc;

    @Autowired
    UserDetailsService users;

    @Autowired
    PasswordEncoder encoder;

    record Login(String username, RequestPostProcessor basic) {
    }

    Login user(String name) throws Exception {
        String username = unique(name);
        signup(mvc, username, PASSWORD).andExpect(status().isCreated());
        return new Login(username, httpBasic(username, PASSWORD));
    }

    long project(Login owner, String name) throws Exception {
        String body = mvc.perform(post("/api/projects").with(owner.basic()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    ResultActions addMember(Login who, long project, String username) throws Exception {
        return mvc.perform(put("/api/projects/" + project + "/members/" + username).with(who.basic()).with(csrf()));
    }

    ResultActions roles(RequestPostProcessor who, String username, String json) throws Exception {
        return mvc.perform(put("/api/admin/users/" + username + "/roles").with(who).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void the_first_admin_comes_from_configuration() throws Exception {
        mvc.perform(get("/api/me").with(CHIEF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("chief"))
                .andExpect(jsonPath("$.roles", contains("ADMIN", "USER")));
        String stored = users.loadUserByUsername("chief").getPassword();
        assertTrue(encoder.matches("chief-password-from-test", stored) && !stored.contains("chief-password"),
                "the administrator password must be stored hashed");
        mvc.perform(get("/api/me").with(httpBasic("root", "root-password-for-dev"))).andExpect(status().isUnauthorized());
    }

    @Test
    void a_user_creates_and_lists_own_projects() throws Exception {
        Login ada = user("ada");
        String location = mvc.perform(post("/api/projects").with(ada.basic()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Apollo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Apollo"))
                .andExpect(jsonPath("$.owner").value(ada.username()))
                .andExpect(jsonPath("$.members", empty()))
                .andReturn().getResponse().getHeader("Location");
        assertTrue(location != null && location.matches(".*/api/projects/\\d+"), "Location was " + location);
        long second = project(ada, "Gemini");
        mvc.perform(get("/api/projects").with(ada.basic()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Apollo", "Gemini")));
        mvc.perform(get("/api/projects/" + second).with(ada.basic())).andExpect(status().isOk());
        mvc.perform(post("/api/projects").with(ada.basic()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").isString());
    }

    @Test
    void strangers_get_404_for_everything() throws Exception {
        Login ada = user("ada");
        Login eve = user("eve");
        long id = project(ada, "Secret");
        String detail = "project " + id + " not found";
        mvc.perform(get("/api/projects/" + id).with(eve.basic()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value(detail));
        addMember(eve, id, eve.username()).andExpect(status().isNotFound());
        mvc.perform(delete("/api/projects/" + id).with(eve.basic()).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/api/projects").with(eve.basic())).andExpect(jsonPath("$[*].name", not(hasItem("Secret"))));
        mvc.perform(get("/api/projects/987654").with(eve.basic()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("project 987654 not found"));
    }

    @Test
    void members_see_but_do_not_change() throws Exception {
        Login ada = user("ada");
        Login bob = user("bob");
        Login cy = user("cy");
        long id = project(ada, "Shared");
        addMember(ada, id, bob.username())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", contains(bob.username())));
        mvc.perform(get("/api/projects/" + id).with(bob.basic())).andExpect(status().isOk());
        mvc.perform(get("/api/projects").with(bob.basic())).andExpect(jsonPath("$[*].name", hasItem("Shared")));
        addMember(bob, id, cy.username()).andExpect(status().isForbidden());
        mvc.perform(delete("/api/projects/" + id).with(bob.basic()).with(csrf())).andExpect(status().isForbidden());
        addMember(ada, id, unique("ghost")).andExpect(status().isNotFound());

        mvc.perform(delete("/api/projects/" + id + "/members/" + bob.username()).with(ada.basic()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", empty()));
        mvc.perform(get("/api/projects/" + id).with(bob.basic())).andExpect(status().isNotFound());
    }

    @Test
    void owners_and_admins_delete_projects() throws Exception {
        Login ada = user("ada");
        long mine = project(ada, "Mine");
        mvc.perform(delete("/api/projects/" + mine).with(ada.basic()).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/projects/" + mine).with(ada.basic())).andExpect(status().isNotFound());

        long other = project(ada, "Watched");
        mvc.perform(get("/api/projects/" + other).with(CHIEF)).andExpect(status().isOk());
        mvc.perform(delete("/api/projects/" + other).with(CHIEF).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/projects/" + other).with(ada.basic())).andExpect(status().isNotFound());
    }

    @Test
    void only_admins_administer() throws Exception {
        Login ada = user("ada");
        mvc.perform(get("/api/admin/users").with(ada.basic())).andExpect(status().isForbidden());
        roles(ada.basic(), ada.username(), "{\"roles\": [\"USER\", \"ADMIN\"]}").andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/admin/users").with(CHIEF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", hasItem(ada.username())))
                .andExpect(jsonPath("$[*].password").doesNotExist());
    }

    @Test
    void admins_change_roles_from_the_next_request_on() throws Exception {
        Login ada = user("ada");
        roles(CHIEF, ada.username(), "{\"roles\": [\"USER\", \"ADMIN\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", contains("ADMIN", "USER")));
        mvc.perform(get("/api/admin/users").with(ada.basic())).andExpect(status().isOk());
        roles(CHIEF, ada.username(), "{\"roles\": [\"USER\"]}").andExpect(status().isOk());
        mvc.perform(get("/api/admin/users").with(ada.basic())).andExpect(status().isForbidden());

        roles(CHIEF, ada.username(), "{\"roles\": []}").andExpect(status().isBadRequest());
        roles(CHIEF, ada.username(), "{\"roles\": [\"GOD\"]}").andExpect(status().isBadRequest());
        roles(CHIEF, unique("ghost"), "{\"roles\": [\"USER\"]}").andExpect(status().isNotFound());
        roles(CHIEF, "chief", "{\"roles\": [\"USER\"]}").andExpect(status().isConflict());
        mvc.perform(get("/api/admin/users").with(CHIEF)).andExpect(status().isOk());
    }
}
