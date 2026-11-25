package dojo.travel.domain;

import dojo.travel.messaging.SupplierReply;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** A trip and its reservations. Anyone can change anything. */
public class Trip {
    private String id;
    private String traveller;
    private List<Leg> legs = new ArrayList<>();
    private Map<Leg, String> confirmations = new EnumMap<>(Leg.class);
    private TripStatus status = TripStatus.PENDING;

    public Trip() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTraveller() {
        return traveller;
    }

    public void setTraveller(String traveller) {
        this.traveller = traveller;
    }

    public List<Leg> getLegs() {
        return legs;
    }

    public void setLegs(List<Leg> legs) {
        this.legs = legs;
    }

    public Map<Leg, String> getConfirmations() {
        return confirmations;
    }

    public TripStatus getStatus() {
        return status;
    }

    public void setStatus(TripStatus status) {
        this.status = status;
    }

    /** Records a supplier's reply straight on the trip. */
    public void apply(SupplierReply reply) {
        if (reply.reserved()) {
            confirmations.put(reply.leg(), reply.detail());
            if (confirmations.size() == legs.size()) {
                status = TripStatus.CONFIRMED;
            }
        } else {
            status = TripStatus.CANCELLED;
        }
    }
}
