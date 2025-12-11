package sn.dev.order_service.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Media {
    private String id;
    private String imageUrl;
    private String productId;
}
