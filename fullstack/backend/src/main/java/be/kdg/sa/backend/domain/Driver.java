package be.kdg.sa.backend.domain;

import lombok.*;

@Getter
@AllArgsConstructor
@ToString
public class Driver {
    private Long id;
    private String name;
    private String email;
    private Money totalEarnings;
}
