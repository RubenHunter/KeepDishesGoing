package be.kdg.backend.infrastructure;

import be.kdg.backend.domain.owner.IOwnerRepository;
import be.kdg.backend.domain.owner.Owner;
import be.kdg.backend.infrastructure.jpa.JpaOwnerEntity;
import be.kdg.backend.infrastructure.jpa.JpaOwnerRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Primary
@Repository
public class DbOwnerRepository implements IOwnerRepository {

    private final JpaOwnerRepository jpa;

    public DbOwnerRepository(JpaOwnerRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Owner> findByExternalSubjectId(String sub) {
        return jpa.findByExternalSubjectId(sub).map(JpaOwnerEntity::toDomain);
    }

    @Override
    public void save(Owner owner) {
        jpa.save(JpaOwnerEntity.fromDomain(owner));
    }
}
