plugins {
    java
    id("org.springframework.boot") version "3.5.8"
}

group = "de.volantic"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)   // Java 25 LTS (Temurin)
    }
}

repositories {
    mavenCentral()
}

// Versionen zentral (perspektivisch nach gradle/libs.versions.toml, sobald ein zweites Subprojekt
// — das SDK — dazukommt; siehe ADR-0003).
val springModulithVersion = "1.4.1"
val jmoleculesVersion = "2023.1.5"
val testcontainersVersion = "1.20.4"

dependencies {
    // --- BOMs: managen alle transitiven Versionen, kein dependency-management-Plugin nötig ---
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.8"))
    implementation(platform("org.springframework.modulith:spring-modulith-bom:$springModulithVersion"))
    implementation(platform("org.jmolecules:jmolecules-bom:$jmoleculesVersion"))
    implementation(platform("org.testcontainers:testcontainers-bom:$testcontainersVersion"))

    // --- Web / API ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // --- Persistenz (PostgreSQL + Flyway je Modul) ---
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // --- Security: OIDC Resource Server (Authentik), keine Passwörter in der ERP-DB ---
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // --- Spring Modulith: Modulgrenzen, transaktionale Domain-Events, Beobachtbarkeit ---
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    runtimeOnly("org.springframework.modulith:spring-modulith-actuator")
    runtimeOnly("org.springframework.modulith:spring-modulith-observability")

    // --- jMolecules: DDD-Bausteine als prüfbare Annotationen ---
    implementation("org.jmolecules:jmolecules-ddd")
    implementation("org.jmolecules:jmolecules-events")

    // --- Betrieb ---
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // --- Test ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("org.springframework.modulith:spring-modulith-docs")
    testImplementation("org.jmolecules.integrations:jmolecules-archunit")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
