package dojo.rooms;

import java.util.Map;

/** How many rooms a day needs, and the room (from 0) of each request id. */
public record Plan(int rooms, Map<String, Integer> roomOf) {
}
