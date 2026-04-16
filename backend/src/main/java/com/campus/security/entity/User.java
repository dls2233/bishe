package com.campus.security.entity;

import lombok.Data;
import java.util.Date;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String email;
    private String college;
    private String role;
    private String avatarUrl;
    private Integer points;
    private String bio;
    private String awards;
    private String hobbies;
    private Date createTime;
    private Date updateTime;
}
