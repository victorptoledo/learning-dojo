package dojo.shortener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dojo.shortener.Fakes.ManualClock;
import dojo.shortener.Fakes.MemoryAllocator;
import dojo.shortener.Fakes.MemoryStore;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Stage1CodesTest {

    private final ManualClock clock = new ManualClock();
    private final MemoryStore store = new MemoryStore();

    private Shortener shortener(long firstId, int blockSize, int codeLength) {
        return new Shortener(store, new MemoryAllocator(firstId), clock, Fakes.config(blockSize, codeLength));
    }

    @Test
    void codesAreTheIdsInBase62PaddedToTheLength() {
        var s = shortener(1, 100, 6);
        assertEquals("000001", s.shorten("https://example.com/a"));
        assertEquals("000002", s.shorten("https://example.com/b"));
        var tens = shortener(9, 100, 6);
        assertEquals("000009", tens.shorten("https://example.com/9"));
        assertEquals("00000a", tens.shorten("https://example.com/10"));
        var high = shortener(61, 100, 6);
        assertEquals("00000Z", high.shorten("https://example.com/61"));
        assertEquals("000010", high.shorten("https://example.com/62"));
        var wrap = shortener(3843, 100, 6);
        assertEquals("0000ZZ", wrap.shorten("https://example.com/3843"));
        assertEquals("000100", wrap.shorten("https://example.com/3844"));
    }

    @Test
    void aNumberLongerThanTheLengthGetsALongerCode() {
        var s = shortener(3844, 10, 2);
        assertEquals("100", s.shorten("https://example.com/"));
        var big = shortener(56_800_235_584L, 10, 6); // 62^6
        assertEquals("1000000", big.shorten("https://example.com/"));
    }

    @Test
    void idsAreReservedInBlocksOnlyWhenNeeded() {
        var ids = new MemoryAllocator(1);
        var s = new Shortener(store, ids, clock, Fakes.config(100, 6));
        assertEquals(0, ids.reservations.get(), "nothing is reserved before the first link");
        for (int i = 0; i < 250; i++) {
            s.shorten("https://example.com/" + i);
        }
        assertEquals(3, ids.reservations.get(), "250 links with blocks of 100 cost 3 round trips");
        assertEquals(250, store.links.size());
    }

    @Test
    void twoServersOnOneTicketServerNeverShareACode() {
        var ids = new MemoryAllocator(1);
        var a = new Shortener(store, ids, clock, Fakes.config(100, 6));
        var b = new Shortener(store, ids, clock, Fakes.config(100, 6));
        assertEquals("000001", a.shorten("https://a.example/1"));
        assertEquals("00001D", b.shorten("https://b.example/1"), "b got the block that starts at 101");
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 300; i++) {
            codes.add(a.shorten("https://a.example/" + i));
            codes.add(b.shorten("https://b.example/" + i));
        }
        assertEquals(600, codes.size());
        assertEquals(602, store.links.size());
    }

    @Test
    void resolveFindsTheLongUrlAndCodesAreCaseSensitive() {
        var s = shortener(10, 100, 6);
        String lower = s.shorten("https://example.com/lower");
        assertEquals("00000a", lower);
        assertEquals(Optional.of("https://example.com/lower"), s.resolve("00000a"));
        assertEquals(Optional.empty(), s.resolve("00000A"));
        assertEquals(Optional.empty(), s.resolve("nobody"));
    }

    @Test
    void theSameUrlTwiceGivesTwoCodes() {
        var s = shortener(1, 100, 6);
        assertNotEquals(s.shorten("https://example.com/"), s.shorten("https://example.com/"));
    }

    @Test
    void invalidUrlsAreRefusedAndCostNoId() {
        var s = shortener(1, 100, 6);
        for (String bad : new String[] {
            "ftp://example.com/file", "example.com", "not a url", "https://", "mailto:ana@example.com",
            "https://example.com/" + "x".repeat(2030), ""
        }) {
            assertThrows(IllegalArgumentException.class, () -> s.shorten(bad), bad);
        }
        assertThrows(IllegalArgumentException.class, () -> s.shorten(null));
        assertEquals(0, store.puts.get(), "nothing reaches the store");
        assertEquals("000001", s.shorten("HTTPS://Example.com/ok"), "the first good link gets the first id");
        assertEquals("000002", s.shorten("http://example.com/" + "y".repeat(2029)), "2048 characters is fine");
    }

    @Test
    void anAliasIsTheCode() {
        var s = shortener(1, 100, 6);
        assertEquals("spring-sale_2026", s.shorten("https://shop.example/sale", "spring-sale_2026"));
        assertEquals(Optional.of("https://shop.example/sale"), s.resolve("spring-sale_2026"));
        assertEquals("abc", s.shorten("https://example.com/abc", "abc"));
    }

    @Test
    void badAliasesAndBadUrlsWithAliasesAreRefused() {
        var s = shortener(1, 100, 6);
        for (String bad : new String[] {"ab", "x".repeat(33), "has space", "dot.dot", "slash/", "ação", ""}) {
            assertThrows(IllegalArgumentException.class, () -> s.shorten("https://example.com/", bad), bad);
        }
        assertThrows(IllegalArgumentException.class, () -> s.shorten("https://example.com/", null));
        assertThrows(IllegalArgumentException.class, () -> s.shorten("gopher://example.com/", "fine-alias"));
        assertEquals(0, store.links.size());
    }

    @Test
    void aTakenAliasIsRefusedAndTheFirstLinkStays() {
        var s = shortener(1, 100, 6);
        s.shorten("https://example.com/first", "promo");
        assertThrows(IllegalStateException.class, () -> s.shorten("https://evil.example/", "promo"));
        assertEquals(Optional.of("https://example.com/first"), s.resolve("promo"));
        s.shorten("https://example.com/x");
        assertThrows(IllegalStateException.class, () -> s.shorten("https://evil.example/", "000001"));
        assertEquals(Optional.of("https://example.com/x"), s.resolve("000001"));
    }

    @Test
    void aGeneratedCodeTakenByAnAliasIsSkipped() {
        var s = shortener(1, 100, 6);
        s.shorten("https://example.com/alias", "000002");
        assertEquals("000001", s.shorten("https://example.com/1"));
        assertEquals("000003", s.shorten("https://example.com/3"));
        assertEquals(Optional.of("https://example.com/alias"), s.resolve("000002"));
    }
}
