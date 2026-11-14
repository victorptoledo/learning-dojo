package dojo;

import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** GET and PUT /articles/{id}, with the HTTP rules of the README. */
@RestController
public class ArticleController {
    private final ArticleStore store;

    public ArticleController(ArticleStore store) {
        this.store = store;
    }

    @GetMapping(value = "/articles/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Article> get(@PathVariable long id,
                                       @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        return store.find(id).map(article -> {
            String etag = etag(article);
            if (etag.equals(ifNoneMatch)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).<Article>build();
            }
            return ResponseEntity.ok()
                    .eTag(etag)
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)))
                    .body(article);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/articles/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Article> put(@PathVariable long id,
                                       @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                       @RequestBody ArticleUpdate update) {
        return store.find(id).map(article -> {
            if (ifMatch != null && !ifMatch.equals(etag(article))) {
                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).<Article>build();
            }
            Article saved = store.update(article, update);
            return ResponseEntity.ok().eTag(etag(saved)).body(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    private static String etag(Article article) {
        return "\"v" + article.version() + "\"";
    }
}
