package com.pnu.momeet.domain.meetup.dto.request;

import com.pnu.momeet.common.validation.annotation.AllowSortFields;
import com.pnu.momeet.common.validation.annotation.ValidMainCategory;
import com.pnu.momeet.common.validation.annotation.ValidMeetupStatus;
import com.pnu.momeet.common.validation.annotation.ValidSubCategory;
import com.pnu.momeet.domain.common.dto.request.BasePageRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MeetupPageRequest extends BasePageRequest {

    @NotNull(message = "시군구 코드는 필수입니다.")
    private Long sigunguCode;

    @ValidMainCategory
    private String category;

    @ValidSubCategory
    private String subCategory;

    @ValidMeetupStatus
    private String status;

    private String search;

    @AllowSortFields(
        fields = {"scoreLimit", "name","createdAt", "endAt"},
        showFields = true
    )
    private String sort;

    public void setCategory(String category) {
        if (category != null) {
            this.category = category.toUpperCase();
        }
    }

    public void setSubCategory(String subCategory) {
        if (subCategory != null) {
            this.subCategory = subCategory.toUpperCase();
        }
    }

    public void setStatus(String status) {
        if (status != null) {
            this.status = status.toUpperCase();
        }
    }
}
