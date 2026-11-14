package dojo;

import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class LinkController {
    private final LinkService links;

    public LinkController(LinkService links) {
        this.links = links;
    }

    @PostMapping("/links")
    public ResponseEntity<LinkView> create(@RequestBody NewLink request, UriComponentsBuilder uri) {
        LinkView link = links.create(request);
        return ResponseEntity.created(uri.path("/links/{code}").build(link.code())).body(link);
    }

    @GetMapping("/links/top")
    public List<LinkView> top(@RequestParam(defaultValue = "10") int limit) {
        return links.top(limit);
    }

    @GetMapping("/links/{code}")
    public LinkView stats(@PathVariable String code) {
        return links.stats(code);
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> follow(@PathVariable String code) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(links.follow(code))).build();
    }
}
