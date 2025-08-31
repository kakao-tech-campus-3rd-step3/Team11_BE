package com.pnu.momeet.common.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        var problemDetail = generateProblemDetail(request, authException);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        try {
            response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
        } catch (IOException e) {
            response.getWriter().write("{" +
                "\"type\":\"https://example.com/error/internal\"," +
                "\"title\":\"인증 중 서버 내부 에러\"," +
                "\"status\":500," +
                "\"detail\":\"예외를 직렬화하던 중 문제가 발생했습니다.\"," +
                "\"instance\":\"/\"" +
                "}"
            );
        }
    }

    private ProblemDetail generateProblemDetail(
            HttpServletRequest request,
            AuthenticationException authException
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                authException.getMessage()
        );
        problemDetail.setTitle("인증 실패 오류");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }
}
