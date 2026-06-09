package de.volantic.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

/**
 * Entry point of the Volantic ERP modulith.
 *
 * The business building blocks live as Spring Modulith application modules in the direct
 * sub-packages of this package (e.g. {@code core}, {@code security}). Their boundaries are enforced
 * via {@link org.springframework.modulith.core.ApplicationModules#verify()} (see
 * {@code ModularityTests}) as a CI gate — not via Gradle sub-projects.
 */
@Modulithic(systemName = "Volantic ERP")
@SpringBootApplication
public class VolanticErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(VolanticErpApplication.class, args);
    }
}
