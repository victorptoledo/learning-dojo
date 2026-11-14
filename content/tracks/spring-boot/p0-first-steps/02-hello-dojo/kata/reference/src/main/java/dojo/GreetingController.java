package dojo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {
    @GetMapping("/greet")
    public String greet(@RequestParam(required = false) String name) {
        String who = name == null || name.isBlank() ? "stranger" : name.strip();
        return "Welcome to the dojo, %s!".formatted(who);
    }
}
