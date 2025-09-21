package com.pnu.momeet.domain.sigungu.dto.request;

import com.pnu.momeet.common.validation.annotation.AllowSortFields;
import com.pnu.momeet.domain.common.dto.request.BasePageRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SigunguPageRequest extends BasePageRequest {

    private Long sidoCode;

    @AllowSortFields(fields = {"sidoName", "sigunguName"}, showFields = true)
    private String sort;
}
