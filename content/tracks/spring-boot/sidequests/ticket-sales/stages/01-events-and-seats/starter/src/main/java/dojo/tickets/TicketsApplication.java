package dojo.tickets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** The ticket API. Everything but the demo users is yours: see README.md. */
@SpringBootApplication
public class TicketsApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketsApplication.class, args);
    }
}
