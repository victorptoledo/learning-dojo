package dojo;

/** Adapter: hides the operator library's digits-only numbers, 160-character limit and booleans. */
public final class SmsChannel implements Channel {
    static final int MAX_BODY = 160;
    private final LegacySmsGateway operator;

    public SmsChannel(LegacySmsGateway operator) {
        this.operator = operator;
    }

    @Override
    public void deliver(Message message) {
        String digits = message.to().replace("+", "").replace(" ", "");
        String body = message.text();
        if (body.length() > MAX_BODY) {
            body = body.substring(0, MAX_BODY - 3) + "...";
        }
        if (!operator.sendSms(digits, body)) {
            throw new DeliveryException("sms to " + message.to() + " failed");
        }
    }
}
