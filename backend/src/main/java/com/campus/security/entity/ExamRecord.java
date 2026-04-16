package com.campus.security.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ExamRecord {
    private Long id;
    private Long userId;
    private Long examId;
    private Integer score;
    private Boolean isPass;
    private Date createTime;
}
