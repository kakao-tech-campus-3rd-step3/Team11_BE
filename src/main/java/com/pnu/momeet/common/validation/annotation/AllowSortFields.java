package com.pnu.momeet.common.validation.annotation;

import com.pnu.momeet.common.validation.validator.SortFieldsValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = { SortFieldsValidator.class })
@Target({ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowSortFields {
    String message() default "필드 정렬 조건이 올바르지 않습니다. ex)field1,asc,field2,desc,...";
    String[] fields() default {};
    boolean showFields() default false;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
