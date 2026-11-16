package dojo;

/**
 * One violation of the style guide.
 *
 * @param rule    the rule id, for example "operation-id"
 * @param pointer where it is, as an RFC 6901 JSON Pointer, for example "/paths/~1orders/get"
 * @param message what is wrong, for a human
 */
public record Finding(String rule, String pointer, String message) {}
