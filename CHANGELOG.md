# Changelog

All notable changes to Volantic ERP, newest first.
Each entry: `date` `type(scope)` (commit) — summary.

<!-- CHANGELOG:INSERT -->
- 2026-06-09 `docs(adr)` (5ecf378) — add ADR-0005: bundled Authentik IdP per self-hosted installation
- 2026-06-09 `refactor` (24ac004) — translate the entire codebase (identifiers + comments) to English
- 2026-06-09 `feat(security)` (bc0f387) — enforce user status (DISABLED -> no permissions) and add jwk-set-uri for a resilient, lazy JWT decoder
- 2026-06-09 `feat(security)` (c5b2043) — M0 foundation: Spring Modulith setup + Security/RBAC module behind a central Authorization port (ADR-0004)
