package com.pnu.momeet.common.validation.annotation;


import com.pnu.momeet.common.validation.validator.ValidMeetupStatusValidator;
import jakarta.validation.Constraint;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = { ValidMeetupStatusValidator.class })
@Target({ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMeetupStatus {
    String message() default "유효하지 않은 밋업 상태입니다.";
    Class<?>[] groups() default {};
    Class<? extends jakarta.validation.Payload>[] payload() default {};
}
