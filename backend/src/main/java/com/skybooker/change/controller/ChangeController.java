package com.skybooker.change.controller;

import com.skybooker.change.service.ChangeService;
import com.skybooker.change.vo.ChangeOptionVO;
import com.skybooker.change.vo.ChangeOrderResultVO;
import com.skybooker.common.response.ApiResponse;
import com.skybooker.order.dto.ChangeOrderDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class ChangeController {

    private final ChangeService changeService;

    @GetMapping("/{id}/change-options")
    public ApiResponse<List<ChangeOptionVO>> listChangeOptions(@PathVariable Long id) {
        return ApiResponse.success(changeService.listChangeOptions(id));
    }

    @PostMapping("/{id}/change")
    public ApiResponse<ChangeOrderResultVO> changeOrder(@PathVariable Long id,
                                                        @Valid @RequestBody ChangeOrderDTO dto) {
        return ApiResponse.success(changeService.changeOrder(id, dto));
    }
}
