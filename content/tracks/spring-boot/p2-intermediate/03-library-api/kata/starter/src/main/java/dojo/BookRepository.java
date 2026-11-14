package dojo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
    // the queries of the README: by isbn, search (one query!), by author with a Pageable, stats
}
