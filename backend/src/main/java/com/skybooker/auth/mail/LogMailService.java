package com.skybooker.auth.mail;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogMailService implements MailService {

    @Override
    public void sendVerificationCode(String toEmail, String code, String scene) {
        log.info("[MailService] 验证码已发送(日志模式): email={}, scene={}", toEmail, scene);
        log.debug("[MailService] 验证码明文(仅 debug 级,生产默认关闭): code={}", code);
    }
}
