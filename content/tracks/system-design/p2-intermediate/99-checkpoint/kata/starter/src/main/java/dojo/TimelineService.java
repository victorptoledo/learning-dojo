package dojo;

import java.time.Clock;
import java.util.List;

/** Home timelines with hybrid fan-out: push for normal authors, pull for celebrities. */
public final class TimelineService {

    public TimelineService(int celebrityThreshold, int inboxSize, Clock clock) {
        throw new UnsupportedOperationException("todo timelines with threshold " + celebrityThreshold);
    }

    public void follow(String follower, String followee) {
        throw new UnsupportedOperationException("todo " + follower + " follows " + followee);
    }

    public void unfollow(String follower, String followee) {
        throw new UnsupportedOperationException("todo " + follower + " unfollows " + followee);
    }

    public int followers(String user) {
        throw new UnsupportedOperationException("todo followers of " + user);
    }

    /** Pushes to follower inboxes, or keeps the post for pulling when the author is a celebrity. */
    public Post post(String author) {
        throw new UnsupportedOperationException("todo post by " + author);
    }

    /** The newest posts of the accounts the user currently follows. */
    public List<Post> timeline(String user, int limit) {
        throw new UnsupportedOperationException("todo timeline of " + user);
    }

    /** Inbox insertions done by fan-out so far. */
    public long inboxWrites() {
        throw new UnsupportedOperationException("todo inbox writes");
    }
}
