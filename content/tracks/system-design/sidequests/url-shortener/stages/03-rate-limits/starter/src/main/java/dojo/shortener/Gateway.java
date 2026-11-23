package dojo.shortener;

import java.time.Clock;

/** The public face of the shortener, with rate limits per client. See README.md. */
public final class Gateway {

    public Gateway(Shortener shortener, Clock clock, Limits limits) {
    }

    public Response post(String client, String url, String alias) {
        throw new UnsupportedOperationException("todo: post");
    }

    public Response get(String client, String code) {
        throw new UnsupportedOperationException("todo: get");
    }

    public int trackedClients() {
        throw new UnsupportedOperationException("todo: trackedClients");
    }
}
