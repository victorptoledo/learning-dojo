package dojo;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/** The /todos endpoints. Write one method per endpoint of the README. */
@RestController
@RequestMapping("/todos")
public class TodoController {
    private final TodoService todos;

    public TodoController(TodoService todos) {
        this.todos = todos;
    }

    @GetMapping
    public List<Todo> list(@RequestParam(required = false) Boolean done) {
        return todos.list(done);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Todo> one(@PathVariable long id) {
        return ResponseEntity.of(todos.find(id));
    }

    @PostMapping
    public ResponseEntity<Todo> create(@RequestBody NewTodo body, UriComponentsBuilder uri) {
        if (body.title() == null || body.title().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Todo created = todos.create(body.title());
        URI location = uri.path("/todos/{id}").build(created.id());
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Todo> update(@PathVariable long id, @RequestBody TodoUpdate body) {
        if (body.title() == null || body.title().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.of(todos.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return todos.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
