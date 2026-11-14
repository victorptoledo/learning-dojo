package dojo;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LinkService {
    private final ShortLinkRepository links;

    public LinkService(ShortLinkRepository links) {
        this.links = links;
    }

    public LinkView create(NewLink request) {
        throw new UnsupportedOperationException("todo create " + request);
    }

    /** Counts one click and returns the target url. */
    public String follow(String code) {
        throw new UnsupportedOperationException("todo follow " + code);
    }

    public LinkView stats(String code) {
        throw new UnsupportedOperationException("todo stats " + code);
    }

    public List<LinkView> top(int limit) {
        throw new UnsupportedOperationException("todo top " + limit);
    }
}
