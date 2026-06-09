---
name: spring-modulith-patterns
description: Spring Modulith patterns for Volantic ERP — domain events, module boundaries, @ApplicationModuleTest, Aggregate navigation. Use when implementing cross-module communication, writing module tests, or reviewing module structure.
---

# Spring Modulith Patterns Skill

Volantic ERP uses Spring Modulith for a modular monolith architecture. Each fachmodul is a Spring module package. Communication between modules happens exclusively via in-JVM domain events (ACID).

## When to Use
- Implementing cross-module communication
- Writing `@ApplicationModuleTest`
- Reviewing module structure for boundary violations
- Designing new module APIs

---

## Module Structure

```
de.volantic.erp/
├── iam/            # M0 — Auth, RBAC
├── stammdaten/     # M1 — CRM, Lieferanten, Artikel
├── lager/          # M3 — Bestände, Warenbewegungen
├── verkauf/        # M4 — Angebote, Aufträge, Rechnungen
├── hr/             # M5 — Personalakten, Urlaub
├── buchhaltung/    # M6 — Debitoren, Kreditoren, DATEV
└── core/           # Querschnitt: workflow, belege, audit, sdk
```

Each module has internal sub-packages:
```
<module>/
├── domain/         # Entities, Value Objects, Domain Services
├── application/    # Use Cases, Application Services, Event Handlers
├── infrastructure/ # Repositories, Adapters, External APIs
└── api/            # DTOs, REST Controllers (inbound only)
```

---

## Domain Events (Cross-Module Communication)

### Publishing Events
```java
// ✅ Use ApplicationEventPublisher (Spring Modulith-aware)
@Service
@RequiredArgsConstructor
public class KundeService {

    private final ApplicationEventPublisher events;

    @Transactional
    public Kunde kundeAnlegen(KundeAnlegenCommand cmd) {
        var kunde = new Kunde(cmd.name(), cmd.email());
        repository.save(kunde);
        events.publishEvent(new KundeAngelegtEvent(kunde.getId(), kunde.getName()));
        return kunde;
    }
}
```

### Defining Events
```java
// Events are plain records — no Spring dependency in domain
public record KundeAngelegtEvent(KundeId kundeId, String name) {}

// For external module consumption, use @Externalized (Spring Modulith)
@Externalized("volantic.stammdaten.kunde-angelegt::#{#this.kundeId()}")
public record KundeAngelegtEvent(KundeId kundeId, String name) {}
```

### Consuming Events
```java
// ✅ @ApplicationModuleListener ensures transactional event processing
@Component
public class LagerEventHandler {

    @ApplicationModuleListener
    void on(KundeAngelegtEvent event) {
        // Runs in own transaction after publisher commits
        lagerService.kundenstammAnlegen(event.kundeId());
    }
}
```

**Rules:**
- `@ApplicationModuleListener` statt `@EventListener` für Cross-Module-Events
- Events sind immutable Records
- Events enthalten keine JPA-Entities — nur IDs und primitive Werte

---

## Module Boundaries — What's Allowed

```java
// ✅ Module exposes its API via a public service in the root package
package de.volantic.erp.stammdaten;

@Service
public class KundeQueryService {
    public KundeDto findById(KundeId id) { ... }
}

// ✅ Other modules can call public services
package de.volantic.erp.verkauf.application;

@Service
@RequiredArgsConstructor
public class AngebotService {
    private final KundeQueryService kundeQuery;  // ✅ public module API
}

// ❌ NEVER reach into another module's internal packages
import de.volantic.erp.stammdaten.infrastructure.KundeJpaRepository;  // ❌
import de.volantic.erp.stammdaten.domain.KundeEntity;  // ❌
```

---

## @ApplicationModuleTest

```java
@ApplicationModuleTest  // Boots only this module + declared dependencies
class StammdatenModuleTest {

    @Test
    void kundeAnlegen_publishesEvent(Scenario scenario) {
        scenario.stimulate(app ->
            app.getBean(KundeService.class)
               .kundeAnlegen(new KundeAnlegenCommand("ACME GmbH", "info@acme.de"))
        )
        .andWaitForEventOfType(KundeAngelegtEvent.class)
        .toArriveAndVerify(event ->
            assertThat(event.name()).isEqualTo("ACME GmbH")
        );
    }
}
```

**Scenarios (Spring Modulith DSL):**
- `scenario.stimulate(...)` — triggers application logic
- `.andWaitForEventOfType(...)` — asserts event was published
- `.toArriveAndVerify(...)` — verifies event payload

---

## Aggregate Design (jMolecules)

```java
import org.jmolecules.ddd.annotation.*;

@AggregateRoot
public class Kunde {

    @Identity
    private KundeId id;

    private String name;
    private Email email;

    // ✅ Value Object, not a raw String
    public record KundeId(UUID value) implements Identifier {}
}

// ✅ Repository only for Aggregate Roots
@Repository
interface KundeRepository extends JpaRepository<Kunde, KundeId> {}
```

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Direct module-to-module repo calls | Publish event or call public API |
| JPA Entity in event payload | Use ID only, receiver fetches if needed |
| `@EventListener` for cross-module | Use `@ApplicationModuleListener` |
| Logic in infrastructure layer | Move to domain/application service |
| Mutable event records | Make events immutable records |
| Missing `@Transactional` on event publisher | Publisher must commit before event fires |

---

## ArchUnit Rules (CI-Gate)

These rules are enforced via `./gradlew check`:

```java
// Module packages don't access other module internals
noClasses()
    .that().resideInAPackage("..stammdaten..")
    .should().accessClassesThat()
    .resideInAPackage("..lager.infrastructure..");

// Domain has no infrastructure imports
noClasses()
    .that().resideInAPackage("..domain..")
    .should().dependOnClassesThat()
    .resideInAPackage("..infrastructure..");

// No JPA entities in API responses
noMethods()
    .that().areDeclaredInClassesThat().resideInAPackage("..api..")
    .should().haveRawReturnType(assignableTo(BaseEntity.class));
```
