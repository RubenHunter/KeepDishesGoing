package be.kdg.backend.infrastructure.http;

import be.kdg.backend.application.RestaurantProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(RestaurantProperties.class)
public class RestClientConfig {

    @Bean
    RestClient restaurantClient(RestaurantProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .build();
    }
}