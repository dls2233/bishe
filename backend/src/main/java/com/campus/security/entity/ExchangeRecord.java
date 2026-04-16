package com.campus.security.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ExchangeRecord {
    private Long id;
    private Long userId;
    private Long goodsId;
    private Integer pointsCost;
    private String status;
    private Date createTime;
}
