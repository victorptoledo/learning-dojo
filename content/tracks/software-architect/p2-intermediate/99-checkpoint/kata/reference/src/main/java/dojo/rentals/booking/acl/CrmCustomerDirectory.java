package dojo.rentals.booking.acl;

import dojo.legacy.crm.CrmClient;
import dojo.rentals.booking.domain.Customer;
import dojo.rentals.booking.domain.CustomerDirectory;
import dojo.rentals.booking.domain.Standing;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** The anti-corruption layer: the only class that speaks the CRM's language. */
public class CrmCustomerDirectory implements CustomerDirectory {
    private final CrmClient crm;

    public CrmCustomerDirectory(CrmClient crm) {
        this.crm = crm;
    }

    @Override
    public Optional<Customer> find(String customerId) {
        Map<String, String> record = crm.fetchCustomer(customerId);
        if (record == null) {
            return Optional.empty();
        }
        return Optional.of(new Customer(
                customerId,
                displayName(record.get("CUST_NM")),
                standing(record.get("STAT_CD")),
                Long.parseLong(record.get("CRDT_LMT")),
                "02".equals(record.get("SEG"))));
    }

    static String displayName(String crmName) {
        String[] parts = crmName.split(",\\s*", 2);
        String given = parts.length == 2 ? parts[1] : "";
        return (capitalised(given) + " " + capitalised(parts[0])).strip();
    }

    private static String capitalised(String words) {
        return Arrays.stream(words.strip().split("\\s+"))
                .filter(word -> !word.isEmpty())
                .map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
    }

    private static Standing standing(String code) {
        return switch (code) {
            case "A" -> Standing.GOOD;
            case "S" -> Standing.SUSPENDED;
            case "X" -> Standing.CLOSED;
            default -> throw new IllegalStateException("unknown CRM status " + code);
        };
    }
}
