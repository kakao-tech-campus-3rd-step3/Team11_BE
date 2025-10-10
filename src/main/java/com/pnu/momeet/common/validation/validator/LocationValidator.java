package com.pnu.momeet.common.validation.validator;

import com.pnu.momeet.common.validation.annotation.ValidLocation;
import com.pnu.momeet.domain.profile.dto.request.LocationInput;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LocationValidator implements
    ConstraintValidator<ValidLocation, LocationInput> {

    private ValidLocation.Mode mode;

    @Override
    public void initialize(ValidLocation constraintAnnotation) {
        this.mode = constraintAnnotation.mode();
    }

    @Override
    public boolean isValid(LocationInput input, ConstraintValidatorContext context) {
        if (input == null) return mode == ValidLocation.Mode.OPTIONAL;

        boolean hasId   = input.baseLocationId() != null;
        boolean hasName = notBlank(input.sidoName()) && notBlank(input.sigunguName());
        boolean hasLL   = input.latitude() != null && input.longitude() != null;

        int variantCount  = (hasId ? 1 : 0) + (hasName ? 1 : 0) + (hasLL ? 1 : 0);
        return variantCount  == 1;  // 한 가지 방법만 허용
    }

    private boolean notBlank(String s){
        return s != null && !s.isBlank();
    }
}
