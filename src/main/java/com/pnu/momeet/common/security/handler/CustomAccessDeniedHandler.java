package com.pnu.momeet.common.security.handler;

import com.pnu.momeet.common.mapper.ProblemDetailMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailMapper problemDetailMapper = new ProblemDetailMapper();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "권한이 부족하여 요청한 리소스에 접근할 수 없습니다 : " + accessDeniedException.getMessage()
        );
        problemDetail.setTitle("접근 거부 오류");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        response.getWriter().write(problemDetailMapper.toJson(problemDetail));
    }
}
