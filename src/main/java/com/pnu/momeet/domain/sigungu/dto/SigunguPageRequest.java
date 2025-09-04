package com.pnu.momeet.domain.sigungu.dto;

import com.pnu.momeet.common.validation.annotation.AllowSortFields;
import com.pnu.momeet.domain.common.dto.BasePageRequest;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SigunguPageRequest extends BasePageRequest {

    @Pattern(regexp = "^[0-9]+$", message = "숫자 형식으로 입력해주세요.")
    private String sidoCode;

    @Pattern(regexp = "^[0-9]+$", message = "숫자 형식으로 입력해주세요.")
    private String sigunguCode;

    @AllowSortFields(fields = {"sidoName", "sigunguName"}, showFields = true)
    private String sort;
}
