package dojo;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The /todos endpoints. Write one method per endpoint of the README. */
@RestController
@RequestMapping("/todos")
public class TodoController {
    private final TodoService todos;

    public TodoController(TodoService todos) {
        this.todos = todos;
    }
}
