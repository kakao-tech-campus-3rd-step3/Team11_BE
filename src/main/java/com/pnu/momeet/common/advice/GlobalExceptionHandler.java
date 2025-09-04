package com.pnu.momeet.common.advice;

import com.pnu.momeet.common.exception.BannedAccountException;
import com.pnu.momeet.common.exception.ExistEmailException;
import com.pnu.momeet.common.exception.UnMatchedPasswordException;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private Map<String, List<String>> extractValidationErrors(MethodArgumentNotValidException e) {
        // 필드 에러 처리
        Map<String, List<String>> fieldErrors = new HashMap<>();
        e.getBindingResult()
            .getFieldErrors()
            .forEach(error -> {
                String field = error.getField();
                String message = error.getDefaultMessage();
                if (message == null) {
                    return;
                }
                fieldErrors.computeIfAbsent(field, key -> new ArrayList<>()).add(message);
        });

        // 글로벌 에러 처리 (클래스 레벨)
        e.getBindingResult().getGlobalErrors().forEach(error -> {
            String message = error.getDefaultMessage();
            if (message == null) return;
            fieldErrors.computeIfAbsent("global", key -> new ArrayList<>()).add(message);
        });

        return fieldErrors;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problemDetail.setTitle("잘못된 인자를 값 전달");

        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> handleNoSuchElementException(NoSuchElementException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problemDetail.setTitle("요청한 리소스를 찾을 수 없음");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    // 유효성 검사 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "유효성 검사에 실패했습니다.");
        problemDetail.setTitle("유효성 검사 실패");
        problemDetail.setProperty("validationErrors", extractValidationErrors(e));
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(UnMatchedPasswordException.class)
    public ResponseEntity<ProblemDetail> handleUnMatchedPasswordException(UnMatchedPasswordException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.");
        problemDetail.setTitle("유효성 검사 실패");
        problemDetail.setProperty("validationErrors", Map.of("password2", "비밀번호가 일치하지 않습니다."));
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(BannedAccountException.class)
    public ResponseEntity<ProblemDetail> handleBannedAccountException(BannedAccountException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
        problemDetail.setTitle("차단된 계정 오류");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
    }

    @ExceptionHandler(ExistEmailException.class)
    public ResponseEntity<ProblemDetail> handleExistEmailException(ExistEmailException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problemDetail.setTitle("중복된 이메일 오류");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }
}
