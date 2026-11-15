package dojo;

public record Or(CustomerSpec left, CustomerSpec right) implements CustomerSpec {}
