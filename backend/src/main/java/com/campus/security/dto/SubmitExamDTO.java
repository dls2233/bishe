package com.campus.security.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Data
public class SubmitExamDTO {
    @NotNull(message = "试卷ID不能为空")
    private Long examId;
    
    // key: questionId, value: user's answer (like "A" or "A,B,C")
    private Map<Long, String> answers;
}
