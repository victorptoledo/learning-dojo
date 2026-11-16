# Conditional Requests: a Product Page That Revalidates

Add validators to a product API (`ETag`, `If-None-Match` and `If-Match`) and write the
client cache that uses them: polls that cost a `304` instead of a download, and edits that turn
a silent lost update into a visible conflict. Press **Enter** to create the kata folder and open
it.
