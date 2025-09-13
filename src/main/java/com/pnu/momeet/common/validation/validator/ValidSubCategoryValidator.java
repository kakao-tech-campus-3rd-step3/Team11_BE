package com.pnu.momeet.common.validation.validator;

import com.pnu.momeet.common.validation.annotation.ValidSubCategory;
import com.pnu.momeet.domain.meetup.enums.SubCategory;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidSubCategoryValidator implements ConstraintValidator<ValidSubCategory, String> {
    @Override
    public boolean isValid(String subCategory, ConstraintValidatorContext context) {
        if (subCategory == null) {
            return true; // 값이 없으면 검증 통과
        }
        try {
            SubCategory.valueOf(subCategory.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false; // enum에 없는 값이면 검증 실패
        }
        return true;
    }
}

