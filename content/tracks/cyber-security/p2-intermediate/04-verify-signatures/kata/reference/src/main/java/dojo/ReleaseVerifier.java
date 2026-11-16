package dojo;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Verifies Ed25519-signed release files against trusted, revocable key ids. */
public final class ReleaseVerifier {
    private static final int SIGNATURE_BYTES = 64;

    private final Map<String, PublicKey> trustedKeys = new HashMap<>();

    public ReleaseVerifier(Map<String, String> trustedKeys) {
        trustedKeys.forEach((id, encoded) -> this.trustedKeys.put(id, ed25519Key(encoded)));
    }

    private static PublicKey ed25519Key(String encoded) {
        try {
            byte[] der = Base64.getDecoder().decode(encoded);
            PublicKey key = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(der));
            if (!key.getAlgorithm().equals("EdDSA") && !key.getAlgorithm().equals("Ed25519")) {
                throw new IllegalArgumentException("not an Ed25519 key");
            }
            return key;
        } catch (GeneralSecurityException | RuntimeException e) {
            throw new IllegalArgumentException("not a Base64 Ed25519 public key", e);
        }
    }

    /** The key id that validly signed artifact, or empty. */
    public Optional<String> verify(byte[] artifact, String signatureFile) {
        try {
            String[] lines = signatureFile.replace("\r\n", "\n").split("\n");
            if (lines.length != 2 || !lines[0].startsWith("key-id: ") || !lines[1].startsWith("signature: ")) {
                return Optional.empty();
            }
            String keyId = lines[0].substring("key-id: ".length());
            PublicKey key = trustedKeys.get(keyId);
            if (key == null) {
                return Optional.empty();
            }
            byte[] signature = Base64.getDecoder().decode(lines[1].substring("signature: ".length()));
            if (signature.length != SIGNATURE_BYTES) {
                return Optional.empty();
            }
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(key);
            verifier.update(artifact);
            return verifier.verify(signature) ? Optional.of(keyId) : Optional.empty();
        } catch (GeneralSecurityException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /** Stops trusting a key, for example after it leaked. */
    public void revoke(String keyId) {
        trustedKeys.remove(keyId);
    }
}
