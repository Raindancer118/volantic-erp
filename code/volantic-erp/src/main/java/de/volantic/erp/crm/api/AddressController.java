package de.volantic.erp.crm.api;

import de.volantic.erp.crm.application.AddressService;
import de.volantic.erp.crm.domain.model.Address;
import de.volantic.erp.crm.domain.model.AddressId;
import de.volantic.erp.crm.domain.model.PartnerRef;
import de.volantic.erp.crm.domain.model.PartnerType;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** REST v1 endpoints for partner addresses. {@link AddressService} enforces authorization. */
@RestController
@RequestMapping("/v1/crm/addresses")
class AddressController {

    private final AddressService addresses;

    AddressController(AddressService addresses) {
        this.addresses = addresses;
    }

    @PostMapping
    ResponseEntity<AddressResponse> create(@Valid @RequestBody AddressRequest request) {
        Address created = addresses.createAddress(
                PartnerRef.of(request.ownerType(), request.ownerId()), request.type(),
                request.street(), request.postalCode(), request.city(), request.countryCode());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id().value()).toUri();
        return ResponseEntity.created(location).body(AddressResponse.from(created));
    }

    @GetMapping("/{id}")
    AddressResponse getById(@PathVariable UUID id) {
        return AddressResponse.from(addresses.getAddress(new AddressId(id)));
    }

    /** Lists addresses of one owner, e.g. {@code GET /v1/crm/addresses?ownerType=CUSTOMER&ownerId=...}. */
    @GetMapping
    List<AddressResponse> listByOwner(@RequestParam PartnerType ownerType, @RequestParam UUID ownerId) {
        return addresses.listAddresses(PartnerRef.of(ownerType, ownerId)).stream()
                .map(AddressResponse::from).toList();
    }

    @PutMapping("/{id}")
    AddressResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateAddressRequest request) {
        Address updated = addresses.updateAddress(new AddressId(id), request.type(),
                request.street(), request.postalCode(), request.city(), request.countryCode());
        return AddressResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        addresses.deleteAddress(new AddressId(id));
        return ResponseEntity.noContent().build();
    }
}
