package dojo;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Searches an account's statement entries. Inherited from the first version of the service: it works
 * for the requests the web app sends, and it is open to anything else. Fix it.
 */
@Component
public class EntrySearch {
    private final JdbcTemplate jdbc;

    public EntrySearch(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** {@code sort} is "bookedOn", "-bookedOn", "amount" or "-amount"; {@code q} is optional. */
    public List<EntryView> find(long accountId, String q, String sort, int size) {
        boolean descending = sort.startsWith("-");
        String field = descending ? sort.substring(1) : sort;
        String column = field.replaceAll("([A-Z])", "_$1").toLowerCase();
        String sql = "select id, booked_on, description, amount from entries where account_id = " + accountId
                + (q == null ? "" : " and lower(description) like '%" + q.toLowerCase() + "%'")
                + " order by " + column + (descending ? " desc" : " asc") + ", id"
                + " limit " + size;
        return jdbc.query(sql, (rs, row) -> new EntryView(rs.getLong("id"),
                rs.getObject("booked_on", java.time.LocalDate.class), rs.getString("description"), rs.getLong("amount")));
    }
}
