package dojo;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Keeps todos in memory. Ids start at 1 and are never reused. */
@Service
public class TodoService {

    public Todo create(String title) {
        throw new UnsupportedOperationException("todo create " + title);
    }

    /** Every todo, by id; only the open or the done ones when {@code done} is not null. */
    public List<Todo> list(Boolean done) {
        throw new UnsupportedOperationException("todo list " + done);
    }

    public Optional<Todo> find(long id) {
        throw new UnsupportedOperationException("todo find " + id);
    }

    /** The updated todo, or empty when there is no todo with that id. */
    public Optional<Todo> update(long id, TodoUpdate update) {
        throw new UnsupportedOperationException("todo update " + id);
    }

    /** True when a todo was deleted. */
    public boolean delete(long id) {
        throw new UnsupportedOperationException("todo delete " + id);
    }
}
