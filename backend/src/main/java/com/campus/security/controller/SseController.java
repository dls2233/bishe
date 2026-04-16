package com.campus.security.controller;

import com.campus.security.common.result.Result;
import com.campus.security.common.utils.JwtUtils;
import com.campus.security.common.utils.SseSessionManager;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
public class SseController {

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam("token") String token) {
        Claims claims = jwtUtils.parseToken(token);
        Long userId = claims.get("userId", Long.class);

        // 设置超时时间为 1 小时 (3600000ms)，0表示永不超时（不推荐，容易资源泄露）
        SseEmitter emitter = new SseEmitter(3600000L);
        SseSessionManager.add(userId, emitter);

        // 建立连接后，向客户端发送一条确认消息，防止Nginx等代理网关因没有数据而超时断开
        try {
            emitter.send(SseEmitter.event().data("CONNECTED"));
        } catch (Exception e) {
            SseSessionManager.remove(userId);
        }

        return emitter;
    }

    /**
     * 获取当前系统在线人数
     */
    @GetMapping("/online-count")
    public Result<Integer> getOnlineCount() {
        return Result.success(SseSessionManager.getOnlineCount());
    }
}
