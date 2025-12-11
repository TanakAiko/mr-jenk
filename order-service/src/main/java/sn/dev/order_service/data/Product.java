package sn.dev.order_service.data;

import java.util.List;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Product {
    private String id;
    private String name;
    private String description;
    private Double price;
    private Integer quantity;
    private String userId;
    private List<Media> images;
}
