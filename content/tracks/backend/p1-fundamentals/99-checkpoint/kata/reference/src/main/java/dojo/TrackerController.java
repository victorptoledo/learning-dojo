package dojo;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class TrackerController {
    private final TrackerService tracker;

    public TrackerController(TrackerService tracker) {
        this.tracker = tracker;
    }

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public void createProject(@Valid @RequestBody NewProject request) {
        tracker.createProject(request);
    }

    @PostMapping("/projects/{key}/issues")
    public ResponseEntity<IssueView> createIssue(@PathVariable String key, @Valid @RequestBody NewIssue request,
                                                 UriComponentsBuilder uri) {
        IssueView issue = tracker.createIssue(key, request);
        return ResponseEntity.created(uri.path("/projects/{key}/issues/{number}").build(key, issue.number()))
                .eTag(etag(issue)).body(issue);
    }

    @GetMapping("/projects/{key}/issues")
    public IssuePage list(@PathVariable String key,
                          @RequestParam(required = false) IssueStatus status,
                          @RequestParam(required = false) String assignee,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size) {
        return tracker.list(key, status, assignee, page, size);
    }

    @GetMapping("/projects/{key}/issues/{number}")
    public ResponseEntity<IssueView> get(@PathVariable String key, @PathVariable int number) {
        IssueView issue = tracker.get(key, number);
        return ResponseEntity.ok().eTag(etag(issue)).body(issue);
    }

    @PatchMapping(value = "/projects/{key}/issues/{number}", consumes = {"application/merge-patch+json", "application/json"})
    public ResponseEntity<IssueView> patch(@PathVariable String key, @PathVariable int number,
                                           @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                           @RequestBody Map<String, Object> patch) {
        IssueView issue = tracker.patch(key, number, version(ifMatch), patch);
        return ResponseEntity.ok().eTag(etag(issue)).body(issue);
    }

    private static String etag(IssueView issue) {
        return "\"" + issue.version() + "\"";
    }

    /** "\"3\"" → 3; null stays null; anything else is a version that can never match. */
    private static Long version(String ifMatch) {
        if (ifMatch == null) {
            return null;
        }
        try {
            return Long.parseLong(ifMatch.replace("\"", "").strip());
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
