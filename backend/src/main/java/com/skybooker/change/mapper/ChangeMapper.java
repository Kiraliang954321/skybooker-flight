package com.skybooker.change.mapper;

import com.skybooker.change.entity.ChangeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChangeMapper {

    void insert(ChangeRecord record);

    List<ChangeRecord> findByOrderId(@Param("orderId") Long orderId);

    int countByOrderId(@Param("orderId") Long orderId);
}
