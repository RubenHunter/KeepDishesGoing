package be.kdg.sa.backend.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@ToString
public class Delivery {
    private Long id;
    //private Order.id orderId;
    private DeliveryStatus status;
    private LocalDateTime claimedAt;
    private LocalDateTime readyForPickupAt;
    private LocalDateTime deliveredAt; // kan null zijn als nog niet geleverd, updaten wanneer geleverd
    private Money deliveryFee;
}
