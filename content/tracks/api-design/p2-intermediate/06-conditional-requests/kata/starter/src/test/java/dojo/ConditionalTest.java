package dojo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ConditionalTest {
    @Autowired
    MockMvc mvc;

    @Test
    void a_document_comes_with_an_etag() throws Exception {
        mvc.perform(get("/documents/readme"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"));
    }

    @Test
    void the_same_etag_back_is_a_304() throws Exception {
        String etag = mvc.perform(get("/documents/license")).andReturn().getResponse().getHeader("ETag");
        mvc.perform(get("/documents/license").header("If-None-Match", String.valueOf(etag)))
                .andExpect(status().isNotModified());
    }
}
