Read requests properly. `Request` grows:

```cpp
struct Request {
    std::string method;
    std::string path;                                         // the target without the query
    std::map<std::string, std::string, std::less<>> query;    // ?a=1&b=2, not decoded
    std::map<std::string, std::string, std::less<>> headers;  // names in lower case
    std::string body;
};
```

- **Query.** `/search?q=cats&page=2&flag` has the path `/search` and the query `q=cats`,
  `page=2`, `flag=` (a name without `=` has an empty value). Nothing is percent-decoded.
- **Headers.** Each line after the request line is `Name: value`. Names are stored in lower
  case (HTTP header names are case-insensitive), values without the spaces around them.
- **Body.** With a `Content-Length` header, the body is exactly that many bytes after the blank
  line, and they may arrive later, in pieces: keep reading until you have them all. Without
  it, the body is empty.

Bad requests get **400** `Bad Request`: a request line that is not exactly
`METHOD /target HTTP/1.1` (or `HTTP/1.0`), a target that does not start with `/`, a header
line without `:` or with an empty or spaced name, or a `Content-Length` that is not a whole
number.

A path that has routes, but not for this method, gets **405** `Method Not Allowed`, with an
`Allow` header listing the methods it has, sorted and separated by `", "`
(`Allow: DELETE, GET, POST`). A path with no route at all is still 404.

A handler that throws gets **500** `Internal Server Error`, and the server goes on serving:
catch at the boundary between your server and the user's code, and nowhere else.

TCP is a stream, not messages: one `recv` may return half a header or two requests. Keep a
buffer, append what arrives, and look for what you need in the buffer.
