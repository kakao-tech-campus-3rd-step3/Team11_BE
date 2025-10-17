package com.pnu.momeet.domain.report.dto.request;

import com.pnu.momeet.common.validation.annotation.ValidImage;
import com.pnu.momeet.domain.report.enums.ReportCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public record ReportCreateRequest(

    @NotNull(message = "신고할 프로필 아이디는 필수입니다.")
    UUID targetProfileId,

    @NotNull(message = "신고 카테고리는 필수입니다.")
    ReportCategory category,

    @Size(max = 5, message = "신고 이미지는 최대 5장까지 가능합니다.")
    List<@ValidImage MultipartFile> images,

    @Size(max = 1000, message = "신고 상세 내용은 최대 1000자까지 가능합니다.")
    String detail
) {
}
