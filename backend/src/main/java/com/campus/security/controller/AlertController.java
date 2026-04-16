package com.campus.security.controller;

import com.campus.security.common.result.Result;
import com.campus.security.common.utils.JwtUtils;
import com.campus.security.common.utils.SseSessionManager;
import com.campus.security.entity.Alert;
import com.campus.security.mapper.AlertMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alert")
public class AlertController {

    @Autowired
    private AlertMapper alertMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 发布预警（仅用于演示，实际项目中需要做管理员权限校验）
     */
    @PostMapping("/publish")
    public Result<String> publish(@RequestBody Alert alert, @RequestParam(required = false) Long targetUserId) {
        // 1. 将预警消息存入数据库
        alertMapper.insert(alert);

        // 2. 通过 SSE 推送给客户端
        try {
            String jsonMessage = objectMapper.writeValueAsString(alert);
            if (targetUserId != null) {
                // 如果指定了用户ID，则只推给该用户
                SseSessionManager.sendToUser(targetUserId, jsonMessage);
            } else {
                // 否则广播给所有人
                SseSessionManager.broadcast(jsonMessage);
            }
        } catch (Exception e) {
            return Result.error(500, "预警广播失败");
        }

        return Result.success("预警发布成功并已推送到在线客户端");
    }

    /**
     * 模拟给当前用户单发测试预警
     */
    @PostMapping("/test-single")
    public Result<String> testSingleAlert(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtUtils.parseToken(token);
        Long userId = claims.get("userId", Long.class);

        Alert alert = new Alert();
        alert.setTitle("个人专属测试预警");
        alert.setContent("这是一条只发给你（用户ID: " + userId + "）的测试预警信息，其他用户收不到。");
        alert.setLevel("WARNING");

        // 不入库，直接推送测试
        try {
            String jsonMessage = objectMapper.writeValueAsString(alert);
            SseSessionManager.sendToUser(userId, jsonMessage);
        } catch (Exception e) {
            return Result.error(500, "推送失败");
        }

        return Result.success("推送成功");
    }
}
