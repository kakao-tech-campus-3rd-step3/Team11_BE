package com.pnu.momeet.common.security.config;

import com.pnu.momeet.common.security.interceptor.LoggingChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.messaging.context.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

import static org.springframework.messaging.simp.SimpMessageType.MESSAGE;
import static org.springframework.messaging.simp.SimpMessageType.SUBSCRIBE;

@Configuration
@RequiredArgsConstructor
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {
    private final ApplicationContext applicationContext;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new AuthenticationPrincipalArgumentResolver());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        AuthorizationChannelInterceptor authorizationChannelInterceptor = new AuthorizationChannelInterceptor(authorizationManager());
        AuthorizationEventPublisher publisher = new SpringAuthorizationEventPublisher(applicationContext);
        authorizationChannelInterceptor.setAuthorizationEventPublisher(publisher);
        registration.interceptors(new SecurityContextChannelInterceptor(), authorizationChannelInterceptor);
    }

    @Bean
    public AuthorizationManager<Message<?>> authorizationManager(
    ) {
        var messages = MessageMatcherDelegatingAuthorizationManager.builder();
        messages
                // Connect 시 인증된 사용자만 허용
                .nullDestMatcher().authenticated()
                // 에러 메시지는 모두 허용
                .simpSubscribeDestMatchers("/user/queue/errors").permitAll()
                // 채팅방 입장 및 메시지 전송은 USER, ADMIN 권한 허용
                .simpDestMatchers("/app/**").hasAnyRole("USER", "ADMIN")
                // 구독은 USER, ADMIN 권한 허용
                .simpSubscribeDestMatchers("/user/**", "/topic/**").hasAnyRole("USER", "ADMIN")
                // 그 외의 메시지는 모두 거부
                .simpTypeMatchers(MESSAGE, SUBSCRIBE).denyAll()
                .anyMessage().denyAll();

        return messages.build();
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(new LoggingChannelInterceptor());
    }
}
