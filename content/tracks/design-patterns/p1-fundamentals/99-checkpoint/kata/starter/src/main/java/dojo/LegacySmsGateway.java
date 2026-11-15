package dojo;

/**
 * The telecom operator's client library. You cannot change it. Numbers are digits only
 * ("351912345678", no "+" or spaces), bodies are at most 160 characters, and a failed send
 * returns {@code false} instead of throwing.
 */
public class LegacySmsGateway {
    public boolean sendSms(String digits, String body) {
        return digits.chars().allMatch(Character::isDigit) && body.length() <= 160;
    }
}
