package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

class IngestServiceTest {
    private static final Map<String, String> ENV = Map.of(
            "DB_URL", "jdbc:postgresql://db:5432/orders", "DB_PASSWORD", "local-only");

    @Test
    void parses_an_order_line() {
        assertEquals(new Order("o-1", "c-7", "ABC-1234", 3, 1999), OrderParser.parse("o-1;c-7;ABC-1234;3;1999"));
    }

    @Test
    void config_has_defaults() {
        Config config = Config.fromEnv(ENV);
        assertEquals(8080, config.port());
        assertEquals(4, config.workers());
    }

    @Test
    void stores_an_order_with_the_customer_name() throws Exception {
        List<String> stored = new CopyOnWriteArrayList<>();
        var service = new IngestService(Config.fromEnv(ENV), id -> "Ada",
                (order, name) -> stored.add(order.id() + " for " + name), System.getLogger("dojo.ingest"));
        service.submit("o-1;c-7;ABC-1234;3;1999");
        service.shutdown(Duration.ofSeconds(5));
        assertEquals(List.of("o-1 for Ada"), stored);
    }
}
