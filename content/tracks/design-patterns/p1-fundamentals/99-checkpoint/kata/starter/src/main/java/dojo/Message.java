package dojo;

/** A notification: the channel to use ("sms", "email" ...), the recipient and the text. */
public record Message(String channel, String to, String text) {}
