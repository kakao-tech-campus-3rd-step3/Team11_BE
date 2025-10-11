package com.pnu.momeet.common.security.config;

import com.pnu.momeet.common.security.interceptor.MessageAuthenticateInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.messaging.context.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

import static org.springframework.messaging.simp.SimpMessageType.MESSAGE;
import static org.springframework.messaging.simp.SimpMessageType.SUBSCRIBE;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {
    private final SecurityProperties securityProperties;
    private final ApplicationContext applicationContext;
    private final MessageAuthenticateInterceptor messageAuthenticateInterceptor;

    @Bean
    public ThreadPoolTaskScheduler customMessageBrokerTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("wss-heartbeat-thread-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureMessageBroker(
            @NonNull MessageBrokerRegistry registry
    ) {

        // 애플리케이션 목적지 prefix
        registry.setApplicationDestinationPrefixes("/app", "/topic");

        // Simple broker 설정 - 메모리 기반
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10_000, 20_000})
                .setTaskScheduler(customMessageBrokerTaskScheduler()); // 커스텀 스케줄러 연결
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        String[] allowOrigins = securityProperties.getCors().getAllowedOrigins().toArray(new String[0]);

        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns(allowOrigins)
                .withSockJS()
                .setHeartbeatTime(25000);
    }

    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new AuthenticationPrincipalArgumentResolver());
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        AuthorizationChannelInterceptor authorizationChannelInterceptor = new AuthorizationChannelInterceptor(authorizationManager());
        AuthorizationEventPublisher publisher = new SpringAuthorizationEventPublisher(applicationContext);
        authorizationChannelInterceptor.setAuthorizationEventPublisher(publisher);

        registration.interceptors(
                messageAuthenticateInterceptor,
                new SecurityContextChannelInterceptor(),
                authorizationChannelInterceptor
        );
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
}
