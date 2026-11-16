package dojo;

import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

/** The two queries of the page, newest first with a row limit. */
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByArticleSlugOrderByIdDesc(String articleSlug, Limit limit);

    List<Comment> findByArticleSlugAndIdLessThanOrderByIdDesc(String articleSlug, long id, Limit limit);
}
