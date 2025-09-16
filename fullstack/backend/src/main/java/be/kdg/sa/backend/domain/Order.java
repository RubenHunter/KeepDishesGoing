package be.kdg.sa.backend.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@ToString
public class Order {
    private Long id;
    //private Restaurant.id restaurantId; moet ik nog uitzoeken (getters setters)
    private OrderStatus status;
    private LocalDateTime placedAt;
    private LocalDateTime acceptBy;
    private Money totalPrice;

    //setters
}
