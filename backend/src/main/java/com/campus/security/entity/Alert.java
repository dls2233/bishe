package com.campus.security.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Alert {
    private Long id;
    private String title;
    private String content;
    private String level; // INFO, WARNING, DANGER
    private String status;
    private Date createTime;
}
