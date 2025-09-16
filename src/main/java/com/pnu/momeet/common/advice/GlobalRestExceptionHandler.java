package com.pnu.momeet.common.advice;

import com.pnu.momeet.common.exception.BannedAccountException;
import com.pnu.momeet.common.exception.CustomValidationException;
import com.pnu.momeet.common.exception.StorageException;
import com.pnu.momeet.common.exception.UnMatchedPasswordException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.*;

@RestControllerAdvice
public class GlobalRestExceptionHandler {

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

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalStateException(IllegalStateException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problemDetail.setTitle("요청 상태 오류");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
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

    @ExceptionHandler(CustomValidationException.class)
    public ResponseEntity<ProblemDetail> handleCustomValidationException(CustomValidationException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problemDetail.setTitle("유효성 검사 실패");
        problemDetail.setProperty("validationErrors", e.getFieldErrors());
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(UnMatchedPasswordException.class)
    public ResponseEntity<ProblemDetail> handleUnMatchedPasswordException(UnMatchedPasswordException ignored) {
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

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ProblemDetail> handleSecurityException(SecurityException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
        problemDetail.setTitle("권한 없음 오류");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateKeyException(DuplicateKeyException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "중복된 키 오류: " + e.getMessage());
        problemDetail.setTitle("중복된 키 오류");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ProblemDetail> handleStorageException(StorageException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        problemDetail.setTitle("서버 저장소 오류");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }
}
