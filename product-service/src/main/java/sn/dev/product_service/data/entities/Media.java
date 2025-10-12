package sn.dev.product_service.data.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Media {
    private String id;
    private String imageUrl;
    private String productId;
}
