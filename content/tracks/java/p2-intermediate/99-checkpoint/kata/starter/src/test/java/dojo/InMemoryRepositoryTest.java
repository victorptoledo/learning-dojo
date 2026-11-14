package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class InMemoryRepositoryTest {
    @Test
    void finds_what_was_saved() {
        var repository = new InMemoryRepository<String, Book>();
        var dune = new Book("111", "Dune", "Herbert");
        repository.save(dune);
        assertEquals(Optional.of(dune), repository.findById("111"));
    }
}
