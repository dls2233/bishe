package com.campus.security.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class ExchangeDTO {
    @NotNull(message = "商品ID不能为空")
    private Long goodsId;
}
