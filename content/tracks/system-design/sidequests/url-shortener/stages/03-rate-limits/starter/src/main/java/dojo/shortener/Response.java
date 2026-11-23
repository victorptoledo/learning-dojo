package dojo.shortener;

import java.util.Map;

/** An HTTP-like answer: the status code, the body and the headers. */
public record Response(int status, String body, Map<String, String> headers) {
}
