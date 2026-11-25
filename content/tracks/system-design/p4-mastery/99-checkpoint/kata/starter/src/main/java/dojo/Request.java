package dojo;

public record Request(String tenant, Priority priority, String body) {}
