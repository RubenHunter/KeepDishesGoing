package be.kdg.sa.backend.architecture.coverage;

import be.kdg.sa.backend.domain.Enums.OrderStatus;
import be.kdg.sa.backend.infrastructure.OrderJpaEntity;
import be.kdg.sa.backend.infrastructure.SpringDataOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SpringDataOrderRepository repository;

    @Test
    void saveOrder_shouldPersistOrder() {
        // Arrange - gebruik constructor i.p.v. setters
        OrderJpaEntity order = new OrderJpaEntity(
                "ORD-123",
                "CUST-123",
                "REST-456",
                "Teststraat 123, 1000 Brussel",
                "test@example.com",
                OrderStatus.PLACED,
                new BigDecimal("25.00"),
                "EUR"
        );

        // Act
        OrderJpaEntity saved = repository.save(order);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<OrderJpaEntity> found = repository.findById("ORD-123");
        assertThat(found).isPresent();
        assertThat(found.get().getCustomerId()).isEqualTo("CUST-123");
        assertThat(found.get().getRestaurantId()).isEqualTo("REST-456");
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(found.get().getTotalAmount()).isEqualByComparingTo("25.00");
    }

    @Test
    void saveOrder_withAllFields_shouldPersistCorrectly() {
        // Arrange
        OrderJpaEntity order = new OrderJpaEntity();
        order.setId("ORD-999");
        order.setCustomerId("CUST-999");
        order.setRestaurantId("REST-999");
        order.setDeliveryAddress("Another Street 456, 2000 Antwerpen");
        order.setCustomerEmail("another@example.com");
        order.setStatus(OrderStatus.ACCEPTED);
        order.setTotalAmount(new BigDecimal("35.50"));
        order.setCurrency("EUR");

        // Act
        OrderJpaEntity saved = repository.save(order);

        // Assert
        assertThat(saved.getId()).isEqualTo("ORD-999");
        assertThat(saved.getCustomerId()).isEqualTo("CUST-999");
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("35.50");
    }

    @Test
    void findById_withNonExistingId_shouldReturnEmpty() {
        // Act
        Optional<OrderJpaEntity> result = repository.findById("NON-EXISTENT");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void existsById_withExistingId_shouldReturnTrue() {
        // Arrange
        OrderJpaEntity order = createTestOrder();
        repository.save(order);

        // Act
        boolean exists = repository.existsById("ORD-123");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void existsById_withNonExistingId_shouldReturnFalse() {
        // Act
        boolean exists = repository.existsById("NON-EXISTENT");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void findAll_shouldReturnAllOrders() {
        // Arrange
        OrderJpaEntity order1 = createTestOrder();
        OrderJpaEntity order2 = new OrderJpaEntity(
                "ORD-456", "CUST-456", "REST-456",
                "Different Address", "test2@example.com",
                OrderStatus.PENDING, new BigDecimal("18.75"), "EUR"
        );

        repository.save(order1);
        repository.save(order2);

        // Act
        List<OrderJpaEntity> allOrders = repository.findAll();

        // Assert
        assertThat(allOrders).hasSize(2);
        assertThat(allOrders)
                .extracting(OrderJpaEntity::getId)
                .containsExactlyInAnyOrder("ORD-123", "ORD-456");
    }

    @Test
    void deleteById_shouldRemoveOrder() {
        // Arrange
        OrderJpaEntity order = createTestOrder();
        repository.save(order);

        // Act
        repository.deleteById("ORD-123");

        // Assert
        assertThat(repository.existsById("ORD-123")).isFalse();
    }

    private OrderJpaEntity createTestOrder() {
        return new OrderJpaEntity(
                "ORD-123",
                "CUST-123",
                "REST-456",
                "Test Address",
                "test@example.com",
                OrderStatus.PLACED,
                new BigDecimal("15.00"),
                "EUR"
        );
    }
}