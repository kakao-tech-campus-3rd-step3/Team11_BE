package com.pnu.momeet.domain.badge.dto.request;

import com.pnu.momeet.common.validation.annotation.AllowSortFields;
import com.pnu.momeet.domain.common.dto.request.BasePageRequest;
import com.pnu.momeet.domain.common.mapper.PageMapper;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Getter
@Setter
@NoArgsConstructor
public class BadgePageRequest extends BasePageRequest {

    @AllowSortFields(
        fields = {"createdAt","name"},
        showFields = true
    )
    private String sort;

    // 도메인 정책(전체 배지 조회 허용 정렬)
    private static final Set<String> ALLOWED_SORTS = Set.of("createdAt", "name");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("createdAt"));

    public PageRequest toPageRequest() {
        Sort sortObj = PageMapper.toSortOrDefault(this.sort, ALLOWED_SORTS, DEFAULT_SORT);
        return PageRequest.of(getPage(), getSize(), sortObj);
    }
}
