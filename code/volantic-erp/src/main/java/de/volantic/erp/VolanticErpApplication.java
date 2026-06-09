package de.volantic.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

/**
 * Einstiegspunkt des Volantic-ERP-Modulith.
 *
 * <p>Die fachlichen Bausteine liegen als Spring-Modulith-Anwendungsmodule in den direkten
 * Sub-Packages dieses Packages (z. B. {@code core}, {@code security}). Ihre Grenzen werden über
 * {@link org.springframework.modulith.core.ApplicationModules#verify()} (siehe
 * {@code ModularityTests}) als CI-Gate erzwungen — nicht über Gradle-Subprojekte.
 */
@Modulithic(systemName = "Volantic ERP")
@SpringBootApplication
public class VolanticErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(VolanticErpApplication.class, args);
    }
}
