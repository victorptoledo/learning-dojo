package dojo;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class LibraryController {
    private final LibraryService library;

    public LibraryController(LibraryService library) {
        this.library = library;
    }
}
