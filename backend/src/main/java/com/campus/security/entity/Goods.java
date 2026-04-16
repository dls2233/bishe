package com.campus.security.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Goods {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Integer pointsRequired;
    private Integer stock;
    private Date createTime;
}
