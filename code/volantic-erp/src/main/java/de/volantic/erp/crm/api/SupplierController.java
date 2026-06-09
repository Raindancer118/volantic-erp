package de.volantic.erp.crm.api;

import de.volantic.erp.crm.application.SupplierService;
import de.volantic.erp.crm.domain.model.Supplier;
import de.volantic.erp.crm.domain.model.SupplierId;
import jakarta.validation.Valid;
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
import java.util.List;
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
    List<SupplierResponse> list() {
        return suppliers.listSuppliers().stream().map(SupplierResponse::from).toList();
    }

    @PutMapping("/{id}")
    SupplierResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateSupplierRequest request) {
        return SupplierResponse.from(suppliers.updateSupplier(new SupplierId(id), request.name(), request.email()));
    }
}
