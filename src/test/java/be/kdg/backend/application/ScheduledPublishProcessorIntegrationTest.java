package be.kdg.backend.application;

import be.kdg.backend.TestHelper;
import be.kdg.backend.domain.dish.DishCategory;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.domain.scheduling.IScheduledPublishRepository;
import be.kdg.backend.domain.scheduling.ScheduledPublishJob;
import be.kdg.backend.infrastructure.jpa.JpaScheduledPublishEntity;
import be.kdg.backend.infrastructure.jpa.JpaScheduledPublishRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ScheduledPublishProcessorIntegrationTest {

    @Autowired
    private ScheduledPublishProcessor processor;

    @Autowired
    private IScheduledPublishRepository scheduledRepo;

    @Autowired
    private TestHelper helper;

    @Autowired
    private DishService dishService;

    @AfterEach
    void tearDown() {
        helper.cleanUp();
    }

    @Test
    void shouldProcessDueJobAndPublishDrafts() {
        // Arrange: restaurant with a draft dish
        Restaurant r = helper.createRestaurant("SchedProc");
        RestaurantId rid = helper.id(r);
        helper.addDraftDish(r, "Noodles", new BigDecimal("7.50"), "EUR", DishCategory.MAIN_COURSE, "veg");

        // Create a due job via the domain interface (publishAt in the past)
        ScheduledPublishJob job = ScheduledPublishJob.create(
                UUID.randomUUID(),
                rid.id(),
                LocalDateTime.now().minusSeconds(1)
        );
        scheduledRepo.save(job);

        // Act
        processor.processJob(job.getId());

        // Assert: menu published and job marked done
        assertEquals(1, dishService.getMenuDishes(rid).size());
        ScheduledPublishJob persisted = scheduledRepo.getById(job.getId()).orElseThrow();
        assertEquals(ScheduledPublishJob.Status.DONE, persisted.getStatus());
        assertEquals(1, persisted.getAttempts());
        assertNull(persisted.getLastError());
    }

    @Test
    void shouldMarkFailedWhenRestaurantMissing() {
        // Arrange: job for a non\-existing restaurant
        UUID unknownRestaurant = UUID.randomUUID();
        ScheduledPublishJob job = ScheduledPublishJob.create(
                UUID.randomUUID(),
                unknownRestaurant,
                LocalDateTime.now().minusSeconds(1)
        );
        scheduledRepo.save(job);

        // Act
        processor.processJob(job.getId());

        // Assert: job marked failed and attempts increased
        ScheduledPublishJob persisted = scheduledRepo.getById(job.getId()).orElseThrow();
        assertEquals(ScheduledPublishJob.Status.FAILED, persisted.getStatus());
        assertEquals(1, persisted.getAttempts());
        assertNotNull(persisted.getLastError());
    }
}
