package com.pnu.momeet.common.validation.annotation;

import com.pnu.momeet.common.validation.validator.LocationValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LocationValidator.class)
@Documented
public @interface ValidLocation {
    Mode mode() default Mode.REQUIRED; // 생성: REQUIRED, 수정: OPTIONAL
    String message() default "지역 입력이 필요합니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    enum Mode { REQUIRED, OPTIONAL }
}
