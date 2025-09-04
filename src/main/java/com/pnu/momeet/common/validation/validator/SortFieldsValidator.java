package com.pnu.momeet.common.validation.validator;

import com.pnu.momeet.common.validation.annotation.AllowSortFields;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SortFieldsValidator implements ConstraintValidator<AllowSortFields, String> {

    private Set<String> allowedFields;
    boolean showFields;
    static Set<String> DIRECTIONS = Set.of("asc", "desc");

    @Override
    public void initialize(AllowSortFields constraintAnnotation) {
        allowedFields = new HashSet<>(Arrays.asList(constraintAnnotation.fields()));
        showFields = constraintAnnotation.showFields();
    }

    private void changeMessageIfShowFields(ConstraintValidatorContext context) {
        if (showFields) {
            String fields = String.join(", ", allowedFields);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "필드 정렬 조건이 올바르지 않습니다. ex)field1,asc,field2,desc (허용된 필드: " + fields + ")"
                    )
                    .addConstraintViolation();
        }
    }


    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // 값이 없으면 검증 통과
        }
        String[] parts = value.split(",");
        if (parts.length % 2 != 0) {
            changeMessageIfShowFields(context);
            return false; // 필드와 방향이 쌍을 이루지 않으면 검증 실패
        }
        for (int i = 0; i < parts.length / 2; i++) {
            String field = parts[i * 2].trim();
            String direction = parts[i * 2 + 1].trim().toLowerCase();
            if (!allowedFields.contains(field) || !DIRECTIONS.contains(direction)) {
                changeMessageIfShowFields(context);
                return false; // 허용되지 않은 필드가 있으면 검증 실패
            }
        }
        return true;
    }
}