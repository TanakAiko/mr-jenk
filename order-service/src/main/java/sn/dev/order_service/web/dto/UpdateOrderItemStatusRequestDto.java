package sn.dev.order_service.web.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateOrderItemStatusRequestDto {

    @NotNull
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
