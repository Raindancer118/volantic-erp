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
 * CI-Gate für die <strong>hexagonale Architektur innerhalb jedes Moduls</strong> (CLAUDE.md, ADR-0001).
 * Die <em>Modulgrenzen</em> selbst (Spring Modulith) prüft {@link ModularityTests#verifiziertModulgrenzen()};
 * hier geht es um die Schichten domain → application → infrastructure/api innerhalb der Module.
 */
@AnalyzeClasses(packages = "de.volantic.erp", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_kennt_keine_infrastruktur =
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
                    .because("Die Domäne ist rein: keine Persistenz, kein Spring, keine Infrastruktur (Hexagonal)");

    @ArchTest
    static final ArchRule application_kennt_keine_infrastruktur =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..infrastructure..")
                    .because("Die Application-Schicht hängt nach innen (Domäne + Ports), nie an Infrastruktur");

    @ArchTest
    static final ArchRule outbound_ports_sind_interfaces =
            classes()
                    .that().resideInAPackage("..application.port.out..")
                    .should().beInterfaces()
                    .because("Outbound-Ports sind Interfaces — die Implementierung liegt in infrastructure");

    @ArchTest
    static final ArchRule jpa_entities_nur_in_infrastructure =
            classes()
                    .that().areAnnotatedWith(Entity.class).or().areAnnotatedWith(Table.class)
                    .should().resideInAPackage("..infrastructure.persistence..")
                    .because("JPA-Entities sind Infrastruktur, nicht Domänen-Modell");

    @ArchTest
    static final ArchRule spring_data_repositories_nur_in_infrastructure =
            classes()
                    .that().areAssignableTo(Repository.class).and().areInterfaces()
                    .should().resideInAPackage("..infrastructure.persistence..")
                    .because("Spring-Data-Repositories gehören in die infrastructure.persistence-Schicht");

    @ArchTest
    static final ArchRule controller_nur_in_api =
            classes()
                    .that().areAnnotatedWith(RestController.class)
                    .should().resideInAPackage("..api..")
                    .because("RestController gehören in das api-Package");

    @ArchTest
    static final ArchRule kein_service_in_api =
            noClasses()
                    .that().resideInAPackage("..api..")
                    .should().beAnnotatedWith(Service.class)
                    .because("Keine @Service-Beans in der API-Schicht — Logik gehört in application");
}
