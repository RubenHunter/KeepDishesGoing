package be.kdg.backend.domain.customer;

import be.kdg.backend.domain.shared.CustomerId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerTest {

    private final CustomerId id = CustomerId.of(UUID.randomUUID());

    @Test
    void registerTrimsAndStoresProfile() {
        Customer c = Customer.register(id, " Ruben ", " ruben@x.be ", " Langestraat ", "12", "2000", "Antwerpen", "BE");

        assertThat(c.id()).isEqualTo(id);
        assertThat(c.name()).isEqualTo("Ruben");
        assertThat(c.email()).isEqualTo("ruben@x.be");
        assertThat(c.street()).isEqualTo("Langestraat");
    }

    @Test
    void registerToleratesBlankFields() {
        Customer c = Customer.register(id, null, null, null, null, null, null, null);

        assertThat(c.name()).isEmpty();
        assertThat(c.email()).isEmpty();
    }

    @Test
    void updateProfileReplacesAllFields() {
        Customer c = Customer.register(id, "Old", "old@x.be", "A", "1", "1000", "B", "BE");
        c.updateProfile("New", "new@x.be", "B", "2", "2000", "C", "NL");

        assertThat(c.name()).isEqualTo("New");
        assertThat(c.city()).isEqualTo("C");
        assertThat(c.country()).isEqualTo("NL");
    }

    @Test
    void rehydrateRestoresFields() {
        Customer c = Customer.rehydrate(id, "Ruben", "ruben@x.be", "L", "12", "2000", "A", "BE");

        assertThat(c.id()).isEqualTo(id);
        assertThat(c.name()).isEqualTo("Ruben");
    }
}
