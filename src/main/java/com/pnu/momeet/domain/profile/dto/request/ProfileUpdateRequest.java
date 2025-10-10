package com.pnu.momeet.domain.profile.dto.request;

import com.pnu.momeet.common.validation.annotation.ValidImage;
import com.pnu.momeet.common.validation.annotation.ValidLocation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

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

    @ValidImage
    MultipartFile image,

    @Size(max = 500, message = "소개글은 최대 500자까지 입력할 수 있습니다.")
    String description,

    @Valid
    @ValidLocation(mode = ValidLocation.Mode.OPTIONAL)
    LocationInput baseLocation
) {
}
