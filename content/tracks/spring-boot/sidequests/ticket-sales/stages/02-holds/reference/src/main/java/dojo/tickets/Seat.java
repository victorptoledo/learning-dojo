package dojo.tickets;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** One seat of one event: the row the database locks when someone takes it. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "label"}))
public class Seat {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Event event;

    private String label;

    private int rowIndex;

    private int number;

    @Enumerated(EnumType.STRING)
    private SeatStatus status = SeatStatus.AVAILABLE;

    /** The hold that has this seat, while it is held. */
    private Long holdId;

    protected Seat() {
    }

    public Seat(Event event, int rowIndex, int number) {
        this.event = event;
        this.rowIndex = rowIndex;
        this.number = number;
        this.label = (char) ('A' + rowIndex) + Integer.toString(number);
    }

    public String getLabel() {
        return label;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public int getNumber() {
        return number;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public boolean isAvailable() {
        return status == SeatStatus.AVAILABLE;
    }

    public void hold(long holdId) {
        this.status = SeatStatus.HELD;
        this.holdId = holdId;
    }

    public void release() {
        this.status = SeatStatus.AVAILABLE;
        this.holdId = null;
    }
}
