# Cursor Feed

Offset pagination is easy to write and wrong for a feed that grows while clients read it.
In this kata you replace `?offset=` with an opaque keyset cursor: the sort key of the last
event a client saw, plus the filter it was issued for, encoded so clients cannot build or
parse it. The grading tests insert events between two pages and walk the feed with pages
small enough to split ties, so only a real keyset survives. Press **Enter** to create the
kata folder and open it.
