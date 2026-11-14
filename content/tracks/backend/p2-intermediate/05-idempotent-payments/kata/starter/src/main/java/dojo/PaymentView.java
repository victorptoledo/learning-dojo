package dojo;

public record PaymentView(long id, String account, long amountCents, String chargeId) {
    public static PaymentView of(Payment payment) {
        return new PaymentView(payment.getId(), payment.getAccount(), payment.getAmountCents(), payment.getChargeId());
    }
}
