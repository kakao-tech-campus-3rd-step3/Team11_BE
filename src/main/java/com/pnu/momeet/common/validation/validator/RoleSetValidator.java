package com.pnu.momeet.common.validation.validator;

import com.pnu.momeet.common.validation.annotation.RoleSet;
import com.pnu.momeet.domain.member.enums.Role;
import jakarta.validation.ConstraintValidator;

import java.util.List;

public class RoleSetValidator implements ConstraintValidator<RoleSet, List<String>> {

    @Override
    public boolean isValid(List<String> roles, jakarta.validation.ConstraintValidatorContext context) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }

        for (String role : roles) {
            try {
                Role.valueOf(role);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return true;
    }
}
