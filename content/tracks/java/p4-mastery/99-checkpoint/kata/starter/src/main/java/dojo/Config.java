package dojo;

import java.util.List;
import java.util.Map;

/** The service's settings, read from the environment. */
public record Config(int port, String dbUrl, String dbPassword, int workers, int cacheSize, List<String> enrichers) {

    public static Config fromEnv(Map<String, String> env) {
        int port = Integer.parseInt(env.getOrDefault("PORT", "8080"));
        String dbUrl = env.get("DB_URL");
        String dbPassword = env.get("DB_PASSWORD");
        int workers = Integer.parseInt(env.getOrDefault("WORKERS", "4"));
        int cacheSize = Integer.parseInt(env.getOrDefault("CACHE_SIZE", "1000"));
        String names = env.getOrDefault("ENRICHERS", "");
        List<String> enrichers = names.isBlank() ? List.of() : List.of(names.split(","));
        return new Config(port, dbUrl, dbPassword, workers, cacheSize, enrichers);
    }
}
