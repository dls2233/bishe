package com.campus.security.mapper;

import com.campus.security.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

    @Select("SELECT * FROM sys_user WHERE id = #{id}")
    User findById(@Param("id") Long id);

    @Insert("INSERT INTO sys_user(username, password, real_name, email, college, role) " +
            "VALUES(#{username}, #{password}, #{realName}, #{email}, #{college}, #{role})")
    int insert(User user);

    @org.apache.ibatis.annotations.Update("UPDATE sys_user SET real_name = #{realName}, email = #{email}, bio = #{bio}, awards = #{awards}, hobbies = #{hobbies} WHERE id = #{id}")
    int updateInfo(User user);

    @org.apache.ibatis.annotations.Update("UPDATE sys_user SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @org.apache.ibatis.annotations.Update("UPDATE sys_user SET avatar_url = #{avatarUrl} WHERE id = #{id}")
    int updateAvatar(@Param("id") Long id, @Param("avatarUrl") String avatarUrl);
}
