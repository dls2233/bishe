package com.campus.security.service.impl;

import com.campus.security.common.result.Result;
import com.campus.security.entity.ExchangeRecord;
import com.campus.security.entity.Goods;
import com.campus.security.mapper.ExchangeRecordMapper;
import com.campus.security.mapper.GoodsMapper;
import com.campus.security.mapper.UserPointsMapper;
import com.campus.security.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private UserPointsMapper userPointsMapper;

    @Autowired
    private ExchangeRecordMapper exchangeRecordMapper;

    @Override
    public List<Goods> getAvailableGoods() {
        return goodsMapper.findAllAvailable();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> exchange(Long userId, Long goodsId) {
        Goods goods = goodsMapper.findById(goodsId);
        if (goods == null || goods.getStock() <= 0) {
            return Result.error(400, "商品不存在或库存不足");
        }

        // 扣减用户积分
        int pointsUpdated = userPointsMapper.decreasePoints(userId, goods.getPointsRequired());
        if (pointsUpdated == 0) {
            return Result.error(400, "用户积分不足");
        }

        // 扣减库存
        int stockUpdated = goodsMapper.decreaseStock(goodsId);
        if (stockUpdated == 0) {
            throw new RuntimeException("库存扣减失败"); // 抛出异常触发事务回滚
        }

        // 记录兑换记录
        ExchangeRecord record = new ExchangeRecord();
        record.setUserId(userId);
        record.setGoodsId(goodsId);
        record.setPointsCost(goods.getPointsRequired());
        record.setStatus("COMPLETED");
        exchangeRecordMapper.insert(record);

        return Result.success("兑换成功");
    }
}
