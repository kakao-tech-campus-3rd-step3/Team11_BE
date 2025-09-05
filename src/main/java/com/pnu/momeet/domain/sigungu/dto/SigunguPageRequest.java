package com.pnu.momeet.domain.sigungu.dto;

import com.pnu.momeet.common.validation.annotation.AllowSortFields;
import com.pnu.momeet.domain.common.dto.BasePageRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SigunguPageRequest extends BasePageRequest {

    private Long sidoCode;

    private Long sigunguCode;

    @AllowSortFields(fields = {"sidoName", "sigunguName"}, showFields = true)
    private String sort;
}
