package com.campus.security.entity;

import lombok.Data;
import java.util.Date;

@Data
public class News {
    private Long id;
    private String title;
    private String category;
    private String content;
    private String coverUrl;
    private Integer views;
    private Date createTime;
}
