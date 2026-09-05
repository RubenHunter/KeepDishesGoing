package be.kdg.backend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.jmolecules.archunit.JMoleculesDddRules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture tests mirroring the testing-demo pattern.
 *
 * Two layers of rules:
 *  (1) JMoleculesDddRules.all() — the canonical DDD rule set shipped with jmolecules-starter-test,
 *        checking aggregate / entity / value object annotations and their relationships. Same call
 *        used in {@code testing-demo/src/test/java/be/kdg/ordering/DddArchTest.java}.
 *  (2) Two hand-rolled layer rules that jmolecules doesn't enforce (infra isolation).
 */
@AnalyzeClasses(packages = "be.kdg.backend")
class ArchitectureTest {

    @ArchTest
    void whenCheckingAllClasses_thenCodeFollowsAllDddPrinciples(JavaClasses classes) {
        JMoleculesDddRules.all().check(classes);
    }

    /** Domain must not depend on infrastructure or web — code-isolation invariant. */
    @ArchTest
    static final ArchRule domainIsolatedFromInfra =
            noClasses().that().resideInAPackage("be.kdg.backend.domain..")
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

    /** API controllers must not know about RabbitMQ — EventPublisher port in app layer is the only entry. */
    @ArchTest
    static final ArchRule apiNoRabbitMq =
            noClasses().that().resideInAPackage("be.kdg.backend.api..")
                    .should().dependOnClassesThat().resideInAPackage("be.kdg.backend.infrastructure.messaging..")
                    .because("Controllers must not know about RabbitMQ (coding-mistakes #7)");
}