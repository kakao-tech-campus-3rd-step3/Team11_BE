package com.pnu.momeet.domain.meetup.dto.request;

import com.pnu.momeet.common.validation.annotation.AllowSortFields;
import com.pnu.momeet.domain.common.dto.request.BasePageRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MeetupSummaryPageRequest extends BasePageRequest {

    @AllowSortFields(
        fields = {"endAt"},
        showFields = true
    )
    private String sort = "endAt,DESC";
}
