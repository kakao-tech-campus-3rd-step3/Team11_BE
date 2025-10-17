package com.pnu.momeet.common.validation.validator;

import com.pnu.momeet.common.validation.annotation.MeetupTimeUnit;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class MeetupTimeUnitValidator implements ConstraintValidator<MeetupTimeUnit, String> {
    // yyyy-MM-ddTHH:mm 30분 단위 검사 정규식
    private static final Pattern MEETUP_TIME_UNIT_PATTERN =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T(\\d{2}:(00|30))$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null 허용 시 true 반환
        }
        return MEETUP_TIME_UNIT_PATTERN.matcher(value).matches();
    }
}
