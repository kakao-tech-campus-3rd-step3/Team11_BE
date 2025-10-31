package com.pnu.momeet.common.validation.annotation;

import com.pnu.momeet.common.validation.validator.MeetupTimeUnitValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = { MeetupTimeUnitValidator.class })
@Target({ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface MeetupTimeUnit {
    String message() default "'yyyy-MM-ddTHH:mm' 형식의 10분 단위 시간만 허용됩니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
