package com.campus.security.entity;

import lombok.Data;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class Exam {
    private Long id;
    private String title;
    private String description;
    private Integer totalScore;
    private Integer passScore;
    private Boolean isMandatory;
    private Integer timeLimit;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deadline;
    private Date createTime;

    // 非数据库字段
    private Boolean isCompleted;
    private Integer highestScore;
    private Integer attempts; // 用户已答题次数
}
