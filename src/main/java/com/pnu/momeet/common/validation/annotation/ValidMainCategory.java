package com.pnu.momeet.common.validation.annotation;

import com.pnu.momeet.common.validation.validator.ValidMainCategoryValidator;
import jakarta.validation.Constraint;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = { ValidMainCategoryValidator.class })
@Target({ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMainCategory {
    String message() default "유효하지 않은 카테고리리 입니다.";
    Class<?>[] groups() default {};
    Class<? extends jakarta.validation.Payload>[] payload() default {};
}
