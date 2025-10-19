package com.pnu.momeet.domain.block.service.mapper;

import com.pnu.momeet.domain.block.dto.request.BlockPageRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BlockDtoMapper {

    public static PageRequest toPageRequest(BlockPageRequest request) {
        return PageRequest.of(
            request.getPage(),
            request.getSize()
        );
    }
}
