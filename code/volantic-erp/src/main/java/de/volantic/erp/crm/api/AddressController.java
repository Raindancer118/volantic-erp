package de.volantic.erp.crm.api;

import de.volantic.erp.core.web.ETags;
import de.volantic.erp.core.web.PageResponse;
import de.volantic.erp.crm.application.AddressService;
import de.volantic.erp.crm.domain.model.Address;
import de.volantic.erp.crm.domain.model.AddressId;
import de.volantic.erp.crm.domain.model.PartnerRef;
import de.volantic.erp.crm.domain.model.PartnerType;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
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
        AddressResponse body = AddressResponse.from(created);
        return ResponseEntity.created(location).eTag(ETags.format(body.version())).body(body);
    }

    @GetMapping("/{id}")
    ResponseEntity<AddressResponse> getById(@PathVariable UUID id) {
        AddressResponse body = AddressResponse.from(addresses.getAddress(new AddressId(id)));
        return ResponseEntity.ok().eTag(ETags.format(body.version())).body(body);
    }

    /** Lists addresses of one owner, e.g. {@code GET /v1/crm/addresses?ownerType=CUSTOMER&ownerId=...}. */
    @GetMapping
    PageResponse<AddressResponse> listByOwner(@RequestParam PartnerType ownerType,
                                              @RequestParam UUID ownerId,
                                              @PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.of(
                addresses.listAddresses(PartnerRef.of(ownerType, ownerId), pageable), AddressResponse::from);
    }

    @PutMapping("/{id}")
    ResponseEntity<AddressResponse> update(@PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody UpdateAddressRequest request) {
        Address updated = addresses.updateAddress(new AddressId(id), ETags.parseIfMatch(ifMatch), request.type(),
                request.street(), request.postalCode(), request.city(), request.countryCode());
        AddressResponse body = AddressResponse.from(updated);
        return ResponseEntity.ok().eTag(ETags.format(body.version())).body(body);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        addresses.deleteAddress(new AddressId(id));
        return ResponseEntity.noContent().build();
    }
}
