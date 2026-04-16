package com.campus.security.service;

import com.campus.security.dto.UserLoginDTO;
import com.campus.security.common.result.Result;
import com.campus.security.entity.User;

public interface UserService {
    Result<String> login(UserLoginDTO loginDTO);
    Result<String> register(UserLoginDTO registerDTO);
    Result<User> getUserInfo(Long userId);
    Result<String> sendCode(String email);
    Result<String> updateUserInfo(User user);
    Result<String> updatePassword(Long userId, String oldPassword, String newPassword);
    Result<String> updateAvatar(Long userId, String avatarUrl);
}