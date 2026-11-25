package dojo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Searches an account's statement entries with bound parameters and an allow-list for the order. */
@Component
public class EntrySearch {
    public static final int MAX_SIZE = 100;

    private static final Map<String, String> ORDER_BY = Map.of(
            "bookedOn", "booked_on asc",
            "-bookedOn", "booked_on desc",
            "amount", "amount asc",
            "-amount", "amount desc");

    private final JdbcTemplate jdbc;

    public EntrySearch(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<EntryView> find(long accountId, String q, String sort, int size) {
        String orderBy = ORDER_BY.get(sort);
        if (orderBy == null) {
            throw StatementsException.badRequest("sort must be one of " + ORDER_BY.keySet());
        }
        if (size < 1 || size > MAX_SIZE) {
            throw StatementsException.badRequest("size must be between 1 and " + MAX_SIZE);
        }
        var sql = new StringBuilder("select id, booked_on, description, amount from entries where account_id = ?");
        List<Object> args = new ArrayList<>(List.of(accountId));
        if (q != null && !q.isBlank()) {
            sql.append(" and lower(description) like ? escape '\\'");
            args.add("%" + escapeLike(q.toLowerCase(Locale.ROOT)) + "%");
        }
        sql.append(" order by ").append(orderBy).append(", id limit ?");
        args.add(size);
        return jdbc.query(sql.toString(), (rs, row) -> new EntryView(rs.getLong("id"),
                rs.getObject("booked_on", LocalDate.class), rs.getString("description"), rs.getLong("amount")),
                args.toArray());
    }

    /** A % or _ typed by the user is a character to find, not a wildcard. */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
