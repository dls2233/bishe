package com.campus.security.service;

import com.campus.security.common.result.Result;
import com.campus.security.entity.Course;

import java.util.List;

public interface CourseService {
    List<Course> getCourseList(Long userId);
    Result<Course> getCourseDetail(Long id, Long userId);
    Result<String> finishCourse(Long userId, Long courseId, Boolean passed, String lastAnswers);
    Result<String> publishCourse(Course course);
}
