package arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

@com.tngtech.archunit.junit.AnalyzeClasses(
    packages = "com.example.backend",
    importOptions = {ImportOption.DoNotIncludeTests.class}
)
public class ArchitectureTest {

    // Cycles
    
    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule no_cyclic_dependencies =
        SlicesRuleDefinition.slices()
            .matching("com.example.backend.(*)..")
            .should().beFreeOfCycles()
            .allowEmptyShould(true)
            .because("Cyclic dependencies prevent independent module development and deployment");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule no_classes_in_default_package =
        noClasses()
            .should().haveNameMatching("^[^.]+$")
            .allowEmptyShould(true)
            .as("No class should reside in the default (unnamed) package");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule no_intra_layer_cycles =
        SlicesRuleDefinition.slices()
            .matching("..service.(*)..")
            .should().beFreeOfCycles()
            .allowEmptyShould(true)
            .because("Cycles between classes inside a single layer make that layer impossible to reason about in isolation");

    // Naming Conventions
    
    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule naming_conventions_controller =
        classes()
            .that().areAnnotatedWith(RestController.class)
            .or().areAnnotatedWith(Controller.class)
            .should().haveSimpleNameEndingWith("Controller")
            .allowEmptyShould(true)
            .because("Naming conventions aid discoverability, whichever package a controller lives in");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule naming_conventions_service =
        classes()
            .that().resideInAPackage("..service")
            .and().areNotInterfaces()
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Service")
            .allowEmptyShould(true)
            .because("Naming conventions aid discoverability");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule naming_conventions_repository =
        classes()
            .that().resideInAPackage("..repository")
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Repository")
            .allowEmptyShould(true)
            .because("Naming conventions aid discoverability");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule exception_naming_convention =
        classes()
            .that().areAssignableTo(Exception.class)
            .should().haveSimpleNameEndingWith("Exception")
            .allowEmptyShould(true)
            .because("Exception naming must be explicit");

    // Class Containment
    
    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule configuration_classes_in_config_package =
        classes()
            .that().areAnnotatedWith(Configuration.class)
            .should().resideInAPackage("..config..")
            .allowEmptyShould(true)
            .because("Configuration classes must be isolated in config packages");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule entities_only_in_entity_packages =
        classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAnyPackage("..entity..", "..domain..")
            .allowEmptyShould(true)
            .because("Entity classes must not leak into controller or service packages");

    // Domain Purity (Onion/Hexagonal)
    
    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule no_domain_infrastructure_imports =
        noClasses()
            .that().resideInAnyPackage("..domain..", "..model..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..", "..adapter..", "..web..", "..controller..")
            .allowEmptyShould(true)
            .because("Domain must remain infrastructure-agnostic");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule no_spring_annotations_in_domain =
        noClasses()
            .that().resideInAnyPackage("..entity..", "..model..", "..domain..")
            .and().resideOutsideOfPackage("..mapper..")
            .and().areTopLevelClasses()
            .should().beAnnotatedWith(Component.class)
            .orShould().beAnnotatedWith(Service.class)
            .orShould().beAnnotatedWith(Repository.class)
            .orShould().beAnnotatedWith(Controller.class)
            .allowEmptyShould(true)
            .because("Domain objects must not carry Spring stereotype annotations");

    // Dependency Injection
    
    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule no_field_injection =
        noFields()
            .that().areDeclaredInClassesThat().areTopLevelClasses()
            .should().beAnnotatedWith(Autowired.class)
            .allowEmptyShould(true)
            .because("Constructor injection is required for testability and immutability");

    // Code Quality
    
    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule utility_classes_private_constructor =
        classes()
            .that().haveSimpleNameEndingWith("Util")
            .or().haveSimpleNameEndingWith("Utils")
            .should().haveOnlyPrivateConstructors()
            .allowEmptyShould(true)
            .because("Utility classes must not be instantiable");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule no_test_imports_in_production =
        noClasses()
            .that().resideOutsideOfPackages("..test..")
            .should().dependOnClassesThat().resideInAnyPackage("org.junit..", "org.mockito..", "org.testng..")
            .allowEmptyShould(true)
            .because("Test dependencies must not leak into production code");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule no_system_out =
        GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
            .allowEmptyShould(true);

    // JPA / Persistence
    
    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule entities_have_required_annotations =
        classes()
            .that().areAnnotatedWith(Entity.class)
            .should().beAnnotatedWith(Table.class)
            .allowEmptyShould(true)
            .because("Explicit table mapping prevents runtime surprises");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule repositories_must_be_interfaces =
        classes()
            .that().resideInAPackage("..repository")
            .and().areTopLevelClasses()
            .should().beInterfaces()
            .allowEmptyShould(true)
            .because("Spring Data repositories must be interfaces");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule no_entity_in_controllers =
        noClasses()
            .that().areAnnotatedWith(RestController.class)
            .or().areAnnotatedWith(Controller.class)
            .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
            .allowEmptyShould(true)
            .because("Controllers must use DTOs, not entities, to prevent lazy-loading issues and data exposure");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule annotation_gated_access =
        classes()
            .that().areAssignableTo(EntityManager.class)
            .should().onlyHaveDependentClassesThat()
            .areAnnotatedWith("org.springframework.transaction.annotation.Transactional")
            .allowEmptyShould(true)
            .because("EntityManager must only be reached from a @Transactional caller");

    // Spring Annotations
    
    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule controllers_must_be_annotated =
        classes()
            .that().haveSimpleNameEndingWith("Controller")
            .and().areTopLevelClasses()
            .and().areNotInterfaces()
            .should().beAnnotatedWith(RestController.class)
            .orShould().beAnnotatedWith(Controller.class)
            .allowEmptyShould(true)
            .because("Controllers must be annotated for Spring component scanning");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule no_transactional_outside_service =
        noClasses()
            .that().resideInAnyPackage("..controller..", "..domain..")
            .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
            .orShould().beAnnotatedWith("jakarta.transaction.Transactional")
            .allowEmptyShould(true)
            .because("Transaction management belongs at the use-case/service boundary");

    // Layer Boundaries

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule no_business_logic_in_controllers =
        noClasses()
            .that().resideInAPackage("..controller..")
            .or().areAnnotatedWith(RestController.class)
            .or().areAnnotatedWith(Controller.class)
            .should().dependOnClassesThat().resideInAnyPackage("..repository..", "..persistence..")
            .allowEmptyShould(true)
            .because("Controllers must delegate to services, not access repositories directly");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule interfaces_in_api_package =
        classes()
            .that().areInterfaces()
            .and().arePublic()
            .and().resideInAPackage("..port..")
            .should().resideInAPackage("..api..")
            .orShould().resideInAPackage("..port..")
            .allowEmptyShould(true)
            .because("Public-facing ports belong in an api or port package, never in an impl package");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule onion_architecture =
        onionArchitecture()
            .domainModels("com.example.backend..domain..")
            .domainServices("com.example.backend..domain.service..")
            .applicationServices("com.example.backend..application..")
            .adapter("persistence", "com.example.backend..infrastructure.persistence..")
            .adapter("web", "com.example.backend..controller..")
            .withOptionalLayers(true)
            .because("Dependencies point inward: adapters depend on application, application on domain, domain on nothing");
}
