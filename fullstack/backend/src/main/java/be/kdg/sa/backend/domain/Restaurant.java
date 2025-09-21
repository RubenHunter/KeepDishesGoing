package be.kdg.sa.backend.domain;

import lombok.*;

@Getter
@AllArgsConstructor
@ToString
public class Restaurant {
    private Long id;
    private String name;
    private Address address;
    private String contactEmail;
    private String type;
    private boolean isManuallyClosed;

    //setters
}
