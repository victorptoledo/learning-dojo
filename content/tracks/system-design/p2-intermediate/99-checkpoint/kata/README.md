# Hybrid Timeline

`TimelineService` builds home timelines with **hybrid fan-out**:

- A post by a **normal** author (fewer than `celebrityThreshold` followers when posting) is
  **pushed**: its id goes into the inbox of every current follower. Each inbox keeps only the
  newest `inboxSize` posts.
- A post by a **celebrity** (at least `celebrityThreshold` followers when posting) is **not**
  pushed: it is kept in the author's list of pulled posts and merged in at read time.

```java
var timelines = new TimelineService(1000, 800, clock);
timelines.follow("ada", "linus");
Post post = timelines.post("linus");
timelines.timeline("ada", 20);   // [post]
```

## API

| Method | Behaviour |
| --- | --- |
| `TimelineService(celebrityThreshold, inboxSize, clock)` | both at least 1, else `IllegalArgumentException` |
| `follow(follower, followee)` | idempotent; following yourself throws `IllegalArgumentException` |
| `unfollow(follower, followee)` | idempotent |
| `followers(user)` | current number of followers |
| `post(author)` | creates `Post(id, author, at)`: ids are 1, 2, 3, ... and `at` comes from the clock |
| `timeline(user, limit)` | the newest `limit` posts of the accounts the user **currently** follows |
| `inboxWrites()` | how many inbox insertions fan-out has done (the cost of pushing) |

## The timeline

1. Take the user's inbox, **keeping only posts of authors the user still follows** (unfollows are
   applied at read time; nothing is rewritten).
2. Add the pulled posts of every followed author who has any (at most `limit` of the newest of
   each is enough).
3. Sort newest first by `at`, ties by **higher id first**, and return the first `limit`. A
   `limit` of 0 gives an empty list; a negative one throws `IllegalArgumentException`.

A post is either pushed or pulled, never both, so nothing appears twice even when an author
crosses the threshold. Your own posts are not in your home timeline. Following someone does not
backfill: you see their pushed posts from then on (pulled posts appear at once, since they are
read at read time).

> Tip: keep `followers` and `following` as `Map<String, Set<String>>`, inboxes as
> `Map<String, ArrayDeque<Post>>` (add last, remove first when over `inboxSize`) and pulled posts
> as `Map<String, List<Post>>`. A `Comparator.comparing(Post::at).thenComparingLong(Post::id).reversed()`
> sorts the timeline.
