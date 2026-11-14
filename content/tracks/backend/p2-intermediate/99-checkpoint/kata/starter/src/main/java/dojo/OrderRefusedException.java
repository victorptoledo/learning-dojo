package dojo;

public class OrderRefusedException extends RuntimeException {
    public OrderRefusedException(String customer) {
        super("orders from " + customer + " are refused");
    }
}
