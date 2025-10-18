package be.kdg.backend.domain.owner;

import java.util.Optional;

public interface IOwnerRepository {
    Optional<Owner> findByExternalSubjectId(String sub);
    void save(Owner owner);
}
