package dojo.rooms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** The room booking API. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class RoomBookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(RoomBookingApplication.class, args);
    }
}
