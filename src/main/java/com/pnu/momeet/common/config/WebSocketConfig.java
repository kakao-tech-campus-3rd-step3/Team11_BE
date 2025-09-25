package com.pnu.momeet.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
//                .setHandshakeHandler(new CustomHandshakeHandler())
                // TODO: 배포 시 도메인 수정
                .setAllowedOriginPatterns("*") // 모든 도메인 허용(개발용)
                .withSockJS()
                .setHeartbeatTime(25000); // 25초마다 heartbeat
    }

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
}
