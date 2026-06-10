package com.skybooker.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ChangeOrderDTO {

    @NotNull(message = "新航班ID不能为空")
    private Long newFlightId;

    @NotEmpty(message = "座位映射不能为空")
    @Valid
    private List<SeatMapping> seatMappings;

    @Data
    public static class SeatMapping {
        @NotNull(message = "乘机人ID不能为空")
        private Long passengerId;

        @NotNull(message = "新座位ID不能为空")
        private Long newSeatId;
    }
}
