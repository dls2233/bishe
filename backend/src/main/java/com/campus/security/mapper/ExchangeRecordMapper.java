package com.campus.security.mapper;

import com.campus.security.entity.ExchangeRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExchangeRecordMapper {

    @Insert("INSERT INTO sys_exchange_record(user_id, goods_id, points_cost, status) " +
            "VALUES(#{userId}, #{goodsId}, #{pointsCost}, #{status})")
    int insert(ExchangeRecord record);
}
