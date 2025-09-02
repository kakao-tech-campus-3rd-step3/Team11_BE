package com.pnu.momeet.unit.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.domain.profile.dto.ProfileResponse;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.repository.ProfileRepository;
import com.pnu.momeet.domain.profile.service.ProfileService;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private ProfileService profileService;

    @Test
    @DisplayName("내 프로필 조회 성공 - ProfileResponse 매핑 검증")
    void getMyProfile_success() {
        // given
        UUID memberId = UUID.randomUUID();
        Profile profile = Profile.create(
            memberId,
            "테스트유저",
            25,
            Gender.MALE,
            "https://cdn.example.com/p.png",
            "소개글",
            "부산 금정구"
        );
        given(profileRepository.findByMemberId(memberId)).willReturn(Optional.of(profile));

        // when
        ProfileResponse resp = profileService.getMyProfile(memberId);

        // then
        assertThat(resp.nickname()).isEqualTo("테스트유저");
        assertThat(resp.age()).isEqualTo(25);
        assertThat(resp.gender()).isEqualTo(Gender.MALE);
        assertThat(resp.baseLocation()).isEqualTo("부산 금정구");
        verify(profileRepository).findByMemberId(memberId);
    }

    @Test
    @DisplayName("내 프로필 조회 실패 - 프로필 없음이면 NoSuchElementException 발생")
    void getMyProfile_notFound() {
        // given
        UUID memberId = UUID.randomUUID();
        given(profileRepository.findByMemberId(memberId)).willReturn(Optional.empty());

        // when / then
        assertThrows(NoSuchElementException.class,
            () -> profileService.getMyProfile(memberId));
        verify(profileRepository).findByMemberId(memberId);
    }
}