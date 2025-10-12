package sn.dev.product_service.web.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;
import sn.dev.product_service.web.dto.ProductCreateDTO;
import sn.dev.product_service.web.dto.ProductResponseDTO;
import sn.dev.product_service.web.dto.ProductUpdateDTO;

@RequestMapping("/api/products")
public interface ProductController {
    @PreAuthorize("hasAuthority('SELLER')")
    @PostMapping
    ResponseEntity<ProductResponseDTO> create(@ModelAttribute @Valid ProductCreateDTO productCreateDTO);

    @GetMapping
    ResponseEntity<List<ProductResponseDTO>> getAll();

    @GetMapping("/{id}")
    ResponseEntity<ProductResponseDTO> getById(@PathVariable String id);

    @PreAuthorize("hasAuthority('SELLER')")
    @PutMapping("/{id}")
    ResponseEntity<ProductResponseDTO> update(@ModelAttribute @Valid ProductUpdateDTO productUpdateDTO,
            @PathVariable String id);

    @PreAuthorize("hasAuthority('SELLER')")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable String id);
}
