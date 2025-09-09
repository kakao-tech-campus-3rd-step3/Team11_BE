package com.pnu.momeet.unit.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.profile.dto.request.ProfileCreateRequest;
import com.pnu.momeet.domain.profile.dto.response.ProfileResponse;
import com.pnu.momeet.domain.profile.dto.request.ProfileUpdateRequest;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.repository.ProfileRepository;
import com.pnu.momeet.domain.profile.service.ProfileService;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private S3StorageService s3StorageService;

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
    @DisplayName("ID로 프로필 단건 조회 성공")
    void getProfileById_success() {
        // given
        UUID profileId = UUID.randomUUID();
        Profile profile = Profile.create(
            UUID.randomUUID(), // memberId
            "다른유저",
            35,
            Gender.FEMALE,
            "url",
            "소개글",
            "서울 서초구"
        );
        given(profileRepository.findById(profileId)).willReturn(Optional.of(profile));

        // when
        ProfileResponse resp = profileService.getProfileById(profileId);

        // then
        assertThat(resp.nickname()).isEqualTo("다른유저");
        assertThat(resp.age()).isEqualTo(35);
        assertThat(resp.gender()).isEqualTo(Gender.FEMALE);

        verify(profileRepository).findById(profileId);
    }

    @Test
    @DisplayName("ID로 프로필 단건 조회 실패 - 프로필 없음")
    void getProfileById_fail_notFound() {
        // given
        UUID profileId = UUID.randomUUID();
        given(profileRepository.findById(profileId)).willReturn(Optional.empty());

        // when & then
        assertThrows(NoSuchElementException.class,
            () -> profileService.getProfileById(profileId));

        verify(profileRepository).findById(profileId);
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

        assertThrows(NoSuchElementException.class,
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

    @Test
    @DisplayName("프로필 이미지 업로드 성공 - S3 URL을 imageUrl로 반영하고 ProfileResponse로 반환")
    void updateProfileImageUrl_success() {
        // given
        UUID memberId = UUID.randomUUID();

        Profile existing = Profile.create(
            memberId,
            "기존유저",
            25,
            Gender.MALE,
            "https://old.example.com/old.png",
            "소개",
            "서울 강남구"
        );

        MockMultipartFile file = new MockMultipartFile(
            "image", "avatar.png", "image/png", new byte[]{1, 2, 3}
        );

        String uploadedUrl = "https://cdn.example.com/profiles/uuid.png";

        given(profileRepository.findByMemberId(memberId)).willReturn(Optional.of(existing));
        given(s3StorageService.uploadImage(file, "profiles/")).willReturn(uploadedUrl);

        // when
        ProfileResponse resp = profileService.updateProfileImageUrl(memberId, file);

        // then
        assertThat(resp.imageUrl()).isEqualTo(uploadedUrl);
        // 엔티티가 영속 상태에서 갱신되므로, 실제 엔티티의 값도 갱신됐는지 확인(선택)
        assertThat(existing.getImageUrl()).isEqualTo(uploadedUrl);

        verify(profileRepository).findByMemberId(memberId);
        verify(s3StorageService).uploadImage(file, "profiles/");
    }

    @Test
    @DisplayName("프로필 이미지 업로드 실패 - 프로필이 존재하지 않으면 NoSuchElementException 발생")
    void updateProfileImageUrl_fail_profileNotFound() {
        // given
        UUID memberId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
            "image", "avatar.png", "image/png", new byte[]{1, 2, 3}
        );

        given(profileRepository.findByMemberId(memberId)).willReturn(Optional.empty());

        // when & then
        assertThrows(NoSuchElementException.class,
            () -> profileService.updateProfileImageUrl(memberId, file));

        verify(profileRepository).findByMemberId(memberId);
        verify(s3StorageService, never()).uploadImage(any(), anyString());
    }

    @Nested
    @DisplayName("deleteProfileImageUrl")
    class DeleteProfileImageUrl {

        @Test
        @DisplayName("이미지 URL이 있으면: S3 삭제 호출 후 엔티티 imageUrl=null 처리")
        void delete_whenImageExists() {
            UUID memberId = UUID.randomUUID();
            Profile profile = mock(Profile.class);
            when(profileRepository.findByMemberId(memberId)).thenReturn(Optional.of(profile));
            when(profile.getImageUrl())
                .thenReturn("https://bucket.s3.ap-northeast-2.amazonaws.com/profiles/a.png");

            profileService.deleteProfileImageUrl(memberId);

            verify(s3StorageService, times(1))
                .deleteImage("https://bucket.s3.ap-northeast-2.amazonaws.com/profiles/a.png");
            verify(profile, times(1))
                .updateProfile(null, null, null, null, null, null);
            verifyNoMoreInteractions(s3StorageService, profileRepository, profile);
        }

        @Test
        @DisplayName("이미지 URL이 없으면: S3 호출 없이 엔티티만 정리(멱등)")
        void delete_whenImageNotExists() {
            UUID memberId = UUID.randomUUID();
            Profile profile = mock(Profile.class);
            when(profileRepository.findByMemberId(memberId)).thenReturn(Optional.of(profile));
            when(profile.getImageUrl()).thenReturn(null);

            profileService.deleteProfileImageUrl(memberId);

            verify(s3StorageService, never()).deleteImage(anyString());
            verify(profile, times(1))
                .updateProfile(null, null, null, null, null, null);
        }

        @Test
        @DisplayName("프로필이 없으면: NoSuchElementException")
        void delete_whenProfileNotFound() {
            UUID memberId = UUID.randomUUID();
            when(profileRepository.findByMemberId(memberId)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class, () -> profileService.deleteProfileImageUrl(memberId));

            verifyNoInteractions(s3StorageService);
        }
    }
}