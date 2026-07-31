package be.kdg.backend.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit rules mirroring order-service layering invariants for delivery-service.
 */
@AnalyzeClasses(packages = "be.kdg.backend")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainMustNotDependOnInfra = noClasses()
            .that().resideInAPackage("be.kdg.backend.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "be.kdg.backend.infrastructure..",
                    "be.kdg.backend.api..",
                    "be.kdg.backend.application..",
                    "jakarta.persistence..",
                    "org.springframework.amqp..",
                    "org.springframework.web..",
                    "org.springframework.stereotype..",
                    "org.springframework.data..")
            .because("Domain layer must be technology-agnostic (coding-mistakes #6, #11)");

    @ArchTest
    static final ArchRule apiMustNotTouchMessaging = noClasses()
            .that().resideInAPackage("be.kdg.backend.api..")
            .should().dependOnClassesThat().resideInAPackage("be.kdg.backend.infrastructure.messaging..")
            .because("Controllers must not know about RabbitMQ (coding-mistakes #7)");

    @ArchTest
    static final ArchRule applicationMustNotDependOnInfraJpa = noClasses()
            .that().resideInAPackage("be.kdg.backend.application..")
            .should().dependOnClassesThat().resideInAPackage("be.kdg.backend.infrastructure.persistence..")
            .because("Application services talk to domain repositories only (coding-mistakes #10)");

    @ArchTest
    static final ArchRule aggregateRootsAnnotated = classes()
            .that().resideInAPackage("be.kdg.backend.domain..")
            .and().haveSimpleNameMatching("(Delivery|DeliveryPerson|Payout)")
            .should().beAnnotatedWith(org.jmolecules.ddd.annotation.AggregateRoot.class)
            .because("Aggregate roots must carry jmolecules @AggregateRoot");

    @ArchTest
    static final ArchRule domainHasNoSetters = noClasses()
            .that().resideInAPackage("be.kdg.backend.domain..")
            .should().beAnnotatedWith(lombok.Setter.class)
            .because("Domain cannot expose setters (coding-mistakes #5)");
}