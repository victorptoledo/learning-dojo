package dojo.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class Stage2RoomPlanTest {

    private static Request r(String id, long start, long end) {
        return new Request(id, start, end);
    }

    private static List<String> ids(List<Request> requests) {
        return requests.stream().map(Request::id).toList();
    }

    /** The rule of the README, room by room: slow but obviously right. */
    private static Plan slowPlan(List<Request> requests) {
        var sorted = new ArrayList<>(requests);
        sorted.sort(Comparator.comparingLong(Request::start).thenComparing(Request::id));
        var roomEnds = new ArrayList<Long>();
        var roomOf = new HashMap<String, Integer>();
        for (Request request : sorted) {
            int room = -1;
            for (int i = 0; i < roomEnds.size() && room < 0; i++) {
                if (roomEnds.get(i) <= request.start()) {
                    room = i;
                }
            }
            if (room < 0) {
                room = roomEnds.size();
                roomEnds.add(0L);
            }
            roomEnds.set(room, request.end());
            roomOf.put(request.id(), room);
        }
        return new Plan(roomEnds.size(), roomOf);
    }

    /** The most meetings without overlap, by trying every subset. */
    private static int bruteForceMost(List<Request> requests) {
        int best = 0;
        for (int mask = 0; mask < 1 << requests.size(); mask++) {
            var chosen = new ArrayList<Request>();
            for (int i = 0; i < requests.size(); i++) {
                if ((mask >> i & 1) == 1) {
                    chosen.add(requests.get(i));
                }
            }
            chosen.sort(Comparator.comparingLong(Request::start));
            boolean ok = true;
            for (int i = 1; i < chosen.size(); i++) {
                ok &= chosen.get(i - 1).end() <= chosen.get(i).start();
            }
            if (ok) {
                best = Math.max(best, chosen.size());
            }
        }
        return best;
    }

    private static List<Request> randomRequests(Random random, int n, int span, int maxLength) {
        var requests = new ArrayList<Request>();
        for (int i = 0; i < n; i++) {
            long start = random.nextInt(span);
            requests.add(r("q" + i, start, start + 1 + random.nextInt(maxLength)));
        }
        return requests;
    }

    @Test
    void theReadmeExamples() {
        var plan = Planner.plan(List.of(r("a", 0, 30), r("b", 10, 40), r("c", 30, 60), r("d", 35, 50)));
        assertEquals(3, plan.rooms());
        assertEquals(Map.of("a", 0, "b", 1, "c", 0, "d", 2), plan.roomOf());

        var kept = Planner.mostMeetings(List.of(r("a", 0, 30), r("b", 10, 20), r("c", 20, 35), r("d", 30, 40)));
        assertEquals(List.of("b", "c"), ids(kept));
    }

    @Test
    void theLowestFreeRoomIsReused() {
        var plan = Planner.plan(List.of(
                r("x", 0, 10), r("y", 0, 20), r("z", 0, 30),
                r("late-z", 30, 40), r("late-x", 10, 15), r("tie-b", 20, 25), r("tie-a", 20, 22)));
        assertEquals(3, plan.rooms());
        assertEquals(0, plan.roomOf().get("x"), "ties at the same start go by id");
        assertEquals(1, plan.roomOf().get("y"));
        assertEquals(2, plan.roomOf().get("z"));
        assertEquals(0, plan.roomOf().get("late-x"));
        assertEquals(0, plan.roomOf().get("tie-a"), "rooms 0 and 1 are free at 20; tie-a comes first");
        assertEquals(1, plan.roomOf().get("tie-b"));
        assertEquals(0, plan.roomOf().get("late-z"), "every room is free at 30: the lowest one");
    }

    @Test
    void emptyAndSingleDays() {
        assertEquals(new Plan(0, Map.of()), Planner.plan(List.of()));
        assertEquals(List.of(), Planner.mostMeetings(List.of()));
        assertEquals(new Plan(1, Map.of("solo", 0)), Planner.plan(List.of(r("solo", 5, 6))));
    }

    @Test
    void mostMeetingsBreaksTiesByStartThenId() {
        var kept = Planner.mostMeetings(List.of(
                r("b", 5, 10), r("a", 5, 10), r("next", 10, 11), r("overlap", 9, 11)));
        assertEquals(List.of("a", "next"), ids(kept), "two end at 10 and start at 5: the id decides");
        var early = Planner.mostMeetings(List.of(r("late", 5, 10), r("early", 0, 10)));
        assertEquals(List.of("early"), ids(early), "the same end: the earlier start comes first");
    }

    @Test
    void badRequestsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> Planner.plan(null));
        assertThrows(IllegalArgumentException.class, () -> Planner.mostMeetings(null));
        assertThrows(IllegalArgumentException.class, () -> Planner.plan(List.of(r("a", 5, 5))));
        assertThrows(IllegalArgumentException.class, () -> Planner.mostMeetings(List.of(r("a", 6, 5))));
        assertThrows(IllegalArgumentException.class, () -> Planner.plan(List.of(r("a", 1, 2), r("a", 3, 4))));
        assertThrows(IllegalArgumentException.class,
                () -> Planner.mostMeetings(List.of(r("a", 1, 2), r("a", 3, 4))));
    }

    @Test
    void theInputIsNotChanged() {
        var requests = new ArrayList<>(List.of(r("b", 5, 9), r("a", 1, 3), r("c", 2, 4)));
        var copy = List.copyOf(requests);
        Planner.plan(requests);
        Planner.mostMeetings(requests);
        assertEquals(copy, requests);
    }

    @Test
    void randomDaysMatchTheRuleAndAreOptimal() {
        var random = new Random(31);
        for (int round = 0; round < 400; round++) {
            var requests = randomRequests(random, 1 + random.nextInt(40), 200, 60);
            assertEquals(slowPlan(requests), Planner.plan(requests), "plan of " + requests);
        }
        for (int round = 0; round < 200; round++) {
            var requests = randomRequests(random, 1 + random.nextInt(12), 100, 40);
            var kept = Planner.mostMeetings(requests);
            assertEquals(bruteForceMost(requests), kept.size(), "most meetings of " + requests);
            for (int i = 1; i < kept.size(); i++) {
                assertTrue(kept.get(i - 1).end() <= kept.get(i).start(), "kept meetings overlap: " + kept);
            }
        }
    }

    @Test
    void aDayWithFiftyThousandMeetingsAtOnceIsFast() {
        var requests = new ArrayList<Request>();
        for (int i = 0; i < 200_000; i++) {
            requests.add(r("m" + i, i, i + 50_000));
        }
        Collections.shuffle(requests, new Random(32));
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            var plan = Planner.plan(requests);
            assertEquals(50_000, plan.rooms());
            assertEquals(0, plan.roomOf().get("m50000"), "room 0 frees at 50,000");
            assertEquals(49_999, plan.roomOf().get("m199999"));
            var kept = Planner.mostMeetings(requests);
            assertEquals(4, kept.size());
            assertEquals(List.of("m0", "m50000", "m100000", "m150000"), ids(kept));
        }, "50,000 overlapping meetings: is something scanning every room for each one?");
    }
}
