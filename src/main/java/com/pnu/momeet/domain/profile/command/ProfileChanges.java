package com.pnu.momeet.domain.profile.command;

import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;

public record ProfileChanges(
    String  nickname, boolean nickChanged,
    Integer age, boolean ageChanged,
    Gender gender, boolean genderChanged,
    String  description, boolean descChanged,
    Sigungu baseLocation, boolean baseChanged
) {
}
