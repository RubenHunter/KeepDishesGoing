package be.kdg.backend;

import be.kdg.backend.application.PaymentProperties;
import be.kdg.backend.application.RestaurantProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({RestaurantProperties.class, PaymentProperties.class})
@EnableScheduling
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}