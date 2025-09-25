package com.pnu.momeet.domain.badge.dto.request;

import com.pnu.momeet.common.validation.annotation.ValidImage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record BadgeCreateRequest(
    @NotBlank(message = "배지 이름은 필수입니다.")
    @Size(min = 2, max = 20, message = "배지 이름은 2~20자 사이로 작성해주세요.")
    String name,

    @Size(max = 255, message = "최대 255자까지 작성할 수 있습니다.")
    String description,

    @NotNull(message = "배지 아이콘은 필수입니다.")
    @ValidImage
    MultipartFile iconImage,

    @NotBlank(message = "배지 코드는 필수입니다.")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "배지 코드는 대문자/숫자/밑줄만 가능합니다.")
    String code
) {
}
