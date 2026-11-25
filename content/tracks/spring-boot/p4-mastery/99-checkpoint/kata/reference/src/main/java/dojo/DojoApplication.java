package dojo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DojoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DojoApplication.class, args);
    }
}
