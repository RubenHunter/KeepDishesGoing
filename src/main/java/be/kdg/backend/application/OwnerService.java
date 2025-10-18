package be.kdg.backend.application;

import be.kdg.backend.domain.owner.IOwnerRepository;
import be.kdg.backend.domain.owner.Owner;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OwnerService {

    private final IOwnerRepository repo;

    public OwnerService(IOwnerRepository repo) {
        this.repo = repo;
    }

    public Owner ensureFromJwt(Jwt token) {
        String sub = token.getClaimAsString(StandardClaimNames.SUB);
        return repo.findByExternalSubjectId(sub).orElseGet(() -> {
            Owner created = Owner.create(
                    sub,
                    token.getClaimAsString(StandardClaimNames.EMAIL),
                    token.getClaimAsString(StandardClaimNames.GIVEN_NAME),
                    token.getClaimAsString(StandardClaimNames.FAMILY_NAME)
            );
            repo.save(created);
            return created;
        });
    }
}
