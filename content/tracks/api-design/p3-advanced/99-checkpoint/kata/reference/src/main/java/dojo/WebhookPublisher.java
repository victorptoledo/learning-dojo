package dojo;

import dojo.Partners.Partner;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Signed webhooks (Standard Webhooks), delivered at least once with exponential backoff. */
@Component
public class WebhookPublisher {
    static final List<Duration> RETRY_DELAYS =
            List.of(Duration.ofSeconds(10), Duration.ofSeconds(20), Duration.ofSeconds(40), Duration.ofSeconds(80));

    private final WebhookSender sender;
    private final BackgroundJobs jobs;
    private final Ticker ticker;
    private final JsonMapper json;
    private final AtomicLong ids = new AtomicLong();

    public WebhookPublisher(WebhookSender sender, BackgroundJobs jobs, Ticker ticker, JsonMapper json) {
        this.sender = sender;
        this.jobs = jobs;
        this.ticker = ticker;
        this.json = json;
    }

    void publish(Partner partner, String type, Map<String, Object> data) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("id", "evt_" + ids.incrementAndGet());
        event.put("type", type);
        event.put("createdAt", ticker.now().toString());
        event.put("data", data);
        attempt(partner, (String) event.get("id"), json.writeValueAsString(event), 0);
    }

    private void attempt(Partner partner, String id, String body, int retries) {
        String timestamp = String.valueOf(ticker.now().getEpochSecond());
        Map<String, String> headers = Map.of(
                "content-type", "application/json",
                "webhook-id", id,
                "webhook-timestamp", timestamp,
                "webhook-signature", sign(partner.webhookSecret(), id + "." + timestamp + "." + body));
        int status = sender.deliver(partner.id(), headers, body);
        if (status / 100 != 2 && retries < RETRY_DELAYS.size()) {
            jobs.schedule(RETRY_DELAYS.get(retries), () -> attempt(partner, id, body, retries + 1));
        }
    }

    static String sign(String secret, String content) {
        try {
            byte[] key = Base64.getDecoder().decode(secret.substring("whsec_".length()));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return "v1," + Base64.getEncoder().encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
