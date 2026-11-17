package dojo.secure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** The secure API. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SecureApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecureApiApplication.class, args);
    }
}
