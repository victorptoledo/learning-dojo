package dojo;

/** POST /links: the target url, and an optional custom code. */
public record NewLink(String url, String code) {}
