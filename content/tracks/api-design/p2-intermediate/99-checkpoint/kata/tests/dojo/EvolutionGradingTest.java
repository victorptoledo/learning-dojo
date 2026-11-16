package dojo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class EvolutionGradingTest {
    @Autowired
    MockMvc mvc;

    ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    long idOf(ResultActions actions) throws Exception {
        return ((Number) JsonPath.read(actions.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    // ---- the old client keeps working (the same contract as LegacyClientContractTest) -------

    @Test
    void v1_reads_keep_their_shape() throws Exception {
        mvc.perform(get("/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Ada Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.phone").value("+44 20 7946 0001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(get("/customers/2"))
                .andExpect(jsonPath("$.name").value("Linus Torvalds"))
                .andExpect(jsonPath("$.email").value("linus@example.org"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
        mvc.perform(get("/customers/3"))
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.emails").doesNotExist());
    }

    @Test
    void v1_signups_keep_working_and_ignore_fields_they_do_not_know() throws Exception {
        postJson("/customers", """
                {"name": "Alan Turing", "email": "alan@example.com", "phone": "+44 161 000 0000",
                 "nickname": "Prof"}
                """)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/customers/")))
                .andExpect(jsonPath("$.name").value("Alan Turing"))
                .andExpect(jsonPath("$.email").value("alan@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(get("/customers/424242")).andExpect(status().isNotFound());
    }

    @Test
    void v1_signups_are_still_validated() throws Exception {
        postJson("/customers", """
                {"name": "", "email": "not-an-email"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // ---- version 1 announces its end -----------------------------------------------------------

    @Test
    void v1_answers_are_deprecated_with_a_sunset_and_a_successor() throws Exception {
        String link = mvc.perform(get("/customers/1"))
                .andExpect(header().string("Deprecation", "@1793491200"))
                .andExpect(header().string("Sunset", "Tue, 01 Jun 2027 00:00:00 GMT"))
                .andReturn().getResponse().getHeader("Link");
        assertThat(link, containsString("</v2/customers/1>; rel=\"successor-version\""));
        assertThat(link, containsString("<https://api.dojo.example/docs/migrate-to-v2>; rel=\"deprecation\""));
        postJson("/customers", """
                {"name": "Katherine Johnson", "email": "kj@example.com"}
                """)
                .andExpect(status().isCreated())
                .andExpect(header().string("Deprecation", "@1793491200"))
                .andExpect(header().string("Sunset", "Tue, 01 Jun 2027 00:00:00 GMT"));
    }

    @Test
    void v2_is_not_deprecated() throws Exception {
        mvc.perform(get("/v2/customers/1"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(header().doesNotExist("Sunset"));
    }

    // ---- version 2 -----------------------------------------------------------------------------

    @Test
    void v2_serves_the_new_model() throws Exception {
        mvc.perform(get("/v2/customers/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.givenName").value("Linus"))
                .andExpect(jsonPath("$.familyName").value("Torvalds"))
                .andExpect(jsonPath("$.emails[0]").value("linus@example.org"))
                .andExpect(jsonPath("$.emails[1]").value("lt@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("+358 9 0000 0002"))
                .andExpect(jsonPath("$.country").value("FI"))
                .andExpect(jsonPath("$.name").doesNotExist());
        mvc.perform(get("/v2/customers/3")).andExpect(jsonPath("$.status").value("SUSPENDED"));
        mvc.perform(get("/v2/customers/424242"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void v2_signups_use_the_new_shape_and_require_a_country() throws Exception {
        postJson("/v2/customers", """
                {"givenName": "Hedy", "familyName": "Lamarr", "emails": ["hedy@example.com"], "country": "AT"}
                """)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/v2/customers/")))
                .andExpect(jsonPath("$.givenName").value("Hedy"))
                .andExpect(jsonPath("$.country").value("AT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        for (String invalid : new String[] {
            "{\"givenName\": \"No\", \"familyName\": \"Country\", \"emails\": [\"n@example.com\"]}",
            "{\"givenName\": \"Bad\", \"familyName\": \"Country\", \"emails\": [\"b@example.com\"], \"country\": \"at\"}",
            "{\"givenName\": \"No\", \"familyName\": \"Emails\", \"emails\": [], \"country\": \"AT\"}"
        }) {
            postJson("/v2/customers", invalid)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
    }

    @Test
    void one_customer_two_representations() throws Exception {
        long fromV2 = idOf(postJson("/v2/customers", """
                {"givenName": "Margaret", "familyName": "Hamilton",
                 "emails": ["margaret@example.com", "mh@example.org"], "country": "US"}
                """).andExpect(status().isCreated()));
        mvc.perform(get("/customers/" + fromV2))
                .andExpect(jsonPath("$.name").value("Margaret Hamilton"))
                .andExpect(jsonPath("$.email").value("margaret@example.com"));
        long fromV1 = idOf(postJson("/customers", """
                {"name": "Barbara Liskov", "email": "barbara@example.com", "phone": "+1 617 000 0000"}
                """).andExpect(status().isCreated()));
        mvc.perform(get("/v2/customers/" + fromV1))
                .andExpect(jsonPath("$.givenName").value("Barbara"))
                .andExpect(jsonPath("$.familyName").value("Liskov"))
                .andExpect(jsonPath("$.emails[0]").value("barbara@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("+1 617 000 0000"))
                .andExpect(jsonPath("$.country").doesNotExist());
    }

    // ---- caching per representation ------------------------------------------------------------

    @Test
    void each_representation_has_its_own_etag_and_revalidates_with_304() throws Exception {
        String v1 = mvc.perform(get("/customers/1")).andExpect(header().exists("ETag"))
                .andReturn().getResponse().getHeader("ETag");
        String v2 = mvc.perform(get("/v2/customers/1")).andExpect(header().exists("ETag"))
                .andReturn().getResponse().getHeader("ETag");
        assertThat(v1, not(is(v2)));
        mvc.perform(get("/customers/1").header("If-None-Match", v1)).andExpect(status().isNotModified());
        mvc.perform(get("/v2/customers/1").header("If-None-Match", v2)).andExpect(status().isNotModified());
        mvc.perform(get("/v2/customers/1").header("If-None-Match", v1)).andExpect(status().isOk());
    }
}
