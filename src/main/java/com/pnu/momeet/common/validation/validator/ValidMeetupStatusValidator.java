package com.pnu.momeet.common.validation.validator;

import com.pnu.momeet.common.validation.annotation.ValidMeetupStatus;
import com.pnu.momeet.domain.meetup.enums.MeetupStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidMeetupStatusValidator implements ConstraintValidator<ValidMeetupStatus, String> {
    @Override
    public boolean isValid(String meetupStatus, ConstraintValidatorContext context) {
        if (meetupStatus == null) {
            return true; // 값이 없으면 검증 통과
        }
        try {
            MeetupStatus.valueOf(meetupStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false; // enum에 없는 값이면 검증 실패
        }
        return true;
    }
}