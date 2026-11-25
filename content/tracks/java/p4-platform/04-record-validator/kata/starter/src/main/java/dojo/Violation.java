package dojo;

/** One broken rule: the path of the component ("address.city", "lines[1].sku") and why. */
public record Violation(String path, String message) {}
