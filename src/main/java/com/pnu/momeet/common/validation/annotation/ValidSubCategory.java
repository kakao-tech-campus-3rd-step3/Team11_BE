package com.pnu.momeet.common.validation.annotation;

import com.pnu.momeet.common.validation.validator.ValidSubCategoryValidator;
import jakarta.validation.Constraint;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = { ValidSubCategoryValidator.class })
@Target({ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSubCategory {
    String message() default "유효하지 않은 서브카테고리 입니다.";
    Class<?>[] groups() default {};
    Class<? extends jakarta.validation.Payload>[] payload() default {};
}

