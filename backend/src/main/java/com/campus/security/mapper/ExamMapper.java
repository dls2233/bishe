package com.campus.security.mapper;

import com.campus.security.entity.Exam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExamMapper {
    @Select("SELECT e.*, " +
            "CASE WHEN SUM(CASE WHEN r.is_pass = 1 THEN 1 ELSE 0 END) > 0 OR COUNT(r.id) >= 3 THEN true ELSE false END AS is_completed, " +
            "MAX(r.score) AS highest_score, " +
            "COUNT(r.id) AS attempts " +
            "FROM sys_exam e " +
            "LEFT JOIN sys_exam_record r ON e.id = r.exam_id AND r.user_id = #{userId} " +
            "GROUP BY e.id " +
            "ORDER BY e.create_time DESC")
    List<Exam> findAllWithProgress(Long userId);

    @Select("SELECT e.*, " +
            "CASE WHEN SUM(CASE WHEN r.is_pass = 1 THEN 1 ELSE 0 END) > 0 OR COUNT(r.id) >= 3 THEN true ELSE false END AS is_completed, " +
            "MAX(r.score) AS highest_score, " +
            "COUNT(r.id) AS attempts " +
            "FROM sys_exam e " +
            "LEFT JOIN sys_exam_record r ON e.id = r.exam_id AND r.user_id = #{userId} " +
            "WHERE e.id = #{id} " +
            "GROUP BY e.id")
    Exam findByIdWithProgress(Long id, Long userId);

    @Select("SELECT * FROM sys_exam WHERE id = #{id}")
    Exam findById(Long id);

    @org.apache.ibatis.annotations.Insert("INSERT INTO sys_exam(title, description, total_score, pass_score, is_mandatory, time_limit, deadline) " +
            "VALUES(#{title}, #{description}, #{totalScore}, #{passScore}, #{isMandatory}, #{timeLimit}, #{deadline})")
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Exam exam);
}
