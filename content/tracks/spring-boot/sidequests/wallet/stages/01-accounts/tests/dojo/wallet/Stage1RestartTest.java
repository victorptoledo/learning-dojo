package dojo.wallet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Starts the real application twice on one file database: the data must survive. */
class Stage1RestartTest {
    @TempDir
    Path folder;

    final HttpClient http = HttpClient.newHttpClient();

    ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(WalletApplication.class)
                .properties(
                        "server.port=0",
                        "spring.datasource.url=jdbc:h2:file:" + folder.resolve("wallet").toAbsolutePath()
                                .toString().replace('\\', '/'),
                        "spring.jpa.hibernate.ddl-auto=update")
                .run();
    }

    String base(ConfigurableApplicationContext context) {
        return "http://127.0.0.1:" + context.getEnvironment().getProperty("local.server.port");
    }

    HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return http.send(request.header("Content-Type", "application/json").build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void accounts_and_balances_survive_a_restart() throws Exception {
        long id;
        try (ConfigurableApplicationContext first = start()) {
            HttpResponse<String> created = send(HttpRequest.newBuilder(URI.create(base(first) + "/accounts"))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"owner\": \"restart\", \"currency\": \"EUR\"}")));
            assertEquals(201, created.statusCode(), created.body());
            id = ((Number) JsonPath.read(created.body(), "$.id")).longValue();
            HttpResponse<String> deposited = send(HttpRequest.newBuilder(
                    URI.create(base(first) + "/accounts/" + id + "/deposits"))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"amountCents\": 4200}")));
            assertEquals(200, deposited.statusCode(), deposited.body());
        }
        try (ConfigurableApplicationContext second = start()) {
            HttpResponse<String> account = send(HttpRequest.newBuilder(URI.create(base(second) + "/accounts/" + id)).GET());
            assertEquals(200, account.statusCode(), "the account is gone after a restart: keep it in the database");
            assertEquals(4200, ((Number) JsonPath.read(account.body(), "$.balanceCents")).longValue());
            assertTrue(account.body().contains("restart"));
        }
    }
}
