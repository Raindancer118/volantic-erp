# Changelog

All notable changes to Volantic ERP, newest first.
Each entry: `date` `type(scope)` (commit) — summary, with optional details indented below.

<!-- CHANGELOG:INSERT -->
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
