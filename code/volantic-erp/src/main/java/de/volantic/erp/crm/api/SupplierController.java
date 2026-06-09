package de.volantic.erp.crm.api;

import de.volantic.erp.core.web.PageResponse;
import de.volantic.erp.crm.application.SupplierService;
import de.volantic.erp.crm.domain.model.Supplier;
import de.volantic.erp.crm.domain.model.SupplierId;
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
        return ResponseEntity.created(location).body(SupplierResponse.from(created));
    }

    @GetMapping("/{id}")
    SupplierResponse getById(@PathVariable UUID id) {
        return SupplierResponse.from(suppliers.getSupplier(new SupplierId(id)));
    }

    @GetMapping
    PageResponse<SupplierResponse> list(@PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.of(suppliers.listSuppliers(pageable), SupplierResponse::from);
    }

    @PutMapping("/{id}")
    SupplierResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateSupplierRequest request) {
        return SupplierResponse.from(suppliers.updateSupplier(new SupplierId(id), request.name(), request.email()));
    }
}
