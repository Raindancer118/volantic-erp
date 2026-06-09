---
name: archunit-volantic
description: ArchUnit rules and patterns for Volantic ERP — hexagonal architecture, Spring Modulith module boundaries, no-JPA-in-API, domain isolation. Use when writing ArchUnit tests, reviewing architecture violations, or adding new ArchUnit rules.
---

# ArchUnit Patterns — Volantic ERP

ArchUnit is a CI-Gate in Volantic ERP (`./gradlew check`). Every architectural rule is enforced automatically.

## When to Use
- Writing new ArchUnit tests
- Reviewing a potential architecture violation
- Adding a new module and need to define its rules
- Understanding why the build fails on architecture checks

---

## Project Architecture

```
de.volantic.erp.<modul>/
├── domain/           # Pure Java — NO Spring, NO JPA, NO infrastructure
│   ├── model/        # Aggregates, Entities, Value Objects
│   ├── service/      # Domain Services
│   └── event/        # Domain Events (plain records)
├── application/      # Use Cases, orchestration
│   ├── port/
│   │   ├── in/       # Inbound ports (use case interfaces)
│   │   └── out/      # Outbound ports (repository interfaces)
│   └── service/      # Application Services
├── infrastructure/   # Spring beans, JPA, HTTP clients, etc.
│   ├── persistence/  # JPA Repositories, Entities
│   └── adapter/      # External API adapters
└── api/              # REST Controllers, DTOs (inbound only)
```

---

## 1. Core ArchUnit Setup

```kotlin
// build.gradle.kts
dependencies {
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.0")
}
```

```java
@AnalyzeClasses(
    packages = "de.volantic.erp",
    importOptions = {ImportOption.DoNotIncludeTests.class}
)
class VolanticArchitectureTest {
    // all rules below go here
}
```

---

## 2. Hexagonal Architecture Rules

```java
// Domain has no dependency on infrastructure
@ArchTest
static final ArchRule domain_hasBke_keine_infrastruktur_imports =
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage(
            "..infrastructure..",
            "jakarta.persistence..",
            "org.springframework.data..",
            "org.springframework.web.."
        )
        .because("Domain darf keine Infrastruktur kennen (Hexagonale Architektur)");

// Application only depends on domain and port interfaces
@ArchTest
static final ArchRule application_haengt_nur_von_domain_ab =
    classes()
        .that().resideInAPackage("..application..")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage(
            "..domain..",
            "..application..",
            "java..",
            "org.springframework.stereotype..",
            "org.springframework.transaction.."
        );

// Infrastructure implements ports, not the other way around
@ArchTest
static final ArchRule ports_sind_interfaces =
    classes()
        .that().resideInAPackage("..port.out..")
        .should().beInterfaces()
        .because("Outbound ports sind Interfaces — Implementierung liegt in infrastructure");
```

---

## 3. Spring Modulith — Module Boundary Rules

```java
// Module packages must not access internal packages of other modules
@ArchTest
static final ArchRule module_grenzen_einhalten =
    SlicesRuleDefinition.slices()
        .matching("de.volantic.erp.(*)..")
        .should().notDependOnEachOther()
        .ignoreDependency(
            // Allow shared core/sdk packages
            resideInAPackage("de.volantic.erp.core.."),
            resideInAPackage("de.volantic.erp.(*)..")
        )
        .because("Module kommunizieren nur über öffentliche APIs und Domain-Events");

// Specific: stammdaten internal is off-limits
@ArchTest
static final ArchRule stammdaten_internal_nicht_zugreifen =
    noClasses()
        .that().resideOutsideOfPackage("de.volantic.erp.stammdaten..")
        .should().accessClassesThat()
        .resideInAPackage("de.volantic.erp.stammdaten.infrastructure..")
        .orShould().accessClassesThat()
        .resideInAPackage("de.volantic.erp.stammdaten.domain..");
```

---

## 4. API Layer Rules

```java
// No JPA entities in API responses
@ArchTest
static final ArchRule keine_jpa_entities_in_api =
    noMethods()
        .that().areDeclaredInClassesThat()
        .resideInAPackage("..api..")
        .should().haveRawReturnType(assignableTo(hasAnnotation(Entity.class)))
        .because("API-Layer darf keine JPA-Entities zurückgeben — DTOs/Records verwenden");

// Controllers only in api package
@ArchTest
static final ArchRule controller_nur_in_api =
    classes()
        .that().areAnnotatedWith(RestController.class)
        .should().resideInAPackage("..api..")
        .because("RestController gehören in das api-Package");

// No @Service in api layer
@ArchTest
static final ArchRule kein_service_in_api =
    noClasses()
        .that().resideInAPackage("..api..")
        .should().beAnnotatedWith(Service.class)
        .because("Keine @Service-Annotationen in der API-Schicht");
```

---

## 5. Persistence Rules

```java
// JPA repositories only in infrastructure
@ArchTest
static final ArchRule repositories_nur_in_infrastructure =
    classes()
        .that().implement(JpaRepository.class)
        .or().areAnnotatedWith(Repository.class)
        .should().resideInAPackage("..infrastructure.persistence..")
        .because("JPA-Repositories gehören in die infrastructure.persistence-Schicht");

// JPA entities only in infrastructure (not in domain model)
@ArchTest
static final ArchRule jpa_entities_nur_in_infrastructure =
    classes()
        .that().areAnnotatedWith(Entity.class)
        .or().areAnnotatedWith(Table.class)
        .should().resideInAPackage("..infrastructure.persistence..")
        .because("JPA-Entities sind Infrastruktur, nicht Domain-Modell");
```

---

## 6. Naming Conventions

```java
@ArchTest
static final ArchRule services_korrekt_benannt =
    classes()
        .that().resideInAPackage("..application.service..")
        .should().haveSimpleNameEndingWith("Service");

@ArchTest
static final ArchRule repositories_korrekt_benannt =
    classes()
        .that().implement(JpaRepository.class)
        .should().haveSimpleNameEndingWith("Repository");

@ArchTest
static final ArchRule dtos_sind_records =
    classes()
        .that().resideInAPackage("..api..")
        .and().haveSimpleNameEndingWith("Dto")
        .should().beRecords()
        .because("DTOs sind immutable Records");
```

---

## 7. Cycle Detection

```java
@ArchTest
static final ArchRule keine_zyklen_zwischen_modulen =
    SlicesRuleDefinition.slices()
        .matching("de.volantic.erp.(*)..")
        .should().beFreeOfCycles()
        .because("Keine zyklischen Abhängigkeiten zwischen Modulen");
```

---

## Common Violations and Fixes

| Violation | Cause | Fix |
|-----------|-------|-----|
| Domain imports JPA `@Entity` | Domain class wrongly annotated | Move to `infrastructure.persistence` |
| Controller returns Entity | Missing DTO mapping | Create `*Dto` record, map in Controller |
| `verkauf` imports `stammdaten` repository | Module boundary violation | Call `stammdaten` public service or use event |
| Cycle between `lager` ↔ `verkauf` | Direct reference in both directions | Introduce event or extract to `core` |
| `@Service` in api package | Controller with business logic | Extract to application service |

---

## Running Architecture Tests

```bash
# Run only ArchUnit tests
./gradlew test --tests "*.VolanticArchitectureTest"

# Run full check (includes ArchUnit)
./gradlew check

# On violation: output shows exact class + rule that failed
```
