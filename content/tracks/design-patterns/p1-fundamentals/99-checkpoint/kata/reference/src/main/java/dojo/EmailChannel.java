package dojo;

/** Adapter: the first line of the text is the subject, the rest is the body. */
public final class EmailChannel implements Channel {
    private final Mailer mailer;

    public EmailChannel(Mailer mailer) {
        this.mailer = mailer;
    }

    @Override
    public void deliver(Message message) {
        String text = message.text();
        int newline = text.indexOf('\n');
        String subject = newline < 0 ? text : text.substring(0, newline);
        String body = newline < 0 ? "" : text.substring(newline + 1);
        mailer.send(message.to(), subject, body);
    }
}
