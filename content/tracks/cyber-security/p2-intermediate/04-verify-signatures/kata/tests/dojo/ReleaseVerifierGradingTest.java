package dojo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReleaseVerifierGradingTest {
    private static final byte[] ARTIFACT = "dojo-1.4.2.tar.gz contents".getBytes(StandardCharsets.UTF_8);
    private static final KeyPair RELEASE = pair("Ed25519");
    private static final KeyPair BACKUP = pair("Ed25519");
    private static final KeyPair STRANGER = pair("Ed25519");

    private static KeyPair pair(String algorithm) {
        try {
            return KeyPairGenerator.getInstance(algorithm).generateKeyPair();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static String pub(KeyPair pair) {
        return Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    }

    private static String sign(KeyPair pair, byte[] data) {
        try {
            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(pair.getPrivate());
            signer.update(data);
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static String file(String keyId, String signature) {
        return "key-id: " + keyId + "\nsignature: " + signature + "\n";
    }

    private static ReleaseVerifier verifier() {
        return new ReleaseVerifier(Map.of("release-2026", pub(RELEASE), "backup", pub(BACKUP)));
    }

    @Test
    void acceptsAValidSignatureAndNamesTheKey() {
        var v = verifier();
        assertEquals(Optional.of("release-2026"), v.verify(ARTIFACT, file("release-2026", sign(RELEASE, ARTIFACT))));
        assertEquals(Optional.of("backup"), v.verify(ARTIFACT, file("backup", sign(BACKUP, ARTIFACT))));
        String crlf = "key-id: backup\r\nsignature: " + sign(BACKUP, ARTIFACT) + "\r\n";
        assertEquals(Optional.of("backup"), v.verify(ARTIFACT, crlf));
    }

    @Test
    void rejectsTamperedArtifactsAndWrongKeys() {
        var v = verifier();
        String sig = sign(RELEASE, ARTIFACT);
        byte[] tampered = ARTIFACT.clone();
        tampered[0] ^= 1;
        assertEquals(Optional.empty(), v.verify(tampered, file("release-2026", sig)));
        assertEquals(Optional.empty(), v.verify(ARTIFACT, file("backup", sig)));
        assertEquals(Optional.empty(), v.verify(ARTIFACT, file("release-2026", sign(STRANGER, ARTIFACT))));
    }

    @Test
    void unknownKeyIdsAreNotTrusted() {
        var v = verifier();
        assertEquals(Optional.empty(), v.verify(ARTIFACT, file("stranger", sign(STRANGER, ARTIFACT))));
    }

    @Test
    void revokedKeysStopVerifying() {
        var v = verifier();
        String good = file("release-2026", sign(RELEASE, ARTIFACT));
        v.revoke("release-2026");
        v.revoke("never-existed");
        assertEquals(Optional.empty(), v.verify(ARTIFACT, good));
        assertEquals(Optional.of("backup"), v.verify(ARTIFACT, file("backup", sign(BACKUP, ARTIFACT))));
    }

    @Test
    void malformedSignatureFilesAreRejectedWithoutThrowing() {
        var v = verifier();
        String sig = sign(RELEASE, ARTIFACT);
        String[] bad = {
            "",
            "key-id: release-2026",
            "signature: " + sig,
            "signature: " + sig + "\nkey-id: release-2026",
            "key-id: release-2026\nsignature: " + sig + "\nextra: line",
            "key-id: release-2026\nsignature: not base64 !!!",
            "key-id: release-2026\nsignature: " + Base64.getEncoder().encodeToString(new byte[63]),
            "key-id: release-2026\nsignature: " + Base64.getEncoder().encodeToString(new byte[64]),
            "key-id:release-2026\nsignature: " + sig,
        };
        for (String text : bad) {
            assertDoesNotThrow(() -> v.verify(ARTIFACT, text), text);
            assertEquals(Optional.empty(), v.verify(ARTIFACT, text), text);
        }
    }

    @Test
    void onlyEd25519KeysCanBeTrusted() {
        String rsa = Base64.getEncoder().encodeToString(pair("RSA").getPublic().getEncoded());
        String ec = Base64.getEncoder().encodeToString(pair("EC").getPublic().getEncoded());
        assertThrows(IllegalArgumentException.class, () -> new ReleaseVerifier(Map.of("rsa", rsa)));
        assertThrows(IllegalArgumentException.class, () -> new ReleaseVerifier(Map.of("ec", ec)));
        assertThrows(IllegalArgumentException.class, () -> new ReleaseVerifier(Map.of("junk", "%%%")));
        assertThrows(IllegalArgumentException.class, () -> new ReleaseVerifier(Map.of("short", "AAAA")));
    }
}
