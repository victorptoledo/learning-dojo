package dojo;

import java.util.List;

/** One page of the feed; nextCursor is null on the last page. */
public record FeedPage(List<Event> data, String nextCursor) {}
