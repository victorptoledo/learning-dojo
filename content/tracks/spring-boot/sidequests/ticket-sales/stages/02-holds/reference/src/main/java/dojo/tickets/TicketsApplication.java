package dojo.tickets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** The ticket API. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class TicketsApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketsApplication.class, args);
    }
}
