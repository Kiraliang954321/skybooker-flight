package com.skybooker.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * /refresh 与 /logout 共用的请求体。admin 端复用同一 DTO。
 */
@Data
public class RefreshRequest {

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
