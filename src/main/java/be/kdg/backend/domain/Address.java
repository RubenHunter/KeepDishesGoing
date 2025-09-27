package be.kdg.backend.domain;

import lombok.*;
import org.jmolecules.ddd.annotation.ValueObject;

@Getter
@AllArgsConstructor
@ToString
@ValueObject
public class Address {
    private String street;
    private String houseNumber;
    private String postalCode;
    private String city;
    private String country;
}
