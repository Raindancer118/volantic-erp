package de.volantic.erp.crm.api;

import de.volantic.erp.core.web.PageResponse;
import de.volantic.erp.crm.application.CustomerService;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
import de.volantic.erp.crm.domain.model.OrgUnitId;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST v1 endpoints for customers. Thin adapter: it maps DTOs and delegates to {@link CustomerService},
 * which enforces authorization. Versioned under {@code /v1} (additive-only policy).
 */
@RestController
@RequestMapping("/v1/crm/customers")
class CustomerController {

    private final CustomerService customers;

    CustomerController(CustomerService customers) {
        this.customers = customers;
    }

    @PostMapping
    ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        OrgUnitId orgUnitId = request.orgUnitId() != null ? new OrgUnitId(request.orgUnitId()) : OrgUnitId.DEFAULT;
        Customer created = customers.createCustomer(orgUnitId, request.customerNumber(), request.name(), request.email());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id().value()).toUri();
        return ResponseEntity.created(location).body(CustomerResponse.from(created));
    }

    @GetMapping("/{id}")
    CustomerResponse getById(@PathVariable UUID id) {
        return CustomerResponse.from(customers.getCustomer(new CustomerId(id)));
    }

    @GetMapping
    PageResponse<CustomerResponse> list(@PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.of(customers.listCustomers(pageable), CustomerResponse::from);
    }

    @PutMapping("/{id}")
    CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCustomerRequest request) {
        return CustomerResponse.from(customers.updateCustomer(new CustomerId(id), request.name(), request.email()));
    }
}
