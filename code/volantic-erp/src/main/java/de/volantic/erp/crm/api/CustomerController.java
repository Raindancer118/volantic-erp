package de.volantic.erp.crm.api;

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
        return withETag(ResponseEntity.created(location), created);
    }

    @GetMapping("/{id}")
    ResponseEntity<CustomerResponse> getById(@PathVariable UUID id) {
        return withETag(ResponseEntity.ok(), customers.getCustomer(new CustomerId(id)));
    }

    @GetMapping
    PageResponse<CustomerResponse> list(@PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.of(customers.listCustomers(pageable), CustomerResponse::from);
    }

    @PutMapping("/{id}")
    ResponseEntity<CustomerResponse> update(@PathVariable UUID id,
                                            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
                                            @Valid @RequestBody UpdateCustomerRequest request) {
        long expectedVersion = parseVersion(ifMatch);
        Customer updated = customers.updateCustomer(new CustomerId(id), expectedVersion, request.name(), request.email());
        return withETag(ResponseEntity.ok(), updated);
    }

    /** Sets the strong {@code ETag} header from the customer's version and writes the response body. */
    private static ResponseEntity<CustomerResponse> withETag(ResponseEntity.BodyBuilder builder, Customer customer) {
        if (customer.version() != null) {
            builder.eTag("\"" + customer.version() + "\"");
        }
        return builder.body(CustomerResponse.from(customer));
    }

    /** Parses the version out of an {@code If-Match} header value (e.g. {@code "5"} or {@code W/"5"}). */
    private static long parseVersion(String ifMatch) {
        String value = ifMatch.strip();
        if (value.startsWith("W/")) {
            value = value.substring(2).strip();
        }
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        try {
            return Long.parseLong(value.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("If-Match must carry the resource version, got: " + ifMatch);
        }
    }
}
