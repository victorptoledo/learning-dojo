package dojo;

import java.util.List;

/** A WHERE clause with ? placeholders and the values that go in them, in order. */
public record SqlWhere(String clause, List<Object> params) {
    public SqlWhere {
        params = List.copyOf(params);
    }
}
