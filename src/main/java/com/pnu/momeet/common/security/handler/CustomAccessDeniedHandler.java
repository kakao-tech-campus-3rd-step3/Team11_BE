package com.pnu.momeet.common.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {

        try {
            var problemDetail = createProblemDetail(request, accessDeniedException);
            response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
        } catch (IOException e) {
            response.getWriter().write("{" +
                "\"type\":\"https://example.com/error/internal\"," +
                "\"title\":\"접근 거부 중 서버 내부 에러\"," +
                "\"status\":500," +
                "\"detail\":\"예외를 직렬화하던 중 문제가 발생했습니다.\"," +
                "\"instance\":\"/\""+
                "}"
            );
        }
    }

    private ProblemDetail createProblemDetail(
            HttpServletRequest request,
            AccessDeniedException accessDeniedException
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "권한이 부족하여 요청한 리소스에 접근할 수 없습니다 : " + accessDeniedException.getMessage()
        );
        problemDetail.setTitle("접근 거부 오류");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }
}
