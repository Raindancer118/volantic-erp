package de.volantic.erp.crm.api;

import de.volantic.erp.core.web.ETags;
import de.volantic.erp.core.web.PageResponse;
import de.volantic.erp.crm.application.SupplierService;
import de.volantic.erp.crm.domain.model.Supplier;
import de.volantic.erp.crm.domain.model.SupplierId;
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

/** REST v1 endpoints for suppliers. Thin adapter; {@link SupplierService} enforces authorization. */
@RestController
@RequestMapping("/v1/crm/suppliers")
class SupplierController {

    private final SupplierService suppliers;

    SupplierController(SupplierService suppliers) {
        this.suppliers = suppliers;
    }

    @PostMapping
    ResponseEntity<SupplierResponse> create(@Valid @RequestBody CreateSupplierRequest request) {
        Supplier created = suppliers.createSupplier(request.supplierNumber(), request.name(), request.email());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id().value()).toUri();
        SupplierResponse body = SupplierResponse.from(created);
        return ResponseEntity.created(location).eTag(ETags.format(body.version())).body(body);
    }

    @GetMapping("/{id}")
    ResponseEntity<SupplierResponse> getById(@PathVariable UUID id) {
        return withETag(SupplierResponse.from(suppliers.getSupplier(new SupplierId(id))));
    }

    @GetMapping
    PageResponse<SupplierResponse> list(@PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.of(suppliers.listSuppliers(pageable), SupplierResponse::from);
    }

    @PutMapping("/{id}")
    ResponseEntity<SupplierResponse> update(@PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody UpdateSupplierRequest request) {
        Supplier updated = suppliers.updateSupplier(
                new SupplierId(id), ETags.parseIfMatch(ifMatch), request.name(), request.email());
        return withETag(SupplierResponse.from(updated));
    }

    private static ResponseEntity<SupplierResponse> withETag(SupplierResponse body) {
        return ResponseEntity.ok().eTag(ETags.format(body.version())).body(body);
    }
}
