package dojo;

import java.util.Objects;

public record InCountry(String country) implements CustomerSpec {
    public InCountry {
        Objects.requireNonNull(country, "country");
    }
}
