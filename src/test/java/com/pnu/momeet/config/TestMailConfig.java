package com.pnu.momeet.config;

import com.pnu.momeet.common.service.MailSenderService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 테스트 환경에서 실제 이메일 발송을 방지하기 위한 Mock 설정
 */
@TestConfiguration
public class TestMailConfig {

    @Bean
    @Primary
    public MailSenderService mockMailSenderService() {
        MailSenderService mockMailSender = mock(MailSenderService.class);
        
        // sendHtmlMail 메서드가 호출되어도 아무 동작도 하지 않음
        doNothing().when(mockMailSender)
            .sendHtmlMail(anyString(), anyString(), anyString());
        
        doNothing().when(mockMailSender)
            .sendHtmlMail(anyString(), anyString(), anyString(), anyString());
        
        return mockMailSender;
    }
}

