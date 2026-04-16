package com.campus.security.controller;

import com.campus.security.common.result.Result;
import com.campus.security.dto.ExchangeDTO;
import com.campus.security.entity.Goods;
import com.campus.security.service.GoodsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.campus.security.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;

import java.util.List;

@RestController
@RequestMapping("/api/mall")
public class MallController {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping("/goods")
    public Result<List<Goods>> getGoods() {
        return Result.success(goodsService.getAvailableGoods());
    }

    @PostMapping("/exchange")
    public Result<String> exchange(@Validated @RequestBody ExchangeDTO dto, HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        Claims claims = jwtUtils.parseToken(token);
        Long userId = claims.get("userId", Long.class);
        
        return goodsService.exchange(userId, dto.getGoodsId());
    }
}
