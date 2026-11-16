package dojo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * GET and POST /api/articles/{slug}/comments, DELETE /api/comments/{id} (see README.md).
 */
@RestController
public class CommentController {
    public record NewComment(@NotBlank @Size(max = 500) String body) {}

    public record CommentView(long id, String author, String body, Instant createdAt, boolean canDelete) {}

    public record Page(List<CommentView> items, String nextCursor) {}

    private final CommentRepository comments;

    public CommentController(CommentRepository comments) {
        this.comments = comments;
    }

    @GetMapping("/api/articles/{slug}/comments")
    Page list(@PathVariable String slug,
              @RequestParam(defaultValue = "10") int limit,
              @RequestParam(required = false) String cursor,
              Authentication viewer) {
        if (limit < 1 || limit > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 50");
        }
        Limit oneMore = Limit.of(limit + 1);
        List<Comment> found = cursor == null
                ? comments.findByArticleSlugOrderByIdDesc(slug, oneMore)
                : comments.findByArticleSlugAndIdLessThanOrderByIdDesc(slug, decode(cursor), oneMore);
        List<Comment> page = found.subList(0, Math.min(limit, found.size()));
        String next = found.size() > limit ? encode(page.getLast().getId()) : null;
        return new Page(page.stream().map(comment -> view(comment, viewer)).toList(), next);
    }

    @PostMapping("/api/articles/{slug}/comments")
    ResponseEntity<CommentView> create(@PathVariable String slug, @Valid @RequestBody NewComment input,
                                       Authentication viewer) {
        Comment saved = comments.save(new Comment(slug, viewer.getName(), input.body().strip(), Instant.now()));
        return ResponseEntity.created(URI.create("/api/comments/" + saved.getId())).body(view(saved, viewer));
    }

    @DeleteMapping("/api/comments/{id}")
    ResponseEntity<Map<String, String>> delete(@PathVariable long id, Authentication viewer) {
        Comment comment = comments.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "no comment " + id));
        if (!canDelete(comment, viewer)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "only the author or an admin may delete it");
        }
        comments.delete(comment);
        return ResponseEntity.noContent().build();
    }

    private static boolean canDelete(Comment comment, Authentication viewer) {
        if (viewer == null || !viewer.isAuthenticated()) {
            return false;
        }
        boolean admin = viewer.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return admin || comment.getAuthor().equals(viewer.getName());
    }

    private static CommentView view(Comment comment, Authentication viewer) {
        return new CommentView(comment.getId(), comment.getAuthor(), comment.getBody(), comment.getCreatedAt(),
                canDelete(comment, viewer));
    }

    static String encode(long id) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(Long.toString(id).getBytes(StandardCharsets.UTF_8));
    }

    static long decode(String cursor) {
        try {
            return Long.parseLong(new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid cursor");
        }
    }
}
