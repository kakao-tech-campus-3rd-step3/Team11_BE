package com.pnu.momeet.domain.auth.service.listener;

import com.pnu.momeet.common.security.config.SecurityProperties;
import com.pnu.momeet.common.service.MailSenderService;
import com.pnu.momeet.domain.auth.event.SendVerificationEmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationEventListener {
    private final MailSenderService mailSenderService;
    private final SecurityProperties securityProperties;
    private final TemplateEngine templateEngine;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendVerificationEmail(SendVerificationEmailEvent event) {
        String email = event.getEmail();
        String code = event.getCode();
        log.info("인증이메일 전송 이벤트 시작 : code = {}", code);
        String verificationUrl = securityProperties.getHttps().getUrl() + "/auth/verify?code=" + code;
        Context context = new Context();
        context.setVariable("title", "MoMeet 회원가입 인증");
        context.setVariable("verificationUrl", verificationUrl);
        String htmlContent = templateEngine.process("mail/email-verification.html", context);
        mailSenderService.sendHtmlMail(
                email,
                "MoMeet 회원가입 인증 메일입니다.",
                htmlContent
        );
        log.info("인증이메일 전송 이벤트 완료 : code = {}", code);
    }
}
