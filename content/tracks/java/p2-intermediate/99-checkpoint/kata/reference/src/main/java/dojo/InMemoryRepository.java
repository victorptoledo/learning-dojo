package dojo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** Keeps items by id, in the order they were first saved. */
public class InMemoryRepository<ID, T extends Identified<ID>> {
    private final Map<ID, T> items = new LinkedHashMap<>();

    /** Stores {@code item}, replacing any item with the same id, and returns it. */
    public T save(T item) {
        items.put(item.id(), item);
        return item;
    }

    public Optional<T> findById(ID id) {
        return Optional.ofNullable(items.get(id));
    }

    /** Every item, in first-saved order; unmodifiable. */
    public List<T> findAll() {
        return List.copyOf(items.values());
    }

    /** The items that match, in first-saved order; unmodifiable. */
    public List<T> findAll(Predicate<? super T> filter) {
        return items.values().stream().filter(filter).toList();
    }

    public boolean delete(ID id) {
        return items.remove(id) != null;
    }

    public int count() {
        return items.size();
    }
}
