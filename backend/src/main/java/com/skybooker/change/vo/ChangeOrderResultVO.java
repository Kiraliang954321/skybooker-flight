package com.skybooker.change.vo;

import com.skybooker.order.vo.OrderVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeOrderResultVO {
    private Long id;
    private String orderNo;
    private String status;
    private Long flightId;
    private BigDecimal totalAmount;
    private List<OrderVO.OrderPassengerVO> passengers;
}
