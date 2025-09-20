package com.pnu.momeet.domain.common.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class BaseCursorRequest<K>{
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    private Integer size = 10;
    private K cursorId;
}
