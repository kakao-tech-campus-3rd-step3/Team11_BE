package com.pnu.momeet.common.security.filter;

import com.pnu.momeet.common.exception.ConcurrentLoginException;
import com.pnu.momeet.common.exception.JwtAuthenticationException;
import com.pnu.momeet.common.model.TokenInfo;
import com.pnu.momeet.common.security.JwtTokenProvider;
import com.pnu.momeet.common.security.details.CustomUserDetailService;
import com.pnu.momeet.common.security.details.CustomUserDetails;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailService userDetailsService;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final Pattern[] whitelistPatterns;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            CustomUserDetailService userDetailsService,
            AuthenticationEntryPoint authenticationEntryPoint,
            String[] whitelistUrls
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.whitelistPatterns = new Pattern[whitelistUrls.length];
        for (int i = 0; i < whitelistUrls.length; i++) {
            String regex = whitelistUrls[i]
                    .replace("**", ".*")  // '**'를 '.*'로 변경
                    .replace("*", "[^/]*"); // '*'를 '[^/]*'로 변경
            this.whitelistPatterns[i] = Pattern.compile(regex);
        }
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // 화이트리스트 경로는 토큰 검사 없이 통과
        if (isWhitelisted(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            // token이 null 이거나 유효하지 않으면 예외 발생
            String token = Objects.requireNonNull(resolveToken(request));
            // 파싱 중 예외 발생 시 catch 블록으로 이동
            TokenInfo tokenInfo = jwtTokenProvider.parseToken(token);

            // 해당 ID의 사용자가 존재하지 않으면 예외 발생
            CustomUserDetails userDetails = userDetailsService.loadUserByUsername(tokenInfo.subject());

            // 사용자의 로그인 시간이 토큰 발급 시간 이후면 예외 발생
            if (userDetails.getLastLoginAt().isBefore(tokenInfo.issuedAt())) {
                throw new ConcurrentLoginException("다른 기기에서 로그인하여 토큰이 만료되었습니다. 다시 로그인 해주세요.");
            }

            var authenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            // 토큰이 만료된 경우
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, new JwtAuthenticationException("토큰이 만료되었습니다."));
            // 다른 위치에서 로그인 된 경우
        } catch (ConcurrentLoginException e) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, e);

        } catch (Exception e) {
            // 토큰이 없거나, 사용자가 존재하지 않거나, 그 외 토큰이 유효하지 않은 경우
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, new JwtAuthenticationException("유효하지 않은 토큰입니다."));
        }
    }

    private boolean isWhitelisted(String path) {
        for (Pattern pattern : whitelistPatterns) {
            if (pattern.matcher(path).matches()) {
                return true;
            }
        }
        return false;
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
