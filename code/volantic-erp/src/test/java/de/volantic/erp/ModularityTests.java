package de.volantic.erp;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * CI gate for the module architecture (§3.1/§3.2 master plan, ADR-0001).
 *
 * <p>{@link ApplicationModules#verify()} breaks the build as soon as a business module violates the
 * boundaries of another (e.g. accessing its internal types instead of the exposed API). This analysis
 * works purely on the package/bytecode structure — it starts <em>no</em> Spring context and needs
 * <em>no</em> database.
 */
class ModularityTests {

    static final ApplicationModules modules = ApplicationModules.of(VolanticErpApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        modules.verify();
    }

    @Test
    void writesModuleDocumentation() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
