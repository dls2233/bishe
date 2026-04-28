package com.campus.security.mapper;

import com.campus.security.entity.News;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NewsMapper {
    @Select("SELECT * FROM sys_news ORDER BY create_time DESC")
    List<News> findAll();

    @Select("SELECT * FROM sys_news WHERE id = #{id}")
    News findById(Long id);

    @Update("UPDATE sys_news SET views = views + 1 WHERE id = #{id}")
    void incrementViews(Long id);

    @Select("SELECT * FROM sys_news WHERE title = #{title} LIMIT 1")
    News findByTitle(String title);

    @org.apache.ibatis.annotations.Insert("INSERT INTO sys_news(title, category, content, cover_url, views) " +
            "VALUES(#{title}, #{category}, #{content}, #{coverUrl}, #{views})")
    int insert(News news);

    @Select("SELECT * FROM sys_news WHERE create_time >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) ORDER BY create_time DESC")
    List<News> findRecentNews(int hours);
}
