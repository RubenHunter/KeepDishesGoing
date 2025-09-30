package be.kdg.sa.backend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.jmolecules.archunit.JMoleculesDddRules;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("be.kdg.sa.backend");
//g
    @Test
    void testDddArchitecture() {
        JMoleculesDddRules.all().check(classes);
    }
//g
    @Test
    void domainShouldNotDependOnOtherLayers() {
        ArchRule rule = classes()
                .that().resideInAPackage("..domain..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..domain..",
                        "java..",
                        "org.jmolecules..",
                        "lombok.."
                );
        rule.check(classes);
    }
//ng
    @Test
    void applicationShouldNotDependOnWebOrInfrastructure() {
        ArchRule rule = classes()
                .that().resideInAPackage("..application..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..application..",
                        "..domain..",
                        "java..",
                        "org.springframework..",
                        "lombok.."
                );
        rule.check(classes);
    }
}