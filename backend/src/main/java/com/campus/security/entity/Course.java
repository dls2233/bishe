package com.campus.security.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Course {
    private Long id;
    private String title;
    private String category;
    private String coverUrl;
    private String videoUrl;
    private String content;
    private String quiz;
    private Integer rewardPoints;
    private Date createTime;

    // 非数据库字段
    private Boolean isLearned;
    private String progressStatus; // LEARNING, COMPLETED, FAILED
    private String lastAnswers; // 最后一次测验的答案 (JSON)
}
