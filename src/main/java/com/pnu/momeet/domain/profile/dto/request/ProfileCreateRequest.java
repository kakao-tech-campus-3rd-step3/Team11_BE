package com.pnu.momeet.domain.profile.dto.request;

import com.pnu.momeet.common.validation.annotation.ValidImage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record ProfileCreateRequest (
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2~20자 사이로 작성해주세요.")
    @Pattern(
        regexp = "^[a-zA-Z0-9가-힣]+$",
        message = "닉네임에는 한글, 영문, 숫자만 사용할 수 있습니다."
    )
    String nickname,

    @NotNull(message = "나이는 필수입니다.")
    @Min(value = 14, message = "14세 이상 100세 이하만 가입할 수 있습니다.")
    @Max(value = 100, message = "14세 이상 100세 이하만 가입할 수 있습니다.")
    Integer age,

    @NotBlank(message = "성별은 필수입니다.")
    String gender,

    @ValidImage
    MultipartFile image,

    @Size(max = 500, message = "최대 500자까지 작성할 수 있습니다.")
    String description,

    @NotBlank(message = "지역은 반드시 입력해야 합니다. (예: '부산 금정구', '서울 마포구')")
    String baseLocation
    ) {
}
