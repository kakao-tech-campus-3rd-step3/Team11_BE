package com.pnu.momeet.domain.badge.dto.request;

import com.pnu.momeet.common.validation.annotation.AllowSortFields;
import com.pnu.momeet.domain.common.dto.request.BasePageRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProfileBadgePageRequest extends BasePageRequest {

    @AllowSortFields(
        fields = {"representative","createdAt","name"},
        showFields = true
    )
    private String sort;
}
