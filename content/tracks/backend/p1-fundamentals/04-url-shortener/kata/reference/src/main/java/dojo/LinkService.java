package dojo;

import java.net.URI;
import java.security.SecureRandom;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinkService {
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9-]{3,20}");
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private final SecureRandom random = new SecureRandom();
    private final ShortLinkRepository links;

    public LinkService(ShortLinkRepository links) {
        this.links = links;
    }

    @Transactional
    public LinkView create(NewLink request) {
        String url = validUrl(request.url());
        String code = request.code() == null ? freeCode() : request.code().strip();
        if (!CODE.matcher(code).matches()) {
            throw LinkException.invalidCode(code);
        }
        if (links.existsByCode(code)) {
            throw LinkException.codeTaken(code);
        }
        return LinkView.of(links.save(new ShortLink(code, url)));
    }

    /** Counts one click and returns the target url. */
    @Transactional
    public String follow(String code) {
        if (links.addClick(code) == 0) {
            throw LinkException.notFound(code);
        }
        return links.findByCode(code).orElseThrow().getTargetUrl();
    }

    @Transactional(readOnly = true)
    public LinkView stats(String code) {
        return links.findByCode(code).map(LinkView::of).orElseThrow(() -> LinkException.notFound(code));
    }

    @Transactional(readOnly = true)
    public List<LinkView> top(int limit) {
        return links.mostClicked(PageRequest.of(0, limit)).stream().map(LinkView::of).toList();
    }

    private static String validUrl(String url) {
        if (url == null) {
            throw LinkException.invalidUrl(null);
        }
        try {
            URI uri = URI.create(url.strip());
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null) {
                throw LinkException.invalidUrl(url);
            }
            return uri.toString();
        } catch (IllegalArgumentException e) {
            throw LinkException.invalidUrl(url);
        }
    }

    private String freeCode() {
        while (true) {
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < 7; i++) {
                code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            if (!links.existsByCode(code.toString())) {
                return code.toString();
            }
        }
    }
}
