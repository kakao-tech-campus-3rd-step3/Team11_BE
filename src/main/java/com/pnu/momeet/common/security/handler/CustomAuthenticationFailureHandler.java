package com.pnu.momeet.common.security.handler;

import com.pnu.momeet.common.mapper.ProblemDetailMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
    private final ProblemDetailMapper problemDetailMapper = new ProblemDetailMapper();

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "인증이 실패했습니다."
        );
        problemDetail.setTitle("인증 실패 오류");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        if (exception instanceof LockedException) {
            problemDetail.setDetail("계정이 잠겼습니다. 관리자에게 문의하세요.");
        } else if (exception instanceof DisabledException) {
            problemDetail.setDetail("로그아웃, 사용자 정보 변경 등의 이유로 토큰이 만료되었습니다. 다시 로그인 해주세요.");
        }

        response.getWriter().write(problemDetailMapper.toJson(problemDetail));
    }
}
