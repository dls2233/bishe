package com.campus.security.mapper;

import com.campus.security.entity.ExamRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExamRecordMapper {
    @Insert("INSERT INTO sys_exam_record(user_id, exam_id, score, is_pass) VALUES(#{userId}, #{examId}, #{score}, #{isPass})")
    int insert(ExamRecord record);

    @Select("SELECT * FROM sys_exam_record WHERE user_id = #{userId} AND exam_id = #{examId} ORDER BY create_time DESC")
    List<ExamRecord> findByUserAndExam(Long userId, Long examId);

    @Select("SELECT COUNT(*) FROM sys_exam_record WHERE user_id = #{userId} AND exam_id = #{examId}")
    int countUserAttempts(Long userId, Long examId);

    @Select("SELECT * FROM sys_exam_record ORDER BY create_time DESC LIMIT #{limit}")
    List<ExamRecord> findRecentRecords(int limit);
}
