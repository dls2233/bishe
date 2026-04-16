package com.campus.security.dto;

import com.campus.security.entity.Exam;
import com.campus.security.entity.Question;
import lombok.Data;
import java.util.List;

@Data
public class ExamDetailDTO {
    private Exam exam;
    private List<Question> questions;
}
