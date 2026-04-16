package com.campus.security.controller;

import com.campus.security.common.result.Result;
import com.campus.security.dto.ExamDetailDTO;
import com.campus.security.dto.SubmitExamDTO;
import com.campus.security.entity.Exam;
import com.campus.security.service.ExamService;
import com.campus.security.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exam")
public class ExamController {

    @Autowired
    private ExamService examService;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping("/list")
    public Result<List<Exam>> getList(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtUtils.parseToken(token);
        Long userId = claims.get("userId", Long.class);
        return Result.success(examService.getExamList(userId));
    }

    @GetMapping("/{id}")
    public Result<ExamDetailDTO> getDetail(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtUtils.parseToken(token);
        Long userId = claims.get("userId", Long.class);
        return examService.getExamDetail(id, userId);
    }

    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@Validated @RequestBody SubmitExamDTO submitDTO, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtUtils.parseToken(token);
        Long userId = claims.get("userId", Long.class);
        return examService.submitExam(userId, submitDTO);
    }

    @PostMapping("/publish")
    public Result<String> publish(@RequestBody com.campus.security.dto.PublishExamDTO publishDTO, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtUtils.parseToken(token);
        String role = claims.get("role", String.class);
        if (!"TEACHER".equals(role) && !"ADMIN".equals(role)) {
            return Result.error(403, "无权限发布测评");
        }
        return examService.publishExam(publishDTO);
    }

    @GetMapping("/attempts/{examId}")
    public Result<Integer> getAttempts(@PathVariable Long examId, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtUtils.parseToken(token);
        Long userId = claims.get("userId", Long.class);
        return examService.getAttempts(examId, userId);
    }
}
