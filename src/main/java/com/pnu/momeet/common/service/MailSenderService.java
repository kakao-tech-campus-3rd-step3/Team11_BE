package com.pnu.momeet.common.service;

import com.pnu.momeet.common.exception.MailSendFailureException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailParseException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailSenderService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.from-domain}")
    private String fromDomain;

    public void sendHtmlMail(String to, String title, String htmlContent) {
        sendHtmlMail("no-reply", to, title, htmlContent);
    }

    public void sendHtmlMail(String from, String to, String title, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from + "@" + fromDomain);
            helper.setTo(to);
            helper.setSubject(title);
            helper.setText(htmlContent, true); // true: HTML
            mailSender.send(message);
        } catch (MailParseException e) {
            log.info("메일 전송 실패: 잘못된 메일 포맷 - {} to {}", title, to, e);
            throw new IllegalArgumentException("잘못된 메일 포맷입니다.: " + e.getMessage());
        } catch (MessagingException e) {
            log.error("메일 전송 실패: {} to {}", title, to, e);
            throw new MailSendFailureException("메일 전송에 실패했습니다.: " + e.getMessage());
        }
    }
}
