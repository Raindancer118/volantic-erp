package de.volantic.erp;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * CI gate for the <strong>hexagonal architecture within each module</strong> (CLAUDE.md, ADR-0001).
 * The <em>module boundaries</em> themselves (Spring Modulith) are checked by
 * {@link ModularityTests#verifiesModuleBoundaries()}; this is about the layers
 * domain → application → infrastructure/api within the modules.
 */
@AnalyzeClasses(packages = "de.volantic.erp", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_has_no_infrastructure =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..infrastructure..",
                            "..application..",
                            "jakarta.persistence..",
                            "org.springframework.data..",
                            "org.springframework.web..",
                            "org.springframework.stereotype..")
                    .because("The domain is pure: no persistence, no Spring, no infrastructure (hexagonal)");

    @ArchTest
    static final ArchRule application_has_no_infrastructure =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..infrastructure..")
                    .because("The application layer depends inward (domain + ports), never on infrastructure");

    @ArchTest
    static final ArchRule outbound_ports_are_interfaces =
            classes()
                    .that().resideInAPackage("..application.port.out..")
                    .should().beInterfaces()
                    .because("Outbound ports are interfaces — the implementation lives in infrastructure");

    @ArchTest
    static final ArchRule jpa_entities_only_in_infrastructure =
            classes()
                    .that().areAnnotatedWith(Entity.class).or().areAnnotatedWith(Table.class)
                    .should().resideInAPackage("..infrastructure.persistence..")
                    .because("JPA entities are infrastructure, not the domain model");

    @ArchTest
    static final ArchRule spring_data_repositories_only_in_infrastructure =
            classes()
                    .that().areAssignableTo(Repository.class).and().areInterfaces()
                    .should().resideInAPackage("..infrastructure.persistence..")
                    .because("Spring Data repositories belong in the infrastructure.persistence layer");

    @ArchTest
    static final ArchRule controllers_only_in_api =
            classes()
                    .that().areAnnotatedWith(RestController.class)
                    .should().resideInAPackage("..api..")
                    .because("RestControllers belong in the api package");

    @ArchTest
    static final ArchRule no_service_in_api =
            noClasses()
                    .that().resideInAPackage("..api..")
                    .should().beAnnotatedWith(Service.class)
                    .because("No @Service beans in the API layer — logic belongs in application");
}
