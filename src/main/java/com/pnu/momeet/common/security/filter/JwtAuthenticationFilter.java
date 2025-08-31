package com.pnu.momeet.common.security.filter;

import com.pnu.momeet.common.exception.JwtAuthenticationException;
import com.pnu.momeet.common.util.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final Pattern[] whitelistPatterns;
    private final String refreshPath;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            UserDetailsService userDetailsService,
            AuthenticationEntryPoint authenticationEntryPoint,
            String[] whitelistUrls,
            String refreshPath
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
        this.refreshPath = refreshPath;
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
            Claims claims = jwtTokenProvider.getPayload(token);
            String memberId = claims.getSubject();
            saveAuthenticationToContext(memberId, request);
            filterChain.doFilter(request, response);
        } catch(ExpiredJwtException e) {
            // 토큰이 만료된 경우, SecurityContext를 비우고 401 응답
            if (requestPath.equals(refreshPath)) {
                // 리프레시 토큰 요청인 경우, 컨텍스트를 비우지 않고 다음 필터로 진행
                try {
                    String memberId = e.getClaims().getSubject();
                    saveAuthenticationToContext(memberId, request);
                    filterChain.doFilter(request, response);
                    return;
                } catch (Exception ex) {
                    SecurityContextHolder.clearContext();
                    authenticationEntryPoint.commence(request, response, new JwtAuthenticationException("유효하지 않은 토큰입니다."));
                    return;
                }
            }
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, new JwtAuthenticationException("토큰이 만료되었습니다."));
        } catch (Exception e) {
            // 그 외의 예외는 모두 401 응답
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

    private void saveAuthenticationToContext(String memberId, HttpServletRequest request) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(memberId);
        var authenticationToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
}
