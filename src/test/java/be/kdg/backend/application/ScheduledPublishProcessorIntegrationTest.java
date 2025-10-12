package be.kdg.backend.application;

import be.kdg.backend.TestHelper;
import be.kdg.backend.domain.dish.DishCategory;
import be.kdg.backend.domain.restaurant.Restaurant;
import be.kdg.backend.domain.restaurant.RestaurantId;
import be.kdg.backend.infrastructure.jpa.JpaScheduledPublishEntity;
import be.kdg.backend.infrastructure.jpa.JpaScheduledPublishRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ScheduledPublishProcessorIntegrationTest {

    @Autowired
    private ScheduledPublishProcessor processor;
    @Autowired
    private JpaScheduledPublishRepository repo;
    @Autowired
    private TestHelper helper;
    @Autowired
    private DishService dishService;

    @AfterEach
    void tearDown() {
        helper.cleanUp();
    }

    @Test
    void shouldProcessDueJob() {
        Restaurant r = helper.createRestaurant("SchedSvc");
        RestaurantId rid = helper.id(r);
        helper.addDraftDish(r, "Noodles", new BigDecimal("8.00"), "EUR", DishCategory.MAIN_COURSE, "Veg");

        JpaScheduledPublishEntity job = new JpaScheduledPublishEntity(
                UUID.randomUUID(),
                rid.id(),
                LocalDateTime.now().minusSeconds(1)
        );
        repo.save(job);

        processor.processJob(job.getId());

        var after = repo.findById(job.getId()).orElseThrow();
        assertEquals(JpaScheduledPublishEntity.Status.DONE, after.getStatus());
        assertEquals(1, dishService.getMenuDishes(rid).size());
    }
}
