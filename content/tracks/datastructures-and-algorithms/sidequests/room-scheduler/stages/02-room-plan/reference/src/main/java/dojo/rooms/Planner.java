package dojo.rooms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

/** Interval scheduling by sorting: the fewest rooms with two heaps, and activity selection. */
public final class Planner {

    private Planner() {
    }

    public static Plan plan(List<Request> requests) {
        List<Request> sorted = checked(requests);
        sorted.sort(Comparator.comparingLong(Request::start).thenComparing(Request::id));
        // busy rooms as {end of their meeting, room}, earliest end first; free rooms by number
        var busy = new PriorityQueue<long[]>(Comparator.comparingLong((long[] r) -> r[0]));
        var free = new PriorityQueue<Integer>();
        var roomOf = new HashMap<String, Integer>();
        int rooms = 0;
        for (Request request : sorted) {
            while (!busy.isEmpty() && busy.peek()[0] <= request.start()) {
                free.add((int) busy.poll()[1]);
            }
            int room = free.isEmpty() ? rooms++ : free.poll();
            busy.add(new long[] {request.end(), room});
            roomOf.put(request.id(), room);
        }
        return new Plan(rooms, roomOf);
    }

    public static List<Request> mostMeetings(List<Request> requests) {
        List<Request> sorted = checked(requests);
        sorted.sort(Comparator.comparingLong(Request::end)
                .thenComparingLong(Request::start)
                .thenComparing(Request::id));
        var kept = new ArrayList<Request>();
        long lastEnd = Long.MIN_VALUE;
        for (Request request : sorted) {
            if (request.start() >= lastEnd) {
                kept.add(request);
                lastEnd = request.end();
            }
        }
        return kept;
    }

    /** A copy of the requests, after checking them. */
    private static List<Request> checked(List<Request> requests) {
        if (requests == null) {
            throw new IllegalArgumentException("no requests");
        }
        var ids = new HashSet<String>();
        for (Request request : requests) {
            if (request == null || request.start() >= request.end() || !ids.add(request.id())) {
                throw new IllegalArgumentException("bad or repeated request: " + request);
            }
        }
        return new ArrayList<>(requests);
    }
}
