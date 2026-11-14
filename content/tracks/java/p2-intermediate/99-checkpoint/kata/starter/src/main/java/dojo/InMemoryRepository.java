package dojo;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Keeps items by id, in the order they were first saved. */
public class InMemoryRepository<ID, T extends Identified<ID>> {

    /** Stores {@code item}, replacing any item with the same id, and returns it. */
    public T save(T item) {
        throw new UnsupportedOperationException("todo save " + item);
    }

    public Optional<T> findById(ID id) {
        throw new UnsupportedOperationException("todo find " + id);
    }

    /** Every item, in first-saved order; unmodifiable. */
    public List<T> findAll() {
        throw new UnsupportedOperationException("todo find all");
    }

    /** The items that match, in first-saved order; unmodifiable. */
    public List<T> findAll(Predicate<? super T> filter) {
        throw new UnsupportedOperationException("todo find matching");
    }

    public boolean delete(ID id) {
        throw new UnsupportedOperationException("todo delete " + id);
    }

    public int count() {
        throw new UnsupportedOperationException("todo count");
    }
}
