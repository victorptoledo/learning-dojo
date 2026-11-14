package dojo;

public record LinkView(String code, String url, long clicks) {
    public static LinkView of(ShortLink link) {
        return new LinkView(link.getCode(), link.getTargetUrl(), link.getClicks());
    }
}
