package sn.dev.product_service.web.controllers.impl;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.dev.product_service.data.entities.Media;
import sn.dev.product_service.data.entities.Product;
import sn.dev.product_service.services.MediaServiceClient;
import sn.dev.product_service.services.ProductService;
import sn.dev.product_service.web.controllers.ProductController;
import sn.dev.product_service.web.dto.ProductCreateDTO;
import sn.dev.product_service.web.dto.ProductResponseDTO;
import sn.dev.product_service.web.dto.ProductUpdateDTO;

@RestController
@RequiredArgsConstructor
public class ProductControllerImpl implements ProductController {

    private final ProductService productService;
    private final MediaServiceClient mediaServiceClient;
    private String maxAge = "300";

    @Override
    public ResponseEntity<ProductResponseDTO> create(
        @Valid ProductCreateDTO productCreateDTO
    ) {
        System.out.println("CREATE product : " + productCreateDTO);

        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) auth.getPrincipal();
        String userId = jwt.getClaimAsString("userID");

        Product product = productService.create(
            productCreateDTO.toProduct(userId)
        );
        List<Media> medias = productCreateDTO
            .getImages()
            .stream()
            .map(file -> mediaServiceClient.upload(file, product.getId()))
            .toList();

        return ResponseEntity.status(HttpStatus.CREATED)
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + maxAge)
            .body(new ProductResponseDTO(product, medias));
    }

    @Override
    public ResponseEntity<List<ProductResponseDTO>> getAll(String search) {
        System.out.println("GET(getAll) products with search: " + search);

        List<Product> products;
        if (search != null && !search.trim().isEmpty()) {
            products = productService.search(search);
        } else {
            products = productService.getAll();
        }

        List<ProductResponseDTO> responseList = products
            .stream()
            .map(product -> {
                List<Media> medias = Optional.ofNullable(
                    mediaServiceClient.getByProductId(product.getId()).getBody()
                ).orElseGet(List::of);
                return new ProductResponseDTO(product, medias);
            })
            .toList();

        return ResponseEntity.ok(responseList);
    }

    @Override
    public ResponseEntity<ProductResponseDTO> getById(String id) {
        System.out.println("GET(product by id) product with id: " + id);

        Product product = productService.getById(id);
        List<Media> medias = mediaServiceClient.getByProductId(id).getBody();

        return ResponseEntity.ok(new ProductResponseDTO(product, medias));
    }

    @Override
    public ResponseEntity<ProductResponseDTO> update(
        @Valid ProductUpdateDTO productUpdateDTO,
        String id
    ) {
        System.out.println("UPDATE(product by id) product with id: " + id);

        Product product = productService.getById(id);
        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) auth.getPrincipal();
        String userId = jwt.getClaimAsString("userID");

        if (!product.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not allowed to update this product"
            );
        }

        Product productToUpdate = productUpdateDTO.toProduct(userId);
        productToUpdate.setId(id);

        Product updatedProduct = productService.update(productToUpdate);

        // Handle new images if provided
        if (
            productUpdateDTO.getImages() != null &&
            !productUpdateDTO.getImages().isEmpty()
        ) {
            System.out.println(
                "Uploading " +
                productUpdateDTO.getImages().size() +
                " new images for product " +
                id
            );
            productUpdateDTO
                .getImages()
                .forEach(file -> {
                    System.out.println(
                        "Uploading image: " + file.getOriginalFilename()
                    );
                    mediaServiceClient.upload(file, updatedProduct.getId());
                });
        }

        // Get all medias (existing + newly uploaded)
        List<Media> medias = mediaServiceClient
            .getByProductId(updatedProduct.getId())
            .getBody();

        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + maxAge)
            .body(new ProductResponseDTO(updatedProduct, medias));
    }

    @Override
    public ResponseEntity<Void> delete(String id) {
        System.out.println("DELETE(product by id) product with id: " + id);

        Product product = productService.getById(id);
        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) auth.getPrincipal();
        String userId = jwt.getClaimAsString("userID");

        if (!product.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not allowed to delete this product"
            );
        }

        productService.delete(product);
        mediaServiceClient.deleteByProductId(id);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> reduceQuantity(String id, int quantity) {
        productService.reduceQuantity(id, quantity);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> restoreQuantity(String id, int quantity) {
        productService.restoreQuantity(id, quantity);
        return ResponseEntity.ok().build();
    }
}
