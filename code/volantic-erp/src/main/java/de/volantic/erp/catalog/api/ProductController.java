package de.volantic.erp.catalog.api;

import de.volantic.erp.catalog.application.ProductService;
import de.volantic.erp.catalog.domain.model.Product;
import de.volantic.erp.catalog.domain.model.ProductId;
import de.volantic.erp.core.measure.Money;
import de.volantic.erp.core.web.ETags;
import de.volantic.erp.core.web.PageResponse;
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
import java.util.Currency;
import java.util.UUID;

/**
 * REST v1 endpoints for products. Thin adapter: it maps DTOs and delegates to {@link ProductService},
 * which enforces authorization. Versioned under {@code /v1} (additive-only policy).
 */
@RestController
@RequestMapping("/v1/catalog/products")
class ProductController {

    private final ProductService products;

    ProductController(ProductService products) {
        this.products = products;
    }

    @PostMapping
    ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        Money price = Money.of(request.priceAmount(), Currency.getInstance(request.priceCurrency()));
        Product created = products.createProduct(request.sku(), request.name(), price);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id().value()).toUri();
        ProductResponse body = ProductResponse.from(created);
        return ResponseEntity.created(location).eTag(ETags.format(body.version())).body(body);
    }

    @GetMapping("/{id}")
    ResponseEntity<ProductResponse> getById(@PathVariable UUID id) {
        return withETag(ProductResponse.from(products.getProduct(new ProductId(id))));
    }

    @GetMapping
    PageResponse<ProductResponse> list(@PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.of(products.listProducts(pageable), ProductResponse::from);
    }

    @PutMapping("/{id}")
    ResponseEntity<ProductResponse> update(@PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody UpdateProductRequest request) {
        Money price = Money.of(request.priceAmount(), Currency.getInstance(request.priceCurrency()));
        Product updated = products.updateProduct(new ProductId(id), ETags.parseIfMatch(ifMatch), request.name(), price);
        return withETag(ProductResponse.from(updated));
    }

    private static ResponseEntity<ProductResponse> withETag(ProductResponse body) {
        return ResponseEntity.ok().eTag(ETags.format(body.version())).body(body);
    }
}
