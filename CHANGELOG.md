# Changelog

All notable changes to Volantic ERP, newest first.
Each entry: `date` `type(scope)` (commit) — summary, with optional details indented below.

<!-- CHANGELOG:INSERT -->
- 2026-06-09 `feat(security)` (76689ab) — administrative write side with cache eviction
  - new SecurityAdmin API (root): definePermission, defineRole, provisionUser (idempotent), setUserStatus, assignRole
  - application SecurityAdminService orchestrates use cases; SecurityWriteStore outbound port persists via JPA in the adapter
  - authorization cache is evicted AFTER commit (TransactionSynchronization) per affected subject — role permission changes clear the whole cache; provisioning clears a prior negative-cache entry
  - tests: service unit (idempotency/delegation), adapter eviction (in-memory cache), Postgres roundtrip IT (define -> provision -> assign -> read, disable revokes) 
- 2026-06-09 `fix(security)` (b28f2b6) — negative-cache unmirrored OIDC subjects to shield the database
  - previously unless=#result==null skipped caching unknown subjects, so a validly signed but not-yet-mirrored subject hit the DB on every request (cache-bypass / DoS lever, Findings.md)
  - now null results are cached too (removed unless + disableCachingNullValues); TTL bounds staleness and the write side will evict on provisioning 
- 2026-06-09 `fix(core)` (7fe8013) — assign entity UUID only for new aggregates, not on every load
  - AbstractEntity no longer initialises the id in the field/no-arg constructor — Hibernate's load path used it to generate a SecureRandom UUIDv7 that was immediately overwritten by the row, wasting CPU on the read hot path (sub-500 ms NFR)
  - new aggregates assign the id via super(true) in the business constructor; equals/hashCode made null-safe
  - added hibernate.default_batch_fetch_size=100 as an N+1 safety net 
- 2026-06-09 `feat(security)` (5335e80) — cache authorization snapshot in Redis
  - per-OIDC-subject snapshot (CachedUser) cached in Redis with a 5-min TTL; decision still runs in the pure domain
  - UserGraphCache loads via single @EntityGraph query on a miss, served from cache on a hit (sub-500 ms NFR)
  - fail-open: cache errors degrade to the database (CacheErrorHandler), Redis connects lazily so startup is unaffected
  - unknown subjects are not negative-cached
  - TODO: explicit eviction once a write side exists 
- 2026-06-09 `docs(changelog)` (3924bfa) — support indented detail lines and expand entries
  - changelog.sh gains an optional details arg (split on '
  - ' into indented bullets)
  - rebuilt CHANGELOG with fuller per-change descriptions 
- 2026-06-09 `build(tooling)` (44f7b3c) — changelog script + project conventions
  - scripts/changelog.sh maintains CHANGELOG.md (newest-first, marker insert, optional indented details)
  - documented two standing conventions in CLAUDE.md: keep the changelog current on every commit, and develop code in English 
- 2026-06-09 `docs(adr)` (5ecf378) — ADR-0005: bundled Authentik IdP per installation
  - each self-hosted customer install ships and provisions (blueprint) its own Authentik
  - ERP stays a pure, env-driven OIDC resource server
  - auth.volantic.de is only the dev/demo default
  - bring-your-own IdP remains possible via the same env vars 
- 2026-06-09 `refactor` (24ac004) — translate the entire codebase to English
  - all identifiers, Javadoc/comments, test method names, ArchUnit rule names + messages, and SQL/YAML/properties comments
  - behaviour and API unchanged
  - docs (CLAUDE.md, ADRs) intentionally stay German 
- 2026-06-09 `feat(security)` (bc0f387) — enforce user status + resilient OIDC decoder
  - DISABLED users now hold no permission regardless of roles (decided in the domain)
  - added jwk-set-uri so the app boots with a lazy JWT decoder even when Authentik is briefly unreachable, while still validating the issuer
  - aligned module name to security (was iam) 
- 2026-06-09 `feat(security)` (c5b2043) — M0 foundation + Security/RBAC module
  - Spring Modulith scaffold (Java 25, Spring Boot 3.5.8, Gradle Kotlin DSL)
  - Security/RBAC behind the central AuthorizationService port (ADR-0004): RBAC + field + instance/scope levels
  - hexagonal per module: pure domain, application service, JPA adapter
  - Flyway security schema, OIDC resource server (Authentik), private GitHub repo + CI (build/test/ArchUnit/Modulith verify) 
