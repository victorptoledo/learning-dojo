package dojo;

import java.util.List;

/**
 * Airports joined by one-way priced routes: regions, the cheapest fare, the cheapest fare
 * within a number of flights, code suggestions and the cheapest network.
 */
public final class FlightNetwork {

    /** Adds an airport without routes; adding a known airport does nothing. */
    public void addAirport(String code) {
        throw new UnsupportedOperationException("todo addAirport " + code);
    }

    /** Adds a one-way route; both airports are added if they are new. */
    public void addRoute(String from, String to, int price) {
        throw new UnsupportedOperationException("todo addRoute " + from + " " + to);
    }

    /** Whether a chain of routes, in either direction, joins the two airports. */
    public boolean connected(String a, String b) {
        throw new UnsupportedOperationException("todo connected");
    }

    /** The cheapest total price from one airport to another, or -1 if there is no way. */
    public long cheapest(String from, String to) {
        throw new UnsupportedOperationException("todo cheapest");
    }

    /** The cheapest total price using at most {@code maxFlights} routes, or -1. */
    public long cheapestWithin(String from, String to, int maxFlights) {
        throw new UnsupportedOperationException("todo cheapestWithin");
    }

    /** Up to {@code limit} airports whose code starts with the prefix, the most routes first, then by code. */
    public List<String> suggest(String prefix, int limit) {
        throw new UnsupportedOperationException("todo suggest " + prefix);
    }

    /** The lowest price of routes, usable both ways, that join every airport; -1 if impossible. */
    public long networkCost() {
        throw new UnsupportedOperationException("todo networkCost");
    }
}
