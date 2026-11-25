package dojo;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.springframework.stereotype.Component;

/** Decides whether a flag is on for a user: the same user always lands in the same bucket. */
@Component
public class Rollout {
    private final FlagStore flags;

    public Rollout(FlagStore flags) {
        this.flags = flags;
    }

    public boolean enabled(String flag, String user) {
        return flags.get(flag)
                .map(f -> f.allow().contains(user) || bucket(flag, user) < f.percent())
                .orElse(false);
    }

    /** 0 to 99, from the CRC-32 of "flag:user" in UTF-8. */
    static int bucket(String flag, String user) {
        CRC32 crc = new CRC32();
        crc.update((flag + ":" + user).getBytes(StandardCharsets.UTF_8));
        return (int) (crc.getValue() % 100);
    }
}
