package dojo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Verifies Ed25519-signed release files against trusted key ids. Today it checks nothing: fix it. */
public final class ReleaseVerifier {
    private final Map<String, String> trustedKeys;

    public ReleaseVerifier(Map<String, String> trustedKeys) {
        this.trustedKeys = new HashMap<>(trustedKeys);
    }

    /** The key id that validly signed artifact, or empty. */
    public Optional<String> verify(byte[] artifact, String signatureFile) {
        for (String line : signatureFile.split("\n")) {
            if (line.startsWith("key-id: ")) {
                return Optional.of(line.substring("key-id: ".length()).trim());
            }
        }
        return Optional.empty();
    }

    /** Stops trusting a key, for example after it leaked. */
    public void revoke(String keyId) {
        trustedKeys.remove(keyId);
    }
}
