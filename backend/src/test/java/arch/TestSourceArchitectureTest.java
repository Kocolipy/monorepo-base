package arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Rules over the test sources.
 *
 * <p>{@link ArchitectureTest} imports production classes only, so a rule about test code needs its
 * own import. Kept in a class of its own rather than as a second importer inside that one, so each
 * class has exactly one class set and every rule in it reads against the same thing.
 */
@com.tngtech.archunit.junit.AnalyzeClasses(
    packages = "com.example.backend",
    importOptions = {ImportOption.OnlyIncludeTests.class}
)
public class TestSourceArchitectureTest {

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule no_logic_in_test_helpers =
        noClasses()
            .that().haveSimpleNameEndingWith("Fixtures")
            .should().dependOnClassesThat().resideInAnyPackage("org.assertj..", "org.junit.jupiter.api..")
            .allowEmptyShould(true)
            .because("A fixture builds inputs; an assertion inside one fails in a helper instead of"
                    + " in the test that owns the expectation");
}
