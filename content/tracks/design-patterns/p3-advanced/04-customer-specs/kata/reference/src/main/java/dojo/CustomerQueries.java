package dojo;

import java.util.ArrayList;
import java.util.List;

/** The SQL interpreter of a {@link CustomerSpec}: values always travel as parameters. */
public final class CustomerQueries {
    private CustomerQueries() {}

    public static SqlWhere where(CustomerSpec spec) {
        List<Object> params = new ArrayList<>();
        String clause = render(spec, params);
        return new SqlWhere(clause, params);
    }

    private static String render(CustomerSpec spec, List<Object> params) {
        return switch (spec) {
            case IsActive active -> "active = TRUE";
            case InCountry(String country) -> {
                params.add(country);
                yield "country = ?";
            }
            case MinOrders(int count) -> {
                params.add(count);
                yield "orders >= ?";
            }
            case And(CustomerSpec left, CustomerSpec right) ->
                    "(" + render(left, params) + " AND " + render(right, params) + ")";
            case Or(CustomerSpec left, CustomerSpec right) ->
                    "(" + render(left, params) + " OR " + render(right, params) + ")";
            case Not(CustomerSpec inner) -> "NOT " + wrap(render(inner, params));
        };
    }

    private static String wrap(String clause) {
        return clause.startsWith("(") ? clause : "(" + clause + ")";
    }
}
