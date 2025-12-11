package sn.dev.order_service.services;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import sn.dev.order_service.config.FeignSupportConfig;
import sn.dev.order_service.data.Product;

@FeignClient(name = "product-service", url = "${product.service.url}", configuration = FeignSupportConfig.class)
public interface ProductServiceClient {
    @GetMapping("/{id}")
    Product getProductById(@PathVariable String id);
}
