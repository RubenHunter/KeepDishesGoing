package be.kdg.backend.domain;

import lombok.*;

@Getter
@AllArgsConstructor
@ToString
public class Address {
    private String street;
    private String houseNumber;
    private String postalCode;
    private String city;
    private String country;
}
