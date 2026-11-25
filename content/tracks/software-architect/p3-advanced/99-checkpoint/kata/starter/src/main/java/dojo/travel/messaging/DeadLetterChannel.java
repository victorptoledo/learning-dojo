package dojo.travel.messaging;

/** Where replies that cannot be processed go, so a person can look at them. Given: do not change. */
public interface DeadLetterChannel {
    void deadLetter(SupplierReply reply, String reason);
}
