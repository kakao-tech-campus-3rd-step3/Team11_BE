package com.pnu.momeet.domain.block.mapper;

import com.pnu.momeet.domain.block.dto.response.BlockResponse;
import com.pnu.momeet.domain.block.entity.UserBlock;

public class BlockEntityMapper {

    public static BlockResponse toBlockResponse(UserBlock block) {
        return new BlockResponse(
            block.getBlockerId(),
            block.getBlockedId(),
            block.getCreatedAt()
        );
    }
}
