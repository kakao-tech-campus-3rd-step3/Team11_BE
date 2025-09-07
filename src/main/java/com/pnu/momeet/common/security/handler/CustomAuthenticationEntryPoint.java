package com.pnu.momeet.common.security.handler;

import com.pnu.momeet.common.exception.BannedAccountException;
import com.pnu.momeet.common.exception.ConcurrentLoginException;
import com.pnu.momeet.common.exception.DisabledAccountException;
import com.pnu.momeet.common.mapper.ProblemDetailMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailMapper problemDetailMapper = new ProblemDetailMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ProblemDetail problemDetail;

        switch (authException) {
            case BannedAccountException ignored -> {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                problemDetail = ProblemDetail.forStatusAndDetail(
                        HttpStatus.FORBIDDEN,
                        authException.getMessage()
                );
                problemDetail.setTitle("차단된 계정 오류");
            }
            case ConcurrentLoginException ignored -> {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                problemDetail = ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED,
                        authException.getMessage()
                );
                problemDetail.setTitle("중복 로그인 오류");
            }
            case DisabledAccountException ignored -> {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                problemDetail = ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED,
                        authException.getMessage()
                );
                // 사용자 계정 정보 변경 등에 의해 인증은 되었으나, 계정이 비활성화된 경우
                problemDetail.setTitle("비활성화된 계정 오류");
            }
            default -> {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                problemDetail = ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED,
                        authException.getMessage()
                );
                problemDetail.setTitle("인증 실패 오류");
                problemDetail.setInstance(URI.create(request.getRequestURI()));
            }
        }

        response.getWriter().write(problemDetailMapper.toJson(problemDetail));
    }
}
