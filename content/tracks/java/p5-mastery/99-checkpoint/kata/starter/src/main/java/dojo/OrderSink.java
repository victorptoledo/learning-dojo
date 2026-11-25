package dojo;

/** Where processed orders go (a database in production). Already written; the tests bring their own. */
public interface OrderSink {
    void store(Order order, String customerName) throws Exception;
}
