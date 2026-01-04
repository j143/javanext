package com.javanext.inventory.controller;

import com.javanext.inventory.domain.Product;
import com.javanext.inventory.dto.ProductRequest;
import com.javanext.inventory.dto.ProductResponse;
import com.javanext.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product and inventory management APIs")
public class ProductController {

    private final InventoryService inventoryService;

    public ProductController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieve list of all products with inventory details")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<Product> products = inventoryService.getAllProducts();
        List<ProductResponse> responses = products.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieve product details by product ID")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
        Product product = inventoryService.getProductById(id);
        return ResponseEntity.ok(toResponse(product));
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU", description = "Retrieve product details by SKU")
    public ResponseEntity<ProductResponse> getProductBySku(@PathVariable String sku) {
        Product product = inventoryService.getProductBySku(sku);
        return ResponseEntity.ok(toResponse(product));
    }

    @PostMapping
    @Operation(summary = "Create product", description = "Create a new product in inventory")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        Product product = inventoryService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(product));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Update existing product details")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request) {
        Product product = inventoryService.updateProduct(id, request);
        return ResponseEntity.ok(toResponse(product));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock products", description = "Retrieve products with low stock levels")
    public ResponseEntity<List<ProductResponse>> getLowStockProducts(
            @RequestParam(defaultValue = "10") int threshold) {
        List<Product> products = inventoryService.getLowStockProducts(threshold);
        List<ProductResponse> responses = products.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getQuantity(),
                product.getReservedQuantity(),
                product.getQuantity() - product.getReservedQuantity(),
                product.getPrice(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
