package com.campus.security.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserPointsMapper {
    @Update("UPDATE sys_user SET points = points - #{points} WHERE id = #{userId} AND points >= #{points}")
    int decreasePoints(@Param("userId") Long userId, @Param("points") Integer points);

    @Update("UPDATE sys_user SET points = points + #{points} WHERE id = #{userId}")
    int increasePoints(@Param("userId") Long userId, @Param("points") Integer points);
}
