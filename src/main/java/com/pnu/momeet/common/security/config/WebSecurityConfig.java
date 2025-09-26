package com.pnu.momeet.common.security.config;

import com.pnu.momeet.common.security.details.CustomUserDetailService;
import com.pnu.momeet.common.security.filter.JwtAuthenticationFilter;
import com.pnu.momeet.common.security.filter.JwtTokenCookieHandingFilter;
import com.pnu.momeet.common.security.handler.CustomAccessDeniedHandler;
import com.pnu.momeet.common.security.handler.CustomAuthenticationEntryPoint;
import com.pnu.momeet.common.security.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final CustomUserDetailService userDetailService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final JwtTokenProvider tokenProvider;
    private final JwtTokenCookieHandingFilter jwtTokenCookieHandingFilter;
    @Value("${jwt.access-token.name_in_cookie}") String accessTokenCookieName;

    private final static String[] WHITE_LIST_PATTERNS = {
            "/api/auth/login",
            "/api/auth/signup",
            "/api/auth/refresh",
            "/api/auth/kakao",  // Kakao 소셜 로그인 허용
            "/api/auth/kakao/callback", // Kakao 소셜 로그인 콜백 허용
            "/api/auth/kakao/withdraw", // Kakao 소셜 회원 탈퇴
            "/error",
            "/admin",  // 관리자 페이지 허용
            "/admin/**",
            "/ws/chat/**"  // WebSocket 엔드포인트 허용
    };

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(
                tokenProvider,
                userDetailService,
                authenticationEntryPoint,
                WHITE_LIST_PATTERNS,
                accessTokenCookieName
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
                    authorize.requestMatchers(WHITE_LIST_PATTERNS)
                        .permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest()
                        .authenticated()
                )
                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 전에 추가
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtTokenCookieHandingFilter, JwtAuthenticationFilter.class)

                // 예외 처리 설정
                .exceptionHandling(handling ->
                    handling.authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.addExposedHeader("Set-Cookie");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
