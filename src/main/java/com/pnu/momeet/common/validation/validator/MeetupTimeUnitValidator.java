package com.pnu.momeet.common.validation.validator;

import com.pnu.momeet.common.validation.annotation.MeetupTimeUnit;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public class MeetupTimeUnitValidator implements ConstraintValidator<MeetupTimeUnit, String> {
    // yyyy-MM-ddTHH:mm 30분 단위 검사 정규식
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null 허용 시 true 반환
        }
        try {
            LocalDateTime dt = LocalDateTime.parse(value, FORMATTER);
            int minute = dt.getMinute();
            return minute == 0 || minute == 30;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
