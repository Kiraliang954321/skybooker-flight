package com.skybooker.change.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ChangeRecord {
    private Long id;
    private Long orderId;
    private Long oldFlightId;
    private Long newFlightId;
    private Long oldSeatId;
    private Long newSeatId;
    private BigDecimal priceDiff;
    private BigDecimal changeFee;
    private String status;
    private LocalDateTime createdAt;
}
