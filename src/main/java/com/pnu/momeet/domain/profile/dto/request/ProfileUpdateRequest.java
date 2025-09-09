package com.pnu.momeet.domain.profile.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요.")
    @Pattern(
        regexp = "^[a-zA-Z0-9가-힣]+$",
        message = "닉네임에는 한글, 영문, 숫자만 사용할 수 있습니다."
    )
    String nickname,

    @Min(value = 14, message = "14세 이상 100세 이하만 가입할 수 있습니다.")
    @Max(value = 100, message = "14세 이상 100세 이하만 가입할 수 있습니다.")
    Integer age,

    @Pattern(regexp = "^(MALE|FEMALE)$", message = "성별은 MALE 또는 FEMALE만 가능합니다.")
    String gender,

    @Size(max = 255, message = "이미지 URL은 최대 255자까지 입력할 수 있습니다.")
    String imageUrl,

    @Size(max = 500, message = "소개글은 최대 500자까지 입력할 수 있습니다.")
    String description,

    @Size(max = 100, message = "지역은 최대 100자까지 입력할 수 있습니다.")
    String baseLocation
) {
}
