package be.kdg.sa.backend.domain;

import lombok.*;

@Getter
@AllArgsConstructor
@ToString
public class CustomerInfo {
    private String name;
    private Address deliveryAddress;
    private String contactEmail;
}
