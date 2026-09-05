package be.kdg.backend.infrastructure.jpa;

import be.kdg.backend.domain.owner.Owner;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "owners")
public class JpaOwnerEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String externalSubjectId;

    @Column
    private String email;

    @Column
    private String firstName;

    @Column
    private String lastName;

    protected JpaOwnerEntity() { }

    public JpaOwnerEntity(UUID id, String sub, String email, String first, String last) {
        this.id = id;
        this.externalSubjectId = sub;
        this.email = email;
        this.firstName = first;
        this.lastName = last;
    }

    public static JpaOwnerEntity fromDomain(Owner o) {
        return new JpaOwnerEntity(o.getId(), o.getExternalSubjectId(), o.getEmail(), o.getFirstName(), o.getLastName());
    }

    public Owner toDomain() {
        return new Owner(id, externalSubjectId, email, firstName, lastName);
    }
}
