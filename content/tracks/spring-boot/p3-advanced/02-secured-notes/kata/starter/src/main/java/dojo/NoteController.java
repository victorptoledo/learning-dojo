package dojo;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class NoteController {
    private final NoteService notes;

    public NoteController(NoteService notes) {
        this.notes = notes;
    }
}
