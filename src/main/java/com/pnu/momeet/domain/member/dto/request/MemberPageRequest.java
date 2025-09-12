package com.pnu.momeet.domain.member.dto.request;

import com.pnu.momeet.common.validation.annotation.AllowSortFields;
import com.pnu.momeet.domain.common.dto.BasePageRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MemberPageRequest extends BasePageRequest {

    @AllowSortFields(fields = {"email", "createdAt", "updatedAt"}, showFields = true)
    private String sort;
}
