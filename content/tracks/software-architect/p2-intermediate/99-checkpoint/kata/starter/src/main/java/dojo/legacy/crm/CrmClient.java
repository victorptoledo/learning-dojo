package dojo.legacy.crm;

import java.util.HashMap;
import java.util.Map;

/** The legacy CRM's client library. Do not change it. */
public class CrmClient {
    private final Map<String, Map<String, String>> records = new HashMap<>();

    /** Adds a record, as the CRM's nightly import would. */
    public void load(String custNo, Map<String, String> record) {
        records.put(custNo, Map.copyOf(record));
    }

    /** The raw record, or {@code null} when the customer number is unknown. */
    public Map<String, String> fetchCustomer(String custNo) {
        return records.get(custNo);
    }
}
