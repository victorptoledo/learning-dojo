package dojo;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * todo: the two queries of the page, newest first with a row limit: the first page of an
 * article, and the comments of an article older than a given id. Spring Data derives them
 * from method names (OrderByIdDesc, IdLessThan) and takes a Limit parameter.
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {
}
