package com.campus.security.service.impl;

import com.campus.security.common.result.Result;
import com.campus.security.dto.ExamDetailDTO;
import com.campus.security.dto.SubmitExamDTO;
import com.campus.security.entity.Exam;
import com.campus.security.entity.ExamRecord;
import com.campus.security.entity.Question;
import com.campus.security.mapper.ExamMapper;
import com.campus.security.mapper.ExamRecordMapper;
import com.campus.security.mapper.QuestionMapper;
import com.campus.security.mapper.UserPointsMapper;
import com.campus.security.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExamServiceImpl implements ExamService {

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private UserPointsMapper userPointsMapper;

    @Override
    public List<Exam> getExamList(Long userId) {
        return examMapper.findAllWithProgress(userId);
    }

    @Override
    public Result<ExamDetailDTO> getExamDetail(Long id, Long userId) {
        Exam exam = examMapper.findByIdWithProgress(id, userId);
        if (exam == null) {
            return Result.error(404, "试卷不存在");
        }
        
        List<Question> questions = questionMapper.findByExamId(id);
        
        ExamDetailDTO detail = new ExamDetailDTO();
        detail.setExam(exam);
        detail.setQuestions(questions);
        return Result.success(detail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> submitExam(Long userId, SubmitExamDTO submitDTO) {
        Exam exam = examMapper.findById(submitDTO.getExamId());
        if (exam == null) {
            return Result.error(404, "试卷不存在");
        }

        // 校验 DDL
        if (exam.getDeadline() != null && new Date().after(exam.getDeadline())) {
            return Result.error(403, "抱歉，该测评已过截止时间，系统已自动禁止提交");
        }

        List<Question> questions = questionMapper.findByExamId(exam.getId());
        int totalScore = 0;
        
        // 判分逻辑
        Map<Long, String> userAnswers = submitDTO.getAnswers();
        if (userAnswers != null) {
            for (Question q : questions) {
                String userAnswer = userAnswers.get(q.getId());
                if (userAnswer != null && userAnswer.equals(q.getAnswer())) {
                    totalScore += q.getScore();
                }
            }
        }

        boolean isPass = totalScore >= exam.getPassScore();

        // 保存考试记录
        ExamRecord record = new ExamRecord();
        record.setUserId(userId);
        record.setExamId(exam.getId());
        record.setScore(totalScore);
        record.setIsPass(isPass);
        examRecordMapper.insert(record);

        // 如果及格且是第一次及格（这里简化为：只要及格就加50积分奖励）
        int rewardPoints = 0;
        if (isPass) {
            rewardPoints = 50; // 固定奖励50分
            userPointsMapper.increasePoints(userId, rewardPoints);
        }

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("score", totalScore);
        resultData.put("isPass", isPass);
        resultData.put("rewardPoints", rewardPoints);

        return Result.success(resultData);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> publishExam(com.campus.security.dto.PublishExamDTO publishDTO) {
        Exam exam = publishDTO.getExam();
        if (exam.getTitle() == null || exam.getTitle().isEmpty()) {
            return Result.error(400, "测评标题不能为空");
        }
        examMapper.insert(exam);
        
        List<Question> questions = publishDTO.getQuestions();
        if (questions != null && !questions.isEmpty()) {
            for (Question q : questions) {
                q.setExamId(exam.getId());
                questionMapper.insert(q);
            }
        }
        
        return Result.success("测评发布成功");
    }

    @Override
    public Result<Integer> getAttempts(Long examId, Long userId) {
        int attempts = examRecordMapper.countUserAttempts(userId, examId);
        return Result.success(attempts);
    }
}

