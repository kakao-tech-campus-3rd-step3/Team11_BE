package com.pnu.momeet.common.config;

import com.pnu.momeet.common.security.details.CustomUserDetailService;
import com.pnu.momeet.common.security.filter.JwtAuthenticationFilter;
import com.pnu.momeet.common.security.handler.CustomAccessDeniedHandler;
import com.pnu.momeet.common.security.handler.CustomAuthenticationEntryPoint;
import com.pnu.momeet.common.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final CustomUserDetailService userDetailService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final JwtTokenProvider tokenProvider;

    private final static String[] WHITE_LIST_PATTERNS = {
            "/api/auth/login",
            "/api/auth/signup",
            "/api/auth/logout",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private final static String REFRESH_PATH = "/api/auth/refresh";


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(
                tokenProvider,
                userDetailService,
                authenticationEntryPoint,
                WHITE_LIST_PATTERNS,
                REFRESH_PATH
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // 기본 페이지 로그인 폼 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                // 세션을 사용하지 않기 때문에 STATELESS로 설정(JWT 사용)
                .sessionManagement(sessionFactory ->
                        sessionFactory.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // CSRF 공격에 대한 방어 비활성화 (JWT 사용 시 필요 없음)
                .csrf(CsrfConfigurer<HttpSecurity>::disable)

                // iframe 내에서 사이트가 로드되는 것을 방지 (clickjacking 공격 방지)
                .headers(headers ->
                    headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

                // 요청에 대한 권한 설정(화이트리스트 이외의 요청은 인증 필요)
                .authorizeHttpRequests(authorize ->
                    authorize.requestMatchers(WHITE_LIST_PATTERNS).permitAll()
                        .anyRequest()
                        .authenticated()
                )
                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 전에 추가
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)

                // 예외 처리 설정
                .exceptionHandling(handling ->
                    handling.authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .build();
    }

}
