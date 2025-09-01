package com.pnu.momeet.common.validation.annotation;

import com.pnu.momeet.common.validation.validator.RoleSetValidator;
import jakarta.validation.Constraint;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = { RoleSetValidator.class })
@Target({ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface RoleSet {
    String message() default "유효하지 않은 역할이 포함되어 있습니다.";
    Class<?>[] groups() default {};
    Class<? extends jakarta.validation.Payload>[] payload() default {};
}
