package com.pnu.momeet.common.validation.validator;

import com.pnu.momeet.common.validation.annotation.MeetupTimeUnit;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

public class MeetupTimeUnitValidator implements ConstraintValidator<MeetupTimeUnit, String> {
    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm")
            .optionalStart()
            .appendPattern(":ss")
            .optionalEnd()
            .toFormatter();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null 허용 시 true 반환
        }
        try {
            LocalDateTime dt = LocalDateTime.parse(value, FORMATTER);
            int minute = dt.getMinute();
            int second = dt.getSecond();
            return second == 0 && minute % 10 == 0; // 10분 단위인지 확인
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
