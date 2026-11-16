package dojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/** A comment under an article. The index matches the page's query: one article, newest first. */
@Entity
@Table(indexes = @Index(name = "comment_article_id_idx", columnList = "articleSlug, id"))
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String articleSlug;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false, length = 500)
    private String body;

    @Column(nullable = false)
    private Instant createdAt;

    protected Comment() {
    }

    public Comment(String articleSlug, String author, String body, Instant createdAt) {
        this.articleSlug = articleSlug;
        this.author = author;
        this.body = body;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getArticleSlug() {
        return articleSlug;
    }

    public String getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
