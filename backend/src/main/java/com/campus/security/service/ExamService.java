package com.campus.security.service;

import com.campus.security.common.result.Result;
import com.campus.security.dto.ExamDetailDTO;
import com.campus.security.dto.SubmitExamDTO;
import com.campus.security.entity.Exam;

import java.util.List;
import java.util.Map;

public interface ExamService {
    List<Exam> getExamList(Long userId);
    Result<ExamDetailDTO> getExamDetail(Long id, Long userId);
    Result<Map<String, Object>> submitExam(Long userId, SubmitExamDTO submitDTO);
    Result<String> publishExam(com.campus.security.dto.PublishExamDTO publishDTO);

    Result<Integer> getAttempts(Long examId, Long userId);
}
