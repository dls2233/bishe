package com.campus.security.mapper;

import com.campus.security.entity.Alert;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AlertMapper {
    @Insert("INSERT INTO sys_alert(title, content, level, status) VALUES(#{title}, #{content}, #{level}, 'ACTIVE')")
    int insert(Alert alert);

    @Select("SELECT * FROM sys_alert ORDER BY create_time DESC")
    List<Alert> findAll();
}
