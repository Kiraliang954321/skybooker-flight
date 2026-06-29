package com.skybooker.admin.controller;

import com.skybooker.admin.dto.AdminLoginDTO;
import com.skybooker.admin.service.AdminAuthService;
import com.skybooker.admin.vo.AdminLoginVO;
import com.skybooker.admin.vo.AdminVO;
import com.skybooker.auth.dto.RefreshRequest;
import com.skybooker.common.response.ApiResponse;
import com.skybooker.common.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/auth/login")
    public ApiResponse<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto,
                                           HttpServletRequest request) {
        return ApiResponse.success(adminAuthService.adminLogin(dto, ClientIpResolver.resolve(request)));
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<AdminLoginVO> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(adminAuthService.refreshAdminAccessToken(request.getRefreshToken()));
    }

    @GetMapping("/me")
    public ApiResponse<AdminVO> me() {
        return ApiResponse.success(adminAuthService.getCurrentAdmin());
    }

    /**
     * 作废当前 refresh token。请求体可选：即使 access 已过期也能调用。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) RefreshRequest request) {
        String refreshToken = request == null ? null : request.getRefreshToken();
        adminAuthService.logout(refreshToken);
        return ApiResponse.success();
    }
}
