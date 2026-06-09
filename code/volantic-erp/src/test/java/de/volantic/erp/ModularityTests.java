package de.volantic.erp;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * CI-Gate für die Modul-Architektur (§3.1/§3.2 Masterplan, ADR-0001).
 *
 * <p>{@link ApplicationModules#verify()} bricht den Build, sobald ein Fachmodul die Grenzen eines
 * anderen verletzt (z. B. auf dessen interne Typen zugreift statt auf die exponierte API). Diese
 * Analyse arbeitet rein auf der Package-/Bytecode-Struktur — sie startet <em>keinen</em> Spring-
 * Kontext und braucht <em>keine</em> Datenbank.
 */
class ModularityTests {

    static final ApplicationModules modules = ApplicationModules.of(VolanticErpApplication.class);

    @Test
    void verifiziertModulgrenzen() {
        modules.verify();
    }

    @Test
    void schreibtModuldokumentation() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
