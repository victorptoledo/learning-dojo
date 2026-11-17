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

    public SeatStatus getStatus() {
        return status;
    }
}
