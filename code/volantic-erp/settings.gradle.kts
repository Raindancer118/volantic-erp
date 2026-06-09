plugins {
    // Erlaubt Gradle, das benötigte Java-Toolchain (21) bei Bedarf automatisch zu beschaffen,
    // unabhängig vom JDK, mit dem Gradle selbst gestartet wird.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "volantic-erp"

// Fachliche Module sind Spring-Modulith-*Packages* innerhalb dieses einen Gradle-Moduls
// (Grenzen erzwungen über ApplicationModules.verify() + ArchUnit, nicht über Gradle).
// Eigene Gradle-Subprojekte folgen erst für publizierte Artefakte — zuerst das volantic-erp-sdk
// (SPI) zu M3. Siehe ADR-0003.
