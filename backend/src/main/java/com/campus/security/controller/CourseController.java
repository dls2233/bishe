package com.campus.security.controller;

import com.campus.security.common.result.Result;
import com.campus.security.common.utils.JwtUtils;
import com.campus.security.entity.Course;
import com.campus.security.service.CourseService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/course")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping("/list")
    public Result<List<Course>> list(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtUtils.parseToken(token);
        Long userId = claims.get("userId", Long.class);
        return Result.success(courseService.getCourseList(userId));
    }

    @GetMapping("/{id}")
    public Result<Course> detail(@PathVariable Long id, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtUtils.parseToken(token);
        Long userId = claims.get("userId", Long.class);
        return courseService.getCourseDetail(id, userId);
    }

    @PostMapping("/{id}/finish")
    public Result<String> finish(@PathVariable Long id, @RequestBody Map<String, Object> params, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtUtils.parseToken(token);
        Long userId = claims.get("userId", Long.class);

        Boolean passed = (Boolean) params.get("passed");
        if (passed == null) passed = true;
        
        String lastAnswers = null;
        if (params.get("lastAnswers") != null) {
            lastAnswers = params.get("lastAnswers").toString();
        }

        return courseService.finishCourse(userId, id, passed, lastAnswers);
    }

    @PostMapping("/publish")
    public Result<String> publish(@RequestBody Course course, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtUtils.parseToken(token);
        String role = claims.get("role", String.class);
        if (!"TEACHER".equals(role) && !"ADMIN".equals(role)) {
            return Result.error(403, "无权限发布课程");
        }
        return courseService.publishCourse(course);
    }
}
