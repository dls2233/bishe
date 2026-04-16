package com.campus.security.mapper;

import com.campus.security.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuestionMapper {
    @Select("SELECT * FROM sys_question WHERE exam_id = #{examId}")
    List<Question> findByExamId(Long examId);

    @org.apache.ibatis.annotations.Insert("INSERT INTO sys_question(exam_id, content, type, options, answer, score) " +
            "VALUES(#{examId}, #{content}, #{type}, #{options}, #{answer}, #{score})")
    int insert(Question question);
}
