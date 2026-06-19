# Volantic ERP - Audit Findings

## Stand der Behebung — 2026-06-19 (Branch `feat/bulk-edit-reversible-sessions`)

> Bearbeitet auf diesem Branch; `./gradlew check` grün (alle 56 Testklassen inkl. ArchUnit, Modulith,
> Testcontainers-ITs, Full-Context-Boot). Legende: ✅ behoben · ⚠️ kein/teilweiser Fehler (Begründung) ·
> 🔜 bewusst verschoben (größerer Umbau, separater PR).

**Critical**
- ✅ **Audit-Hash Kanonisierung** — Felder werden jetzt längen-präfixiert (`<byteLen>:<feld>|`) gehasht
  statt mit `String.join("|", …)`; `|` im Payload/Actor kann keine fremde Kanonik mehr fälschen
  (`AuditEntry`, neuer Test `delimiterInFieldsCannotForgeAnotherEntrysCanonicalForm`).
- ✅ **IDOR auf ChangeSets** — `apply/commit/discard/revert` prüfen jetzt `currentActor == changeSet.actor`
  (`ChangeSetAccessDeniedException` → 403); Test `operatingOnAnotherActorsSessionIsRejected`.
- ✅ **SecureRandom Thread-Block** — `UuidV7` nutzt `ThreadLocal<SecureRandom>` statt einer geteilten
  Instanz; CSPRNG-Qualität bleibt, Contention weg.
- ✅ **Quantity-Equality** — `equals/hashCode` jetzt skalen-insensitiv (`compareTo`), `Quantity.of("2")`
  == `Quantity.of("2.0")`; Tests ergänzt.
- 🔜 **Globale Transaktions-Serialisierung (Audit-Lock)** — der `pg_advisory_xact_lock` bis Commit ist die
  *bewusste* Integritäts-Garantie der Hash-Kette. Früh freigeben (REQUIRES_NEW) bräche die Atomarität
  zwischen Fachschreibung und Audit. Echte Entlastung = asynchrone Audit-Pipeline (Outbox) — eigene
  Architektur-Entscheidung, nicht still im Vorbeigehen. **Offen, dokumentiert.**
- ⚠️ **Spring-Cache NullValue-NPE** — `RedisCache` serialisiert `NullValue` über einen eigenen
  Binär-Marker, *unabhängig* vom JSON-Serializer; `@Cacheable` liefert beim Negativ-Treffer `null`, der
  JSON-Serializer sieht das `NullValue`-Objekt nie. Kein NPE-Pfad. Kein Codeänderung nötig.

**High**
- ✅ **Zeit-/Zonen-Drift** — alle `OffsetDateTime.now()` der Zeitquellen auf `now(ZoneOffset.UTC)`
  (`AuditService`, `JpaAuditingConfig`, `ChangeSet`, `ChangeSetService`).
- ✅ **Outbox-Wachstum + fehlende Retries** — `spring.modulith.events.completion-mode: delete` +
  `republish-outstanding-publications-on-restart: true` in `application.yml`.
- ✅ **JWT ohne `aud`-Prüfung** — eigener `JwtDecoder` mit Issuer- *und* (optionaler, `OIDC_AUDIENCE`)
  Audience-Validierung; verhindert Confused-Deputy mit Tokens anderer Apps derselben Authentik.
- ✅ **`verifyIntegrity()` OOM** — verifiziert jetzt in 500er-Seiten (`findAscending(Pageable)`) statt die
  ganze Tabelle in eine `List` zu laden.
- ✅ **Zirkuläre BOMs** — `BomService.createBom` macht jetzt eine Graph-Traversierung (`CircularBom` → 422);
  Tests für direkte und transitive Zyklen.
- ✅ **Un-revertbare Probemodus-Änderungen** — Capture-at-commit: beim „Übertragen" eines DEFERRED-Sets
  wird der `beforeState` erfasst (`RecordedOperation.withBeforeState`), Rollback funktioniert danach;
  Test `commitProbemodusCapturesBeforeStateSoItCanBeReverted`.
- ✅ **equals/hashCode auf Aggregaten** — identitätsbasiert ergänzt (`Customer/Supplier/Product/Address/
  Contact/Bom/ChangeSet`).
- 🔜 **Scope-Access „Fassade" / preview-Exfiltration** — `hasPermission(null, …)` ist bewusster Ist-Stand
  (Org-Scopes sind dokumentierter Folge-Meilenstein). Echte Scope-Auflösung ist ein querschnittlicher
  Umbau aller Services → separater PR.
- 🔜 **Lost-Update / ETag** — valider Punkt; benötigt `@Version`-Durchreichung in DTOs + `If-Match`/412 in
  allen CRUD-Controllern → fokussierter Folge-PR.
- 🔜 **Client- vs. Backend-ID bei CREATE** — latent: der Bulk-Pfad macht aktuell nur UPDATE, ein
  Bulk-CREATE existiert noch nicht. Vor Einführung von CREATE adressieren.

**Medium / Low**
- ✅ **Naive E-Mail-Validierung** — gemeinsamer `EmailAddresses`-Validator (Regex statt `contains("@")`),
  von `Customer` und `Contact` genutzt (DRY).
- ✅ **Naive Ländercode-Validierung** — gegen `Locale.getISOCountries()` geprüft (`XX` fliegt raus).
- ✅ **Null-/Zero-Mengen in BOM-Zeilen** — `BomLine` lehnt `0` ab (`Quantity.isZero()`).
- 🔜 **Orphaned Approval Workflows (Flowable-Autotx)** — valide; sauber nur via
  `TransactionSynchronization` (Start erst `afterCommit`). Eigener PR.
- ⚠️ **Info-Leak im Error-Handler** — die durchgereichten `IllegalArgumentException`-Messages sind
  kontrollierte Domänen-Validierungstexte (z. B. „email is not a valid address"), keine internen
  Klassen-/DB-Details. Akzeptabel; nicht geändert.
- ⚠️ **`CREATE INDEX` ohne `CONCURRENTLY`** — Fehlalarm für diese Migration: der Index entsteht in
  *derselben* Migration auf einer frisch angelegten, leeren Tabelle (Build ist sofort, nichts wird
  blockiert) — und `CONCURRENTLY` darf gar nicht in der Tx mit `CREATE TABLE` laufen.
- ⚠️ **Premature Rounding in `Money.multiply`** — bewusst: jede `Money`-Instanz ist kanonisch gerundet,
  damit `equals` korrekt ist. Tradeoff dokumentiert, nicht geändert.
- ⚠️ **Hardcoded Type-Mapping im Synchronizer** — die Ternary liegt im Link-Graph-Adapter, der genau für
  dieses Mapping zuständig ist; Auslagern in die Domäne würde die Link-Typ-Strings dorthin lecken. Belassen.

---

# (Original-Audit als Snapshot)

## 🚨 CRITICAL (Showstopper)

- **Audit Hash Canonicalization Attack (Spoofing/Tampering)**
  - **File:** `code/volantic-erp/src/main/java/de/volantic/erp/audit/domain/model/AuditEntry.java`
  - **Finding:** The cryptographic `GENESIS_HASH` chain concatenates fields using `String.join("|", ...)`. The inputs (`actor`, `payload`) are completely untrusted and not escaped. 
  - **Impact:** An attacker can insert the `|` character into the payload or actor fields to spoof the canonical string of another entry. This allows malicious actors to rewrite history while generating perfectly valid, verifiable hash chains, entirely defeating the tamper-evident purpose of the audit log.

- **Global Transaction Serialization (Massive Throughput Bottleneck)**
  - **Files:** `AuditLogStoreAdapter.java` & `AuditService.java`
  - **Finding:** `AuditService.record()` is `@Transactional` (joining the parent transaction) and calls `store.lockForAppend()`, which acquires a PostgreSQL transaction-level exclusive lock (`pg_advisory_xact_lock`).
  - **Impact:** Because the lock is held until the parent transaction (e.g. creating a Customer) commits, ALL write operations across the entire ERP system that emit an audit log are strictly serialized. Only one write transaction can execute at a time globally. This completely destroys database concurrency and scalability.

- **IDOR / Broken Access Control on ChangeSets**
  - **File:** `code/volantic-erp/src/main/java/de/volantic/erp/changeset/application/ChangeSetService.java`
  - **Finding:** The methods `commit(ChangeSetId)`, `discard(ChangeSetId)` and `revert(ChangeSetId)` load the session by ID but do **not** verify if the `currentActor()` matches `changeSet.actor()`. 
  - **Impact:** Any authenticated user with `changeset.bulk:execute` permission can secretly commit or discard another user's in-progress Probemodus sessions, or revert them.

- **Massive Thread Block in UUID Generation**
  - **File:** `code/volantic-erp/src/main/java/de/volantic/erp/core/UuidV7.java`
  - **Finding:** Uses a shared `static final SecureRandom RANDOM = new SecureRandom();` to generate IDs for every entity. `SecureRandom.nextBytes()` is internally synchronized on the instance level.
  - **Impact:** In high-throughput scenarios (e.g., bulk inserts or a spike of events), all threads will block continuously on this single intrinsic lock, dropping throughput to near zero and causing severe thread starvation.

- **Spring Cache + Jackson Poisoning (NPE/DoS)**
  - **Files:** `CacheConfig.java` & `UserDirectoryAdapter.java`
  - **Finding:** Negative caching is enabled for unprovisioned users. `Jackson2JsonRedisSerializer` serializes Spring's `NullValue` into an empty JSON object `{}`. When read back, Jackson deserializes this into a `CachedUser` with all fields `null`. Then, `UserDirectoryAdapter` mistakenly maps it and chains `.assignments().stream()`.
  - **Impact:** Results in a `NullPointerException`. Any lookup for a valid but unprovisioned OIDC subject permanently poisons the cache, throwing 500s on all subsequent requests for that user until the 5-minute TTL expires.

- **Broken Equality in Quantity Value Object**
  - **File:** `code/volantic-erp/src/main/java/de/volantic/erp/core/measure/Quantity.java`
  - **Finding:** The `Quantity` VO wraps a `BigDecimal` without normalizing its scale, relying on the auto-generated Java record `equals()` method. This delegates to `BigDecimal.equals()`, which strictly compares both value and scale.
  - **Impact:** `Quantity.of("2").equals(Quantity.of("2.0"))` evaluates to `false`. This silently breaks domain logic, comparisons in collections, and BOM logic whenever quantities are parsed with different scales.

## 🔴 HIGH (Architecture Flaws)

- **Hexagonal Architecture Mismatch (Client vs Backend ID Generation)**
  - **Files:** `ChangeSetService.java` & `Customer.java`
  - **Finding:** The `BulkChange` payload assumes the client provides the `targetIds` upfront. However, domain aggregate factories like `Customer.create()` completely ignore client-provided IDs and forcefully generate a new `UuidV7` on the backend.
  - **Impact:** If a user submits a bulk `CREATE` operation, the newly created entity will get a different UUID than the one stored in the `RecordedOperation`. The `ChangeSet` will point to a dummy/non-existent ID. Later attempts to `commit()` or `revert()` these creations will permanently fail, breaking the entire Rollback Engine for new entities.

- **Scope-Based Access Control is a Facade (Tenant/Org Isolation Broken)**
  - **Files:** `ProductService.java`, `CustomerService.java`, `WebSecurityConfig.java`
  - **Finding:** While the `Security` module meticulously implements `AccessScope` for fine-grained, org-level role assignments, the application services universally hardcode `@PreAuthorize("hasPermission(null, '...')")`.
  - **Impact:** The `null` target entirely bypasses scope resolution. Users either need global permissions or their requests fail. If this system is ever used for multi-tenancy or organizational unit isolation, data leakage is guaranteed because the service layer is blind to data scopes.

- **Time Drift & Zone Offset Inconsistencies**
  - **Files:** `AuditService.java`, `ChangeSet.java`, `JpaAuditingConfig.java`
  - **Finding:** Throughout the application, timestamps are generated using `OffsetDateTime.now()` without an explicit `java.time.Clock` or `ZoneId.of("UTC")`.
  - **Impact:** `OffsetDateTime.now()` falls back to the JVM's default timezone. In a distributed environment (e.g., Kubernetes pods running in different regions, or a developer running it locally), this results in completely unpredictable time offsets (+02:00 vs Z). This breaks cross-node ordering, audit trail timeline reconstruction, and invalidates GoBD chronologies.

- **Unmanaged Outbox Growth (Database Leak)**
  - **Files:** `application.yml` & `CrmEntityLinkSynchronizer.java`
  - **Finding:** Spring Modulith's Event Publication Registry (Outbox) is heavily used to publish events (`@ApplicationModuleListener`). However, `application.yml` does not configure the deletion of completed publications (e.g. `spring.modulith.events.completed-publications.delete.enabled`), nor is there any scheduled cleanup job.
  - **Impact:** Every single domain event ever fired is stored permanently in the `event_publication` table. In an ERP system, this table will balloon into gigabytes/terabytes of dead data, eventually exhausting database storage and causing a system-wide crash.

- **Zero-Downtime Migration Failure (Blocking Index Creation)**
  - **File:** `code/volantic-erp/src/main/resources/db/migration/changeset/V301__changeset_schema.sql`
  - **Finding:** The Flyway script creates an index using `CREATE INDEX idx_change_set_actor_status ...`.
  - **Impact:** In PostgreSQL, a standard `CREATE INDEX` acquires a `ShareLock`, blocking all writes (INSERT/UPDATE/DELETE) to the table until the index is fully built. For large tables, this takes minutes or hours, causing total application downtime during deployment and violating the `Drop-In-Update ≤ 40 min` architecture goal. (Fix: `CREATE INDEX CONCURRENTLY` in a non-transactional migration).

- **Lost Update Anomaly (Missing Optimistic Locking in REST API)**
  - **Files:** `CustomerService.java`, `SupplierService.java`, `ProductService.java`
  - **Finding:** The REST API and Application Services (`updateCustomer`, etc.) do not accept or validate a version/ETag. The service blindly fetches the latest DB state, applies modifications, and saves it.
  - **Impact:** If two users edit the same entity simultaneously, the second save silently overwrites the first user's changes. The `@Version` annotation on the JPA entities is rendered useless for API clients, resulting in classic Lost Updates.

- **Missing Audience (aud) validation for JWT**
  - **File:** `code/volantic-erp/src/main/java/de/volantic/erp/security/infrastructure/config/WebSecurityConfig.java`
  - **Finding:** `.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))` is used without any custom JWT decoder or audience validator.
  - **Impact:** Confused Deputy vulnerability. Tokens minted for entirely different applications on the same Authentik instance can be used to authenticate against Volantic ERP.

- **Authorization Bypass (Data Exfiltration) via ChangeSet preview()**
  - **File:** `code/volantic-erp/src/main/java/de/volantic/erp/changeset/application/ChangeSetService.java`
  - **Finding:** `preview()` iterates over `ReversibleResourceHandler.capture(id)` to return the "before state" to the user, but only checks for the global `changeset.bulk:execute` permission.
  - **Impact:** A user with bulk edit permission can read the full state of *any* entity in the system, completely bypassing module-specific read authorizations (e.g. `crm.customer:read`).

- **Circular BOM Dependencies Not Prevented**
  - **File:** `code/volantic-erp/src/main/java/de/volantic/erp/catalog/application/BomService.java`
  - **Finding:** `createBom` checks if component products exist but fails to verify if the components recursively depend back on the parent product.
  - **Impact:** Allows the creation of circular dependencies (Product A needs Product B, which needs Product A), which will cause infinite loops (StackOverflow) during MRP evaluation or cost rollups, taking down the application.

- **Data Loss / Un-revertable Probemodus Changes**
  - **File:** `code/volantic-erp/src/main/java/de/volantic/erp/changeset/application/ChangeSetService.java`
  - **Finding:** When a `DEFERRED` session is recorded during `apply()`, the `beforeState` is set to `null` because no DB write happens yet. However, when `commit()` is called, the operations are applied but the `beforeState` is never captured/updated in the `RecordedOperation` (missing capture-at-commit).
  - **Impact:** If a user commits a Probemodus session and later attempts to use the Rollback Engine (`revert()`), the rollback fails or writes `null` into the database, corrupting the data.

- **Missing Pagination Leading to System Crash (OOM)**
  - **Files:** `AuditService.java` & `AuditLogJpaRepository.java`
  - **Finding:** The method `verifyIntegrity()` (exposed via `@GetMapping("/verify")`) calls `store.findAllOrdered()`, fetching the *entire* audit log table into an unbounded `List<AuditEntry>` in memory.
  - **Impact:** Audit tables grow endlessly. Fetching 100,000+ entries at once will cause an immediate `OutOfMemoryError` (OOM), crashing the JVM.

- **Missing equals() and hashCode() on DDD Aggregate Roots**
  - **Files:** `Product.java`, `Customer.java`, `Supplier.java`, `Address.java`, `Contact.java`, `Bom.java`, `ChangeSet.java`
  - **Finding:** Pure domain aggregate roots do not extend `AbstractEntity` and do not override `equals()`/`hashCode()` based on their aggregate IDs.
  - **Impact:** Two separate instances loaded from the DB representing the exact same business entity will evaluate to `false` (`==` reference equality), breaking standard collections and mapping logic.

## 🟡 MEDIUM (Logic & Resilience)

- **Orphaned Approval Workflows (Missing Transaction Sync)**
  - **File:** `code/volantic-erp/src/main/java/de/volantic/erp/workflow/application/ApprovalService.java`
  - **Finding:** Flowable manages its own autonomous transactions. If `requestApproval()` is called inside a business transaction that subsequently rolls back, the workflow engine still commits the process start.
  - **Impact:** Approvals run for business entities that never actually persisted in the database, confusing users and leaving dead processes.

- **Information Leakage in Error Handlers**
  - **File:** `code/volantic-erp/src/main/java/de/volantic/erp/crm/api/CrmExceptionHandler.java`
  - **Finding:** The global exception handler catches `IllegalArgumentException` and unmaskedly returns `exception.getMessage()` to the client as an HTTP 400 ProblemDetail.
  - **Impact:** Leaks internal class names, stack context, or database IDs to the client.

- **Unhandled Event Exceptions & Missing Fallbacks in Outbox**
  - **File:** `code/volantic-erp/src/main/java/de/volantic/erp/crm/infrastructure/link/CrmEntityLinkSynchronizer.java`
  - **Finding:** The event listeners use `@ApplicationModuleListener` but blindly call `links.link(...)` with no fallback/retry.
  - **Impact:** If the core `EntityLinkRegistry` fails, the exception is swallowed by Modulith's outbox. Without a retry scheduler, the 360° graph data will silently go out of sync forever.

- **Zero-Quantity BOM Lines Allowed**
  - **File:** `code/volantic-erp/src/main/java/de/volantic/erp/catalog/domain/model/BomLine.java`
  - **Finding:** The constructor validation only checks `quantity.isNegative()`, allowing a quantity of exactly `0`.
  - **Impact:** Breaks downstream Material Requirements Planning (MRP) algorithms or causes divide-by-zero explosions.

- **Naive Email Validation**
  - **Files:** `Customer.java`, `Contact.java`
  - **Finding:** Validation solely relies on `!normalized.contains("@")`.
  - **Impact:** Syntactically broken addresses bypass the guard, polluting the system.

- **Naive Country Code Validation**
  - **File:** `code/volantic-erp/src/main/java/de/volantic/erp/crm/domain/model/Address.java`
  - **Finding:** Country Code validation merely checks `value.length() != 2`.
  - **Impact:** Allows entirely fake ISO codes (e.g., `XX`) to be persisted.

## 🟢 LOW / COSMETIC (Clean Code & Smells)

- **Premature Rounding in Money Math**
  - **File:** `code/volantic-erp/src/main/java/de/volantic/erp/core/measure/Money.java`
  - **Finding:** `multiply(BigDecimal)` instantly re-applies banker's rounding.
  - **Impact:** Compounding precision loss during intermediate calculations.

- **Hardcoded Type Mapping Logic**
  - **File:** `code/volantic-erp/src/main/java/de/volantic/erp/crm/infrastructure/link/CrmEntityLinkSynchronizer.java`
  - **Finding:** Manually reconstructs type strings using a ternary operator (`owner.type() == PartnerType.CUSTOMER ? "crm.customer" : "crm.supplier"`).
  - **Impact:** Leaks module representation details.
