package dojo;

/** Why a token was refused: "malformed", "unsupported algorithm", "bad signature" or "expired". */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String reason) {
        super(reason);
    }

    public String reason() {
        return getMessage();
    }
}
