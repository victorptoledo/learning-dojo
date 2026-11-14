package dojo;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/** Keeps todos in memory. Ids start at 1 and are never reused. */
@Service
public class TodoService {
    private final Map<Long, Todo> todos = new ConcurrentSkipListMap<>();
    private final AtomicLong lastId = new AtomicLong();

    public Todo create(String title) {
        Todo todo = new Todo(lastId.incrementAndGet(), title.strip(), false);
        todos.put(todo.id(), todo);
        return todo;
    }

    /** Every todo, by id; only the open or the done ones when {@code done} is not null. */
    public List<Todo> list(Boolean done) {
        return todos.values().stream().filter(todo -> done == null || todo.done() == done).toList();
    }

    public Optional<Todo> find(long id) {
        return Optional.ofNullable(todos.get(id));
    }

    /** The updated todo, or empty when there is no todo with that id. */
    public Optional<Todo> update(long id, TodoUpdate update) {
        return Optional.ofNullable(todos.computeIfPresent(id,
                (key, old) -> new Todo(id, update.title().strip(), update.done())));
    }

    /** True when a todo was deleted. */
    public boolean delete(long id) {
        return todos.remove(id) != null;
    }
}
