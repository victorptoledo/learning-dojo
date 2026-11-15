package dojo;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Home timelines with hybrid fan-out: push for normal authors, pull for celebrities. */
public final class TimelineService {
    private static final Comparator<Post> NEWEST_FIRST =
            Comparator.comparing(Post::at).thenComparingLong(Post::id).reversed();

    private final int celebrityThreshold;
    private final int inboxSize;
    private final Clock clock;
    private final Map<String, Set<String>> followers = new HashMap<>();
    private final Map<String, Set<String>> following = new HashMap<>();
    private final Map<String, Deque<Post>> inboxes = new HashMap<>();
    private final Map<String, List<Post>> pulled = new HashMap<>();
    private long nextId = 1;
    private long inboxWrites;

    public TimelineService(int celebrityThreshold, int inboxSize, Clock clock) {
        if (celebrityThreshold < 1 || inboxSize < 1) {
            throw new IllegalArgumentException("threshold and inbox size must be at least 1");
        }
        this.celebrityThreshold = celebrityThreshold;
        this.inboxSize = inboxSize;
        this.clock = clock;
    }

    public void follow(String follower, String followee) {
        if (follower.equals(followee)) {
            throw new IllegalArgumentException("you cannot follow yourself");
        }
        followers.computeIfAbsent(followee, k -> new HashSet<>()).add(follower);
        following.computeIfAbsent(follower, k -> new HashSet<>()).add(followee);
    }

    public void unfollow(String follower, String followee) {
        followers.getOrDefault(followee, new HashSet<>()).remove(follower);
        following.getOrDefault(follower, new HashSet<>()).remove(followee);
    }

    public int followers(String user) {
        return followers.getOrDefault(user, Set.of()).size();
    }

    /** Pushes to follower inboxes, or keeps the post for pulling when the author is a celebrity. */
    public Post post(String author) {
        Post post = new Post(nextId++, author, clock.instant());
        Set<String> audience = followers.getOrDefault(author, Set.of());
        if (audience.size() >= celebrityThreshold) {
            pulled.computeIfAbsent(author, k -> new ArrayList<>()).add(post);
            return post;
        }
        for (String follower : audience) {
            Deque<Post> inbox = inboxes.computeIfAbsent(follower, k -> new ArrayDeque<>());
            inbox.addLast(post);
            if (inbox.size() > inboxSize) {
                inbox.removeFirst();
            }
            inboxWrites++;
        }
        return post;
    }

    /** The newest posts of the accounts the user currently follows. */
    public List<Post> timeline(String user, int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit cannot be negative");
        }
        Set<String> followees = following.getOrDefault(user, Set.of());
        List<Post> candidates = new ArrayList<>();
        for (Post post : inboxes.getOrDefault(user, new ArrayDeque<>())) {
            if (followees.contains(post.author())) {
                candidates.add(post);
            }
        }
        for (String followee : followees) {
            List<Post> posts = pulled.getOrDefault(followee, List.of());
            candidates.addAll(posts.subList(Math.max(0, posts.size() - limit), posts.size()));
        }
        candidates.sort(NEWEST_FIRST);
        return List.copyOf(candidates.subList(0, Math.min(limit, candidates.size())));
    }

    /** Inbox insertions done by fan-out so far. */
    public long inboxWrites() {
        return inboxWrites;
    }
}
