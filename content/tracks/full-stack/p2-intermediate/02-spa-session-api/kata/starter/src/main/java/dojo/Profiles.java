package dojo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Each user's display name, in memory; the username until it is changed. */
@Component
public class Profiles {
    private final Map<String, String> displayNames = new ConcurrentHashMap<>();

    public String displayName(String username) {
        return displayNames.getOrDefault(username, username);
    }

    public void rename(String username, String displayName) {
        displayNames.put(username, displayName);
    }
}
