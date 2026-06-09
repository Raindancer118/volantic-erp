---
name: gradle-dependency-audit
description: Audit Gradle (Kotlin DSL) dependencies for outdated versions, security vulnerabilities, and conflicts. Use when user says "check dependencies", "audit dependencies", "outdated deps", or before releases.
---

# Gradle Dependency Audit Skill

Audit Gradle (Kotlin DSL) dependencies for updates, vulnerabilities, and conflicts.
This project uses **Gradle with Kotlin DSL** (`build.gradle.kts`). All commands use `./gradlew`.

## When to Use
- User says "check dependencies" / "audit dependencies" / "outdated dependencies"
- Before a release
- Regular maintenance (monthly recommended)
- After security advisory

## Audit Workflow

1. **Check for updates** - Find outdated dependencies
2. **Analyze tree** - Find conflicts and duplicates
3. **Security scan** - Check for vulnerabilities
4. **Report** - Summary with prioritized actions

---

## 1. Check for Outdated Dependencies

### Command (ben-manes versions plugin)
```bash
./gradlew dependencyUpdates
```

Add to `build.gradle.kts` if not present:
```kotlin
plugins {
    id("com.github.ben-manes.versions") version "0.51.0"
}
```

### Output Analysis
```
The following dependencies have later milestone versions:
 - org.slf4j:slf4j-api [1.7.36 -> 2.0.13]
 - com.fasterxml.jackson.core:jackson-databind [2.14.0 -> 2.17.0]
```

### Categorize Updates

| Category | Criteria | Action |
|----------|----------|--------|
| **Security** | CVE fix in newer version | Update ASAP |
| **Major** | x.0.0 change | Review changelog, test thoroughly |
| **Minor** | x.y.0 change | Usually safe, test |
| **Patch** | x.y.z change | Safe, minimal testing |

---

## 2. Analyze Dependency Tree

### Full Tree
```bash
./gradlew dependencies
```

### Specific Configuration
```bash
./gradlew dependencies --configuration runtimeClasspath
./gradlew dependencies --configuration testRuntimeClasspath
```

### Filter for Specific Dependency
```bash
./gradlew dependencyInsight --dependency slf4j-api --configuration runtimeClasspath
```

### Find Conflicts
Look for forced resolutions:
```
org.slf4j:slf4j-api:1.7.36 -> 2.0.13 (conflict resolution)
```

### Force a Specific Version (Resolve Conflict)
```kotlin
configurations.all {
    resolutionStrategy {
        force("org.slf4j:slf4j-api:2.0.13")
    }
}
```

Or via BOM:
```kotlin
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.8"))
}
```

---

## 3. Security Vulnerability Scan

### Option A: OWASP Dependency-Check (Recommended)

Add to `build.gradle.kts`:
```kotlin
plugins {
    id("org.owasp.dependencycheck") version "10.0.4"
}
```

Run:
```bash
./gradlew dependencyCheckAnalyze
```

Report at `build/reports/dependency-check-report.html`

### Option B: GitHub Dependabot
Enable Dependabot alerts in repository settings (`.github/dependabot.yml`):
```yaml
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
```

### Severity Levels

| CVSS Score | Severity | Action |
|------------|----------|--------|
| 9.0 - 10.0 | Critical | Update immediately |
| 7.0 - 8.9 | High | Update within days |
| 4.0 - 6.9 | Medium | Update within weeks |
| 0.1 - 3.9 | Low | Update at convenience |

---

## 4. Generate Audit Report

### Output Format

```markdown
## Dependency Audit Report

**Project:** Volantic ERP
**Date:** {date}
**Total Dependencies:** {count}

### Security Issues

| Dependency | Current | CVE | Severity | Fixed In |
|------------|---------|-----|----------|----------|
| log4j-core | 2.14.0 | CVE-2021-44228 | Critical | 2.17.1 |

### Outdated Dependencies

#### Major Updates (Review Required)
| Dependency | Current | Latest | Notes |
|------------|---------|--------|-------|
| slf4j-api | 1.7.36 | 2.0.13 | API changes, see migration guide |

#### Minor/Patch Updates (Safe)
| Dependency | Current | Latest |
|------------|---------|--------|
| junit-jupiter | 5.9.0 | 5.10.1 |
| jackson-databind | 2.14.0 | 2.17.0 |

### Conflicts Detected
- slf4j-api: resolved to 2.0.13 via Spring Boot BOM

### Recommendations
1. **Immediate:** Fix any Critical CVEs
2. **This sprint:** Update minor/patch versions
3. **Plan:** Evaluate major version migrations
```

---

## Common Scenarios

### Scenario: Check Before Release
```bash
./gradlew dependencyUpdates
./gradlew dependencyCheckAnalyze
```

### Scenario: Find Why Dependency is Included
```bash
./gradlew dependencyInsight --dependency commons-logging --configuration runtimeClasspath
```

### Scenario: Exclude Transitive Dependency
```kotlin
dependencies {
    implementation("com.example:some-library:1.0") {
        exclude(group = "commons-logging", module = "commons-logging")
    }
}
```

---

## Token Optimization

- Use `--configuration runtimeClasspath` to narrow the tree
- Use `dependencyInsight` for a specific dep rather than the full tree
- Summarize findings instead of pasting raw Gradle output

## Quick Commands Reference

| Task | Command |
|------|---------|
| Outdated deps | `./gradlew dependencyUpdates` |
| Full dep tree | `./gradlew dependencies` |
| Runtime tree | `./gradlew dependencies --configuration runtimeClasspath` |
| Specific dep | `./gradlew dependencyInsight --dependency <name>` |
| Security scan | `./gradlew dependencyCheckAnalyze` |
| Build | `./gradlew build` |
| Tests | `./gradlew test` |
