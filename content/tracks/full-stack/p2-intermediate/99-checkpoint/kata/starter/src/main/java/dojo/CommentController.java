package dojo;

import org.springframework.web.bind.annotation.RestController;

/**
 * GET and POST /api/articles/{slug}/comments, DELETE /api/comments/{id} (see README.md).
 */
@RestController
public class CommentController {
    private final CommentRepository comments;

    public CommentController(CommentRepository comments) {
        this.comments = comments;
    }

    // todo: the three endpoints, the opaque cursor and canDelete
}
