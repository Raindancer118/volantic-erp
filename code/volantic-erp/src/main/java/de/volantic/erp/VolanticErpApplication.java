package de.volantic.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point of the Volantic ERP modulith.
 *
 * The business building blocks live as Spring Modulith application modules in the direct
 * sub-packages of this package (e.g. {@code core}, {@code security}). Their boundaries are enforced
 * via {@link org.springframework.modulith.core.ApplicationModules#verify()} (see
 * {@code ModularityTests}) as a CI gate — not via Gradle sub-projects.
 *
 * <p>{@link EnableAsync} activates the asynchronous delivery used by {@code @ApplicationModuleListener},
 * whose publications are persisted in the event publication registry (Outbox) for durability.
 */
@Modulithic(systemName = "Volantic ERP")
@SpringBootApplication
@EnableAsync
public class VolanticErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(VolanticErpApplication.class, args);
    }
}
