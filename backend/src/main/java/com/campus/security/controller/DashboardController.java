package com.campus.security.controller;

import com.campus.security.common.result.Result;
import com.campus.security.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtUtils.parseToken(token);
        Long userId = claims.get("userId", Long.class);

        // 1. 获取已完成课程数量 (状态为 FINISHED 或者 FAILED 都算已学习/完成)
        String courseSql = "SELECT COUNT(*) FROM sys_course_progress WHERE user_id = ? AND status IN ('FINISHED', 'FAILED')";
        Integer completedCourses = jdbcTemplate.queryForObject(courseSql, Integer.class, userId);

        // 2. 获取测评通过次数
        String examSql = "SELECT COUNT(*) FROM sys_exam_record WHERE user_id = ? AND is_pass = 1";
        Integer passedExams = jdbcTemplate.queryForObject(examSql, Integer.class, userId);

        // 3. 获取当前用户积分
        String pointsSql = "SELECT points FROM sys_user WHERE id = ?";
        Integer points = jdbcTemplate.queryForObject(pointsSql, Integer.class, userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("completedCourses", completedCourses != null ? completedCourses : 0);
        stats.put("passedExams", passedExams != null ? passedExams : 0);
        stats.put("points", points != null ? points : 0);

        return Result.success(stats);
    }
}
