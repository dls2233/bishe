package com.campus.security.mapper;

import com.campus.security.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface GoodsMapper {

    @Select("SELECT * FROM sys_goods WHERE stock > 0 ORDER BY create_time DESC")
    List<Goods> findAllAvailable();

    @Select("SELECT * FROM sys_goods WHERE id = #{id}")
    Goods findById(@Param("id") Long id);

    @Update("UPDATE sys_goods SET stock = stock - 1 WHERE id = #{id} AND stock > 0")
    int decreaseStock(@Param("id") Long id);
}
