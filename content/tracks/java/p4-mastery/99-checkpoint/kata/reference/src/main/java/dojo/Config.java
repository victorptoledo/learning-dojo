package dojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The service's settings, read from the environment and checked once, at startup. */
public record Config(int port, String dbUrl, String dbPassword, int workers, int cacheSize, List<String> enrichers) {

    public Config {
        enrichers = List.copyOf(enrichers);
    }

    public static Config fromEnv(Map<String, String> env) {
        List<String> problems = new ArrayList<>();
        int port = number(env, "PORT", 8080, 1, 65_535, problems);
        String dbUrl = value(env, "DB_URL");
        if (dbUrl == null) {
            problems.add("DB_URL: is required");
        } else if (!dbUrl.startsWith("jdbc:")) {
            problems.add("DB_URL: must start with jdbc:");
        }
        String dbPassword = value(env, "DB_PASSWORD");
        if (dbPassword == null) {
            problems.add("DB_PASSWORD: is required");
        }
        int workers = number(env, "WORKERS", 4, 1, 64, problems);
        int cacheSize = number(env, "CACHE_SIZE", 1_000, 1, 1_000_000, problems);
        List<String> enrichers = new ArrayList<>();
        String names = value(env, "ENRICHERS");
        if (names != null) {
            for (String name : names.split(",")) {
                String trimmed = name.strip();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (Enrichers.known().contains(trimmed)) {
                    enrichers.add(trimmed);
                } else {
                    problems.add("ENRICHERS: unknown enricher '" + trimmed + "' (known: "
                            + String.join(", ", Enrichers.known()) + ")");
                }
            }
        }
        if (!problems.isEmpty()) {
            throw new ConfigException(problems);
        }
        return new Config(port, dbUrl, dbPassword, workers, cacheSize, enrichers);
    }

    private static String value(Map<String, String> env, String key) {
        String raw = env.get(key);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.strip();
    }

    private static int number(Map<String, String> env, String key, int fallback, int min, int max, List<String> problems) {
        String raw = value(env, key);
        if (raw == null) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(raw);
            if (parsed >= min && parsed <= max) {
                return parsed;
            }
        } catch (NumberFormatException e) {
            // reported below
        }
        problems.add(key + ": must be a whole number from " + min + " to " + max + ", got '" + raw + "'");
        return fallback;
    }

    @Override
    public String toString() {
        return "Config[port=" + port + ", dbUrl=" + dbUrl + ", dbPassword=****, workers=" + workers
                + ", cacheSize=" + cacheSize + ", enrichers=" + enrichers + "]";
    }
}
