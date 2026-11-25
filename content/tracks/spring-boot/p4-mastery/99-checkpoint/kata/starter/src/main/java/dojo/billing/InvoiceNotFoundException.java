package dojo.billing;

public class InvoiceNotFoundException extends RuntimeException {
    public InvoiceNotFoundException(String number) {
        super("invoice " + number + " not found");
    }
}
