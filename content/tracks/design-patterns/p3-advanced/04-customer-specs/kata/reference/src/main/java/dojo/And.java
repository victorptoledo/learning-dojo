package dojo;

public record And(CustomerSpec left, CustomerSpec right) implements CustomerSpec {}
