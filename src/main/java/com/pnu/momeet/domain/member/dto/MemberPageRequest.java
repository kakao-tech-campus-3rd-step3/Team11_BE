package com.pnu.momeet.domain.member.dto;

import com.pnu.momeet.common.validation.annotation.AllowSortFields;
import com.pnu.momeet.domain.common.dto.BasePageRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MemberPageRequest extends BasePageRequest {

    @AllowSortFields(fields = {"email", "createdAt", "updatedAt"})
    private String sort;
}
