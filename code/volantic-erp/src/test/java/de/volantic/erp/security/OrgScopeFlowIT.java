package de.volantic.erp.security;

import de.volantic.erp.crm.application.ContactService;
import de.volantic.erp.crm.application.CustomerService;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
import de.volantic.erp.crm.domain.model.PartnerRef;
import de.volantic.erp.crm.domain.model.PartnerType;
import de.volantic.erp.security.application.OrgUnitService;
import de.volantic.erp.security.domain.model.OrgUnit;
import de.volantic.erp.security.domain.model.OrgUnitId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end proof of hierarchical org-unit scoping (ADR-0007) against a real PostgreSQL (Testcontainers),
 * driving the <em>real</em> {@code @PreAuthorize} + {@link ScopeEnforcer} through the full Spring context.
 * Unlike every other flow test (which seeds only {@link AccessScope#GLOBAL} grants), this one seeds a
 * <strong>non-global</strong> grant and proves least-privilege actually bites:
 *
 * <ul>
 *   <li>a clerk scoped to one unit may read/write records in that unit, and — hierarchically — in its
 *       descendant units, but is denied records in a sibling unit;</li>
 *   <li>listing is filtered to the clerk's permitted (descendant-expanded) units;</li>
 *   <li>a contact inherits its owning customer's unit, so the same boundary applies to it;</li>
 *   <li>a global admin still sees and writes everything.</li>
 * </ul>
 *
 * <p>Skipped without a Docker daemon; runs in CI.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class OrgScopeFlowIT {

    private static final String ADMIN = "org-scope-admin";
    private static final String CLERK = "org-scope-clerk-a";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static boolean seeded;
    private static UUID unitA;
    private static UUID unitAChild;
    private static UUID unitB;

    @Autowired
    private SecurityAdmin securityAdmin;

    @Autowired
    private OrgUnitService orgUnits;

    @Autowired
    private CustomerService customers;

    @Autowired
    private ContactService contacts;

    @BeforeEach
    void seed() {
        if (!seeded) {
            Set<String> crmPermissions = Set.of(
                    "crm.customer:create", "crm.customer:read", "crm.customer:update", "crm.customer:delete",
                    "crm.contact:read", "crm.contact:write");
            Set<String> adminPermissions = Set.of(
                    "security.orgunit:create", "security.orgunit:read", "security.orgunit:update");

            java.util.stream.Stream.concat(crmPermissions.stream(), adminPermissions.stream())
                    .forEach(key -> securityAdmin.definePermission(key, key));
            // The admin holds everything globally; the clerk holds the CRM permissions, but only within
            // the org unit its assignment is scoped to.
            securityAdmin.defineRole("org-admin", "Org admin",
                    java.util.stream.Stream.concat(crmPermissions.stream(), adminPermissions.stream())
                            .collect(java.util.stream.Collectors.toUnmodifiableSet()));
            securityAdmin.defineRole("crm-clerk", "CRM clerk", crmPermissions);
            securityAdmin.provisionUser(ADMIN, "admin", "admin@volantic.de");
            securityAdmin.provisionUser(CLERK, "clerk", "clerk@volantic.de");
            securityAdmin.assignRole(ADMIN, "org-admin", AccessScope.GLOBAL);

            // Build the scope tree and the baseline data as the global admin.
            authenticateAs(ADMIN);
            unitA = orgUnits.createOrgUnit("UNIT-A", "Unit A", null).id().value();
            unitB = orgUnits.createOrgUnit("UNIT-B", "Unit B", null).id().value();
            unitAChild = orgUnits.createOrgUnit("UNIT-A-CHILD", "Unit A Child", new OrgUnitId(unitA)).id().value();
            SecurityContextHolder.clearContext();

            // The clerk is scoped to UNIT-A only (covering UNIT-A and, hierarchically, UNIT-A-CHILD).
            securityAdmin.assignRole(CLERK, "crm-clerk", AccessScope.orgUnit(unitA));
            seeded = true;
        }
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void clerkCanReadAndWriteInItsOwnUnit() {
        CustomerId inA = asAdminCreateCustomer(unitA, "OS-A-" + unique());

        authenticateAs(CLERK);
        assertThat(customers.getCustomer(inA).orgUnitId()).isEqualTo(unitA);

        // create in its own unit is allowed; update too
        CustomerId created = customers.createCustomer(unitA, "OS-A-" + unique(), "Clerk Co", null).id();
        assertThat(customers.getCustomer(created)).isNotNull();
        customers.updateCustomer(inA, "Renamed By Clerk", null);
    }

    @Test
    void clerkIsDeniedASiblingUnit() {
        CustomerId inB = asAdminCreateCustomer(unitB, "OS-B-" + unique());

        authenticateAs(CLERK);
        assertThatThrownBy(() -> customers.getCustomer(inB)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> customers.updateCustomer(inB, "Nope", null)).isInstanceOf(AccessDeniedException.class);
        // creating in the sibling unit is denied declaratively (@PreAuthorize on the requested unit)
        assertThatThrownBy(() -> customers.createCustomer(unitB, "OS-B-" + unique(), "Sneaky", null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void clerkInheritsScopeOverDescendantUnits() {
        // A grant on UNIT-A (the parent) must hierarchically cover UNIT-A-CHILD.
        CustomerId inChild = asAdminCreateCustomer(unitAChild, "OS-AC-" + unique());

        authenticateAs(CLERK);
        assertThat(customers.getCustomer(inChild).orgUnitId()).isEqualTo(unitAChild);
    }

    @Test
    void listingIsFilteredToThePermittedDescendantExpandedUnits() {
        CustomerId inA = asAdminCreateCustomer(unitA, "OS-LA-" + unique());
        CustomerId inChild = asAdminCreateCustomer(unitAChild, "OS-LC-" + unique());
        CustomerId inB = asAdminCreateCustomer(unitB, "OS-LB-" + unique());

        authenticateAs(CLERK);
        var visibleIds = customers.listCustomers(Pageable.unpaged()).getContent().stream()
                .map(c -> c.id()).toList();

        assertThat(visibleIds).contains(inA, inChild);
        assertThat(visibleIds).doesNotContain(inB);
    }

    @Test
    void contactInheritsItsCustomersUnitForScope() {
        CustomerId ownerInA = asAdminCreateCustomer(unitA, "OS-CA-" + unique());
        CustomerId ownerInB = asAdminCreateCustomer(unitB, "OS-CB-" + unique());
        PartnerRef inAOwner = PartnerRef.of(PartnerType.CUSTOMER, ownerInA.value());
        PartnerRef inBOwner = PartnerRef.of(PartnerType.CUSTOMER, ownerInB.value());

        authenticateAs(CLERK);
        // a contact for a UNIT-A customer is allowed (inherited scope = UNIT-A)
        contacts.createContact(inAOwner, "Erika", "Mustermann", "e@acme.de", null);
        // a contact for a UNIT-B customer is denied (inherited scope = UNIT-B, out of the clerk's reach)
        assertThatThrownBy(() -> contacts.createContact(inBOwner, "Max", "Sneaky", "m@acme.de", null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void globalAdminSeesAndWritesEveryUnit() {
        CustomerId inA = asAdminCreateCustomer(unitA, "OS-GA-" + unique());
        CustomerId inB = asAdminCreateCustomer(unitB, "OS-GB-" + unique());

        authenticateAs(ADMIN);
        assertThat(customers.getCustomer(inA)).isNotNull();
        assertThat(customers.getCustomer(inB)).isNotNull();
        customers.updateCustomer(inB, "Admin Edit", null);
    }

    private CustomerId asAdminCreateCustomer(UUID orgUnitId, String number) {
        authenticateAs(ADMIN);
        try {
            return customers.createCustomer(orgUnitId, number, "Co " + number, null).id();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static void authenticateAs(String oidcSubject) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(oidcSubject, "n/a", AuthorityUtils.NO_AUTHORITIES));
    }
}
