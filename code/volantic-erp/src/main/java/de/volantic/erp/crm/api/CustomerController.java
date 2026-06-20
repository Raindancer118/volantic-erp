package de.volantic.erp.crm.api;

import de.volantic.erp.core.web.ETags;
import de.volantic.erp.core.web.PageResponse;
import de.volantic.erp.crm.application.CustomerService;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
        Customer created = customers.createCustomer(request.customerNumber(), request.name(), request.email());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id().value()).toUri();
        CustomerResponse body = CustomerResponse.from(created);
        return ResponseEntity.created(location).eTag(ETags.format(body.version())).body(body);
    }

    @GetMapping("/{id}")
    ResponseEntity<CustomerResponse> getById(@PathVariable UUID id) {
        return withETag(CustomerResponse.from(customers.getCustomer(new CustomerId(id))));
    }

    @GetMapping
    PageResponse<CustomerResponse> list(@PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.of(customers.listCustomers(pageable), CustomerResponse::from);
    }

    @PutMapping("/{id}")
    ResponseEntity<CustomerResponse> update(@PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody UpdateCustomerRequest request) {
        Customer updated = customers.updateCustomer(
                new CustomerId(id), ETags.parseIfMatch(ifMatch), request.name(), request.email());
        return withETag(CustomerResponse.from(updated));
    }

    private static ResponseEntity<CustomerResponse> withETag(CustomerResponse body) {
        return ResponseEntity.ok().eTag(ETags.format(body.version())).body(body);
    }
}
