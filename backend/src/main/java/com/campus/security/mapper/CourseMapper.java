package com.campus.security.mapper;

import com.campus.security.entity.Course;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CourseMapper {

    @Select("SELECT c.*, " +
            "CASE WHEN cp.id IS NOT NULL THEN true ELSE false END AS is_learned, " +
            "cp.status AS progress_status " +
            "FROM sys_course c " +
            "LEFT JOIN sys_course_progress cp ON c.id = cp.course_id AND cp.user_id = #{userId} " +
            "ORDER BY c.create_time DESC")
    List<Course> findAllWithProgress(Long userId);

    @Select("SELECT * FROM sys_course WHERE id = #{id}")
    Course findById(Long id);

    @Select("SELECT status, last_answers FROM sys_course_progress WHERE user_id = #{userId} AND course_id = #{courseId} LIMIT 1")
    java.util.Map<String, Object> getProgress(Long userId, Long courseId);

    @Insert("INSERT INTO sys_course_progress (user_id, course_id, status, last_answers) VALUES (#{userId}, #{courseId}, #{status}, #{lastAnswers}) " +
            "ON DUPLICATE KEY UPDATE status = #{status}, last_answers = #{lastAnswers}")
    void insertProgress(Long userId, Long courseId, String status, String lastAnswers);

    @Insert("INSERT INTO sys_course(title, category, cover_url, video_url, content, reward_points, quiz) " +
            "VALUES(#{title}, #{category}, #{coverUrl}, #{videoUrl}, #{content}, #{rewardPoints}, #{quiz})")
    int insert(Course course);

    @Update("UPDATE sys_course SET cover_url = #{coverUrl} WHERE id = #{id}")
    void updateCover(Long id, String coverUrl);
}
