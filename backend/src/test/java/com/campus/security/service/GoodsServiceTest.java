package com.campus.security.service;

import com.campus.security.common.result.Result;
import com.campus.security.entity.Goods;
import com.campus.security.mapper.ExchangeRecordMapper;
import com.campus.security.mapper.GoodsMapper;
import com.campus.security.mapper.UserPointsMapper;
import com.campus.security.service.impl.GoodsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GoodsServiceTest {

    @Mock
    private GoodsMapper goodsMapper;

    @Mock
    private UserPointsMapper userPointsMapper;

    @Mock
    private ExchangeRecordMapper exchangeRecordMapper;

    @InjectMocks
    private GoodsServiceImpl goodsService;

    @Test
    public void testExchangeSuccess() {
        // Mock数据
        Long userId = 1L;
        Long goodsId = 1L;
        Goods mockGoods = new Goods();
        mockGoods.setId(goodsId);
        mockGoods.setStock(10);
        mockGoods.setPointsRequired(100);

        // 设置Mock行为
        when(goodsMapper.findById(goodsId)).thenReturn(mockGoods);
        when(userPointsMapper.decreasePoints(userId, 100)).thenReturn(1); // 扣积分成功
        when(goodsMapper.decreaseStock(goodsId)).thenReturn(1); // 扣库存成功
        when(exchangeRecordMapper.insert(any())).thenReturn(1); // 插入记录成功

        // 执行测试
        Result<String> result = goodsService.exchange(userId, goodsId);

        // 验证结果
        assertEquals(200, result.getCode());
        assertEquals("兑换成功", result.getData());
        
        // 验证方法调用次数
        verify(goodsMapper, times(1)).findById(goodsId);
        verify(userPointsMapper, times(1)).decreasePoints(userId, 100);
        verify(goodsMapper, times(1)).decreaseStock(goodsId);
        verify(exchangeRecordMapper, times(1)).insert(any());
    }

    @Test
    public void testExchangeFail_NoStock() {
        Long userId = 1L;
        Long goodsId = 1L;
        Goods mockGoods = new Goods();
        mockGoods.setId(goodsId);
        mockGoods.setStock(0); // 库存为0

        when(goodsMapper.findById(goodsId)).thenReturn(mockGoods);

        Result<String> result = goodsService.exchange(userId, goodsId);

        assertEquals(400, result.getCode());
        assertEquals("商品不存在或库存不足", result.getMessage());
        
        // 验证后续扣减操作没有被执行
        verify(userPointsMapper, never()).decreasePoints(anyLong(), anyInt());
        verify(goodsMapper, never()).decreaseStock(anyLong());
    }

    @Test
    public void testExchangeFail_NotEnoughPoints() {
        Long userId = 1L;
        Long goodsId = 1L;
        Goods mockGoods = new Goods();
        mockGoods.setId(goodsId);
        mockGoods.setStock(10);
        mockGoods.setPointsRequired(100);

        when(goodsMapper.findById(goodsId)).thenReturn(mockGoods);
        when(userPointsMapper.decreasePoints(userId, 100)).thenReturn(0); // 扣积分失败，返回0影响行数

        Result<String> result = goodsService.exchange(userId, goodsId);

        assertEquals(400, result.getCode());
        assertEquals("用户积分不足", result.getMessage());
        
        // 验证积分扣减失败后，库存没有被扣减
        verify(goodsMapper, never()).decreaseStock(anyLong());
    }
}
