package com.pnu.momeet.unit.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pnu.momeet.domain.profile.dto.ProfileCreateRequest;
import com.pnu.momeet.domain.profile.dto.ProfileResponse;
import com.pnu.momeet.domain.profile.dto.ProfileUpdateRequest;
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

        ProfileResponse resp = profileService.getMyProfile(memberId);

        assertThat(resp.nickname()).isEqualTo("테스트유저");
        assertThat(resp.age()).isEqualTo(25);
        assertThat(resp.gender()).isEqualTo(Gender.MALE);
        assertThat(resp.baseLocation()).isEqualTo("부산 금정구");
        verify(profileRepository).findByMemberId(memberId);
    }

    @Test
    @DisplayName("내 프로필 조회 실패 - 프로필 없음이면 NoSuchElementException 발생")
    void getMyProfile_notFound() {
        UUID memberId = UUID.randomUUID();
        given(profileRepository.findByMemberId(memberId)).willReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> profileService.getMyProfile(memberId));
        verify(profileRepository).findByMemberId(memberId);
    }

    @Test
    @DisplayName("내 프로필 생성 성공")
    void createMyProfile_success() {
        UUID memberId = UUID.randomUUID();
        ProfileCreateRequest request = new ProfileCreateRequest(
            "새로운유저",
            30,
            "FEMALE",
            "url",
            "소개",
            "장소"
        );

        given(profileRepository.existsByMemberId(memberId)).willReturn(false);
        given(profileRepository.existsByNicknameIgnoreCase(request.nickname().trim())).willReturn(false);
        given(profileRepository.save(any(Profile.class))).will(returnsFirstArg());

        ProfileResponse resp = profileService.createMyProfile(memberId, request);

        assertThat(resp.nickname()).isEqualTo("새로운유저");
        assertThat(resp.age()).isEqualTo(30);
        assertThat(resp.gender()).isEqualTo(Gender.FEMALE);

        verify(profileRepository).existsByMemberId(memberId);
        verify(profileRepository).existsByNicknameIgnoreCase(request.nickname().trim());
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    @DisplayName("내 프로필 생성 실패 - 이미 프로필이 존재하면 IllegalStateException 발생")
    void createMyProfile_fail_profileAlreadyExists() {
        UUID memberId = UUID.randomUUID();
        ProfileCreateRequest request = new ProfileCreateRequest(
            "닉네임",
            20,
            "MALE",
            "url",
            "소개",
            "장소"
        );

        given(profileRepository.existsByMemberId(memberId)).willReturn(true);

        assertThrows(IllegalStateException.class,
            () -> profileService.createMyProfile(memberId, request));

        verify(profileRepository).existsByMemberId(memberId);
        verify(profileRepository, never()).existsByNicknameIgnoreCase(anyString());
        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    @DisplayName("내 프로필 생성 실패 - 이미 닉네임이 존재하면 IllegalArgumentException 발생")
    void createMyProfile_fail_nicknameAlreadyExists() {
        UUID memberId = UUID.randomUUID();
        ProfileCreateRequest request = new ProfileCreateRequest(
            "중복된닉네임",
            20,
            "MALE",
            "url",
            "소개",
            "장소"
        );

        given(profileRepository.existsByMemberId(memberId)).willReturn(false);
        given(profileRepository.existsByNicknameIgnoreCase(request.nickname().trim())).willReturn(true);

        assertThrows(IllegalArgumentException.class,
            () -> profileService.createMyProfile(memberId, request));

        verify(profileRepository).existsByMemberId(memberId);
        verify(profileRepository).existsByNicknameIgnoreCase(request.nickname().trim());
        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    @DisplayName("내 프로필 수정 성공")
    void updateMyProfile_success() {
        UUID memberId = UUID.randomUUID();
        Profile existingProfile = Profile.create(
            memberId,
            "기존유저",
            25,
            Gender.MALE,
            "url",
            "소개",
            "장소"
        );
        ProfileUpdateRequest request = new ProfileUpdateRequest(
            "수정된유저",
            30,
            "FEMALE",
            "new_url",
            "수정된소개",
            "수정된장소"
        );

        given(profileRepository.findByMemberId(memberId)).willReturn(Optional.of(existingProfile));
        given(profileRepository.existsByNicknameIgnoreCase(request.nickname().trim())).willReturn(false);

        ProfileResponse resp = profileService.updateMyProfile(memberId, request);

        // then
        assertThat(resp.nickname()).isEqualTo("수정된유저");
        assertThat(resp.age()).isEqualTo(30);
        assertThat(resp.gender()).isEqualTo(Gender.FEMALE);

        verify(profileRepository).findByMemberId(memberId);
        verify(profileRepository).existsByNicknameIgnoreCase(request.nickname().trim());
    }

    @Test
    @DisplayName("내 프로필 수정 실패 - 프로필이 존재하지 않으면 NoSuchElementException 발생")
    void updateMyProfile_fail_profileNotFound() {
        UUID memberId = UUID.randomUUID();
        ProfileUpdateRequest request = new ProfileUpdateRequest(
            "닉네임",
            20,
            "MALE",
            "url",
            "소개",
            "장소"
        );

        given(profileRepository.findByMemberId(memberId)).willReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
            () -> profileService.updateMyProfile(memberId, request));

        verify(profileRepository).findByMemberId(memberId);
        verify(profileRepository, never()).existsByNicknameIgnoreCase(anyString());
    }

    @Test
    @DisplayName("내 프로필 수정 실패 - 닉네임이 중복되면 IllegalArgumentException 발생")
    void updateMyProfile_fail_duplicateNickname() {
        UUID memberId = UUID.randomUUID();
        Profile existingProfile = Profile.create(
            memberId,
            "기존유저",
            25,
            Gender.MALE,
            "url",
            "소개",
            "장소"
        );
        ProfileUpdateRequest request = new ProfileUpdateRequest(
            "중복된닉네임",
            30,
            "FEMALE",
            "url",
            "소개",
            "장소"
        );

        given(profileRepository.findByMemberId(memberId)).willReturn(Optional.of(existingProfile));
        given(profileRepository.existsByNicknameIgnoreCase(request.nickname().trim())).willReturn(true);

        assertThrows(IllegalArgumentException.class,
            () -> profileService.updateMyProfile(memberId, request));

        verify(profileRepository).findByMemberId(memberId);
        verify(profileRepository).existsByNicknameIgnoreCase(request.nickname().trim());
    }
}