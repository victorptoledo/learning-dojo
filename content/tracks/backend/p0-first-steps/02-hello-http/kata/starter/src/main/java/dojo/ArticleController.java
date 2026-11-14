package dojo;

import org.springframework.web.bind.annotation.RestController;

/** GET and PUT /articles/{id}, with the HTTP rules of the README. */
@RestController
public class ArticleController {
    private final ArticleStore store;

    public ArticleController(ArticleStore store) {
        this.store = store;
    }
}
