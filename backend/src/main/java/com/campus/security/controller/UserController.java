package com.campus.security.controller;

import com.campus.security.common.result.Result;
import com.campus.security.common.utils.JwtUtils;
import com.campus.security.dto.UserLoginDTO;
import com.campus.security.entity.User;
import com.campus.security.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping("/info")
    public Result<User> getInfo(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtUtils.parseToken(token);
        Long userId = claims.get("userId", Long.class);
        
        return userService.getUserInfo(userId);
    }

    @PostMapping("/login")
    public Result<String> login(@Validated @RequestBody UserLoginDTO loginDTO) {
        return userService.login(loginDTO);
    }

    @PostMapping("/register")
    public Result<String> register(@Validated @RequestBody UserLoginDTO registerDTO) {
        return userService.register(registerDTO);
    }

    @PostMapping("/sendCode")
    public Result<String> sendCode(@RequestBody java.util.Map<String, String> body) {
        String email = body.get("email");
        return userService.sendCode(email);
    }

    @PostMapping("/update")
    public Result<String> updateInfo(@RequestBody User user, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtUtils.parseToken(token);
        Long userId = claims.get("userId", Long.class);
        user.setId(userId);
        return userService.updateUserInfo(user);
    }

    @PostMapping("/updatePassword")
    public Result<String> updatePassword(@RequestBody java.util.Map<String, String> body, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtUtils.parseToken(token);
        Long userId = claims.get("userId", Long.class);
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        return userService.updatePassword(userId, oldPassword, newPassword);
    }

    @PostMapping("/updateAvatar")
    public Result<String> updateAvatar(@RequestBody java.util.Map<String, String> body, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtUtils.parseToken(token);
        Long userId = claims.get("userId", Long.class);
        String avatarUrl = body.get("avatarUrl");
        return userService.updateAvatar(userId, avatarUrl);
    }
}
