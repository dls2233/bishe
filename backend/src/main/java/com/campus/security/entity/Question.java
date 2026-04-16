package com.campus.security.entity;

import lombok.Data;

@Data
public class Question {
    private Long id;
    private Long examId;
    private String content;
    private String type; // SINGLE_CHOICE, MULTIPLE_CHOICE, JUDGE
    private String options; // JSON array string
    private String answer;
    private Integer score;
}
