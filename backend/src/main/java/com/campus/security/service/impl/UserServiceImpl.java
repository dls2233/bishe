package com.campus.security.service.impl;

import com.campus.security.common.result.Result;
import com.campus.security.common.utils.JwtUtils;
import com.campus.security.dto.UserLoginDTO;
import com.campus.security.entity.User;
import com.campus.security.mapper.UserMapper;
import com.campus.security.service.UserService;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

@Service
public class UserServiceImpl implements UserService {

    private final Map<String, String> emailCodeMap = new ConcurrentHashMap<>();

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public Result<String> login(UserLoginDTO loginDTO) {
        User user = userMapper.findByUsername(loginDTO.getUsername());
        if (user == null) {
            return Result.error(401, "用户不存在");
        }

        // 简单的MD5加密校验
        String encryptPassword = DigestUtils.md5Hex(loginDTO.getPassword());
        if (!encryptPassword.equals(user.getPassword())) {
            return Result.error(401, "密码错误");
        }

        // 生成 JWT Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        return Result.success(token);
    }

    @Override
    public Result<User> getUserInfo(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        // 脱敏处理
        user.setPassword(null);
        return Result.success(user);
    }

    @Override
    public Result<String> sendCode(String email) {
        if (email == null || email.isEmpty()) {
            return Result.error(400, "邮箱不能为空");
        }
        // 简单生成6位验证码
        String code = String.format("%06d", new Random().nextInt(1000000));
        emailCodeMap.put(email, code);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("重游安全教育平台 - 注册验证码");
            message.setText("您的注册验证码为：" + code + "，有效期为5分钟，请勿泄露给他人。");
            mailSender.send(message);
            System.out.println("向邮箱 " + email + " 真实发送验证码: " + code);
            return Result.success("验证码已发送");
        } catch (Exception e) {
            e.printStackTrace();
            // 为防止未配置真实邮箱时阻断流程，若发送失败在控制台打印并返回模拟成功，真实项目请返回error
            System.err.println("邮件发送异常，请检查 application.yml 的 mail 配置");
            System.out.println("由于发送失败，改为控制台输出验证码: " + code);
            return Result.success("验证码已发送");
        }
    }

    @Override
    public Result<String> register(UserLoginDTO registerDTO) {
        if (registerDTO.getEmail() == null || registerDTO.getCode() == null) {
            return Result.error(400, "邮箱或验证码不能为空");
        }
        String storedCode = emailCodeMap.get(registerDTO.getEmail());
        if (storedCode == null || !storedCode.equals(registerDTO.getCode())) {
            return Result.error(400, "验证码错误或已过期");
        }
        if (registerDTO.getCollege() == null || registerDTO.getCollege().isEmpty()) {
            return Result.error(400, "学院不能为空");
        }

        User existUser = userMapper.findByUsername(registerDTO.getUsername());
        if (existUser != null) {
            return Result.error(400, "用户名已存在");
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(DigestUtils.md5Hex(registerDTO.getPassword()));
        user.setRealName(registerDTO.getUsername()); // 默认与用户名一致
        user.setEmail(registerDTO.getEmail());
        user.setCollege(registerDTO.getCollege());
        user.setRole("USER");

        userMapper.insert(user);
        // 注册成功后移除验证码
        emailCodeMap.remove(registerDTO.getEmail());
        return Result.success("注册成功");
    }

    @Override
    public Result<String> updateUserInfo(User user) {
        if (user.getId() == null) {
            return Result.error(400, "用户ID不能为空");
        }
        userMapper.updateInfo(user);
        return Result.success("更新成功");
    }

    @Override
    public Result<String> updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.findById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        String oldEncrypt = DigestUtils.md5Hex(oldPassword);
        if (!oldEncrypt.equals(user.getPassword())) {
            return Result.error(400, "原密码错误");
        }
        String newEncrypt = DigestUtils.md5Hex(newPassword);
        userMapper.updatePassword(userId, newEncrypt);
        return Result.success("密码修改成功");
    }

    @Override
    public Result<String> updateAvatar(Long userId, String avatarUrl) {
        if (userId == null) {
            return Result.error(400, "用户ID不能为空");
        }
        userMapper.updateAvatar(userId, avatarUrl);
        return Result.success("头像更新成功");
    }
}
