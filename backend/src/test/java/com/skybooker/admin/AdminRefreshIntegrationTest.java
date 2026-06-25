package com.skybooker.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skybooker.admin.dto.AdminLoginDTO;
import com.skybooker.auth.dto.ResetPasswordDTO;
import com.skybooker.auth.service.AuthService;
import com.skybooker.auth.verification.VerificationCodeStore;
import com.skybooker.common.AbstractIntegrationTest;
import com.skybooker.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理后台 /api/admin/auth/refresh 与 /api/admin/logout：refresh 旋转 + logout 作废。
 */
class AdminRefreshIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private VerificationCodeStore codeStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SuppressWarnings("unchecked")
    private Map<String, Object> adminLogin() throws Exception {
        AdminLoginDTO dto = new AdminLoginDTO();
        dto.setUsername("admin");
        dto.setPassword("SkyBooker@Init2026!");
        MvcResult result = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();
        ApiResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ApiResponse.class);
        return (Map<String, Object>) response.getData();
    }

    @Test
    void adminRefreshIssuesNewPairAndRevokesOld() throws Exception {
        String oldRefresh = (String) adminLogin().get("refreshToken");

        MvcResult result = mockMvc.perform(post("/api/admin/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", oldRefresh))))
                .andExpect(status().isOk())
                .andReturn();
        ApiResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ApiResponse.class);
        assertEquals(200, response.getCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertNotEquals(oldRefresh, data.get("refreshToken"));

        // rotation：旧 refresh 已作废
        mockMvc.perform(post("/api/admin/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", oldRefresh))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminLogoutRevokesRefreshToken() throws Exception {
        String refreshToken = (String) adminLogin().get("refreshToken");

        mockMvc.perform(post("/api/admin/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @SuppressWarnings("unchecked")
    @Test
    void resettingAdminPasswordRevokesAdminTokensToo() throws Exception {
        Map<String, Object> login = adminLogin();
        String adminAccess = (String) login.get("accessToken");
        String adminRefresh = (String) login.get("refreshToken");

        // 先确认 token 可用
        mockMvc.perform(get("/api/admin/me").header("Authorization", "Bearer " + adminAccess))
                .andExpect(status().isOk());

        String email = "admin@skybooker.local";
        String originalHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE email = ?", String.class, email);
        try {
            // 对管理员邮箱触发 resetPassword（绕过邮件，直接存验证码）
            codeStore.storeCode(email, "RESET_PASSWORD", "654321");
            ResetPasswordDTO dto = new ResetPasswordDTO();
            dto.setEmail(email);
            dto.setCode("654321");
            dto.setNewPassword("NewAdmin@123456");
            dto.setConfirmPassword("NewAdmin@123456");
            authService.resetPassword(dto);

            // ADMIN 版本被 bump：旧 admin access 立即失效（Filter 校验 tokenVer）
            mockMvc.perform(get("/api/admin/me").header("Authorization", "Bearer " + adminAccess))
                    .andExpect(status().isUnauthorized());
            // 旧 admin refresh 也失效
            mockMvc.perform(post("/api/admin/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("refreshToken", adminRefresh))))
                    .andExpect(status().isUnauthorized());
        } finally {
            // 恢复密码 hash，避免污染依赖原管理员口令的其他测试类
            jdbcTemplate.update("UPDATE users SET password_hash = ? WHERE email = ?",
                    originalHash, email);
        }
    }
}
