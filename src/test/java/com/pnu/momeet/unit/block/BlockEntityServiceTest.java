package com.pnu.momeet.unit.block;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.domain.block.entity.UserBlock;
import com.pnu.momeet.domain.block.repository.BlockRepository;
import com.pnu.momeet.domain.block.service.BlockEntityService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlockEntityServiceTest {

    @Mock
    private BlockRepository blockRepository;

    @InjectMocks
    private BlockEntityService entityService;

    @Test
    @DisplayName("exists: 레포지토리 위임")
    void exists_delegatesToRepository() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        given(blockRepository.existsByBlockerIdAndBlockedId(a, b)).willReturn(true);

        boolean exists = entityService.exists(a, b);

        assertThat(exists).isTrue();
        verify(blockRepository).existsByBlockerIdAndBlockedId(a, b);
    }

    @Test
    @DisplayName("save: UserBlock 생성 후 저장 위임 + 반환")
    void save_persistsAndReturns() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();

        // repo.save가 그대로 엔티티를 반환한다고 가정
        given(blockRepository.save(any(UserBlock.class)))
            .willAnswer(inv -> inv.getArgument(0));

        UserBlock saved = entityService.save(a, b);

        assertThat(saved.getBlockerId()).isEqualTo(a);
        assertThat(saved.getBlockedId()).isEqualTo(b);
        verify(blockRepository).save(any(UserBlock.class));
    }

    @Test
    @DisplayName("delete: (blockerId, blockedId) 조건 삭제 위임")
    void delete_delegatesToRepository() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        given(blockRepository.deleteByBlockerIdAndBlockedId(a, b)).willReturn(1L);

        long deleted = entityService.delete(a, b);

        assertThat(deleted).isEqualTo(1L);
        verify(blockRepository).deleteByBlockerIdAndBlockedId(a, b);
    }
}
