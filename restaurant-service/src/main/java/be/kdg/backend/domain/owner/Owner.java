package be.kdg.backend.domain.owner;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@AllArgsConstructor
@ToString
public class Owner {
    private final UUID id;
    private final String externalSubjectId; // Keycloak sub
    private final String email;
    private final String firstName;
    private final String lastName;

    public static Owner create(String sub, String email, String first, String last) {
        return new Owner(UUID.randomUUID(), sub, email, first, last);
    }
}
