package be.kdg.sa.backend.domain;

import lombok.*;

@Getter
@AllArgsConstructor
@ToString
public class OrderLine {
    private String dishName;
    private Money priceAtTimeOfOrder;
    private int quantity;
}
