package com.campus.security.service;

import com.campus.security.common.result.Result;
import com.campus.security.entity.Goods;

import java.util.List;

public interface GoodsService {
    List<Goods> getAvailableGoods();
    Result<String> exchange(Long userId, Long goodsId);
}
