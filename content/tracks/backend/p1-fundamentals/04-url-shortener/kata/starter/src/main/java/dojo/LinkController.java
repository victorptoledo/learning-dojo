package dojo;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class LinkController {
    private final LinkService links;

    public LinkController(LinkService links) {
        this.links = links;
    }
}
