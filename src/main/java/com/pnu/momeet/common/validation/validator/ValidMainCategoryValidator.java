package com.pnu.momeet.common.validation.validator;

import com.pnu.momeet.common.validation.annotation.ValidMainCategory;
import com.pnu.momeet.domain.meetup.enums.MainCategory;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidMainCategoryValidator implements ConstraintValidator<ValidMainCategory, String> {
    @Override
    public boolean isValid(String category, ConstraintValidatorContext context) {
        if (category == null) {
            return true; // 값이 없으면 검증 통과
        }
        try {
            MainCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false; // enum에 없는 값이면 검증 실패
        }
        return true;
    }
}

