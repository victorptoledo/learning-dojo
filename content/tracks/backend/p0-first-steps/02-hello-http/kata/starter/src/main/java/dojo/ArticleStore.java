package dojo;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Two articles to start with, kept in memory. */
@Component
public class ArticleStore {
    private final Map<Long, Article> articles = new ConcurrentHashMap<>(Map.of(
            1L, new Article(1, "Hello, HTTP", "Methods, status codes and headers.", 1),
            2L, new Article(2, "Caching", "ETag and Cache-Control.", 1)));

    public Optional<Article> find(long id) {
        return Optional.ofNullable(articles.get(id));
    }

    /** Saves a new version of the article and returns it. */
    public Article update(Article current, ArticleUpdate update) {
        Article next = new Article(current.id(), update.title(), update.body(), current.version() + 1);
        articles.put(next.id(), next);
        return next;
    }
}
