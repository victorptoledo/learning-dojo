package dojo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {
    @EntityGraph(attributePaths = "author")
    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    @Query("select b from Book b join fetch b.author where lower(b.title) like lower(concat('%', :text, '%')) order by b.title")
    List<Book> search(@Param("text") String text);

    @EntityGraph(attributePaths = "author")
    Page<Book> findByAuthorName(String name, Pageable pageable);

    @Query("select new dojo.AuthorStats(a.name, count(b)) from Book b join b.author a group by a.name order by count(b) desc, a.name")
    List<AuthorStats> stats();
}
