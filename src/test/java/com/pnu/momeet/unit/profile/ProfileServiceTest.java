//package com.pnu.momeet.unit.profile;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.AdditionalAnswers.returnsFirstArg;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//
//import com.pnu.momeet.common.service.S3StorageService;
//import com.pnu.momeet.domain.profile.dto.request.ProfileCreateRequest;
//import com.pnu.momeet.domain.profile.dto.request.ProfileUpdateRequest;
//import com.pnu.momeet.domain.profile.dto.response.ProfileResponse;
//import com.pnu.momeet.domain.profile.entity.Profile;
//import com.pnu.momeet.domain.profile.enums.Gender;
//import com.pnu.momeet.domain.profile.repository.ProfileRepository;
//import com.pnu.momeet.domain.profile.service.ProfileService;
//import java.util.NoSuchElementException;
//import java.util.Optional;
//import java.util.UUID;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.mock.web.MockMultipartFile;
//
//@ExtendWith(MockitoExtension.class)
//class ProfileServiceTest {
//
//    @Mock
//    private ProfileRepository profileRepository;
//
//    @Mock
//    private S3StorageService s3StorageService;
//
//    @InjectMocks
//    private ProfileService profileService;
//
//    @Test
//    @DisplayName("내 프로필 조회 성공 - ProfileResponse 매핑 검증")
//    void getMyProfile_success() {
//        UUID memberId = UUID.randomUUID();
//        Profile profile = Profile.create(
//            memberId,
//            "테스트유저",
//            25,
//            Gender.MALE,
//            "https://cdn.example.com/p.png",
//            "소개글",
//            "부산 금정구"
//        );
//        given(profileRepository.findByMemberId(memberId)).willReturn(Optional.of(profile));
//
//        ProfileResponse resp = profileService.getMyProfile(memberId);
//
//        assertThat(resp.nickname()).isEqualTo("테스트유저");
//        assertThat(resp.age()).isEqualTo(25);
//        assertThat(resp.gender()).isEqualTo(Gender.MALE);
//        assertThat(resp.baseLocation()).isEqualTo("부산 금정구");
//        verify(profileRepository).findByMemberId(memberId);
//    }
//
//    @Test
//    @DisplayName("내 프로필 조회 실패 - 프로필 없음이면 NoSuchElementException 발생")
//    void getMyProfile_notFound() {
//        UUID memberId = UUID.randomUUID();
//        given(profileRepository.findByMemberId(memberId)).willReturn(Optional.empty());
//
//        assertThrows(NoSuchElementException.class,
//            () -> profileService.getMyProfile(memberId));
//        verify(profileRepository).findByMemberId(memberId);
//    }
//
//    @Test
//    @DisplayName("ID로 프로필 단건 조회 성공")
//    void getProfileById_success() {
//        // given
//        UUID profileId = UUID.randomUUID();
//        Profile profile = Profile.create(
//            UUID.randomUUID(), // memberId
//            "다른유저",
//            35,
//            Gender.FEMALE,
//            "url",
//            "소개글",
//            "서울 서초구"
//        );
//        given(profileRepository.findById(profileId)).willReturn(Optional.of(profile));
//
//        // when
//        ProfileResponse resp = profileService.getProfileById(profileId);
//
//        // then
//        assertThat(resp.nickname()).isEqualTo("다른유저");
//        assertThat(resp.age()).isEqualTo(35);
//        assertThat(resp.gender()).isEqualTo(Gender.FEMALE);
//
//        verify(profileRepository).findById(profileId);
//    }
//
//    @Test
//    @DisplayName("ID로 프로필 단건 조회 실패 - 프로필 없음")
//    void getProfileById_fail_notFound() {
//        // given
//        UUID profileId = UUID.randomUUID();
//        given(profileRepository.findById(profileId)).willReturn(Optional.empty());
//
//        // when & then
//        assertThrows(NoSuchElementException.class,
//            () -> profileService.getProfileById(profileId));
//
//        verify(profileRepository).findById(profileId);
//    }
//
//    @Nested
//    @DisplayName("프로필 생성 (createMyProfile)")
//    class CreateProfile {
//
//        @Test
//        @DisplayName("성공 - 이미지를 포함하여 프로필 생성")
//        void create_withImage_success() {
//            // given
//            UUID memberId = UUID.randomUUID();
//            MockMultipartFile imageFile = new MockMultipartFile(
//                "image",
//                "test.png",
//                "image/png",
//                new byte[]{1}
//            );
//            ProfileCreateRequest request = new ProfileCreateRequest(
//                "새유저",
//                25,
//                "MALE",
//                imageFile,
//                "소개",
//                "장소"
//            );
//
//            String fakeImageUrl = "https://s3.example.com/profiles/uuid.png";
//            given(profileRepository.existsByMemberId(memberId)).willReturn(false);
//            given(profileRepository.existsByNicknameIgnoreCase("새유저")).willReturn(false);
//            given(s3StorageService.uploadImage(imageFile, "/profiles")).willReturn(fakeImageUrl);
//            given(profileRepository.save(any(Profile.class))).will(returnsFirstArg());
//
//            // when
//            ProfileResponse resp = profileService.createMyProfile(memberId, request);
//
//            // then
//            assertThat(resp.nickname()).isEqualTo("새유저");
//            assertThat(resp.imageUrl()).isEqualTo(fakeImageUrl);
//
//            verify(s3StorageService, times(1))
//                .uploadImage(imageFile, "/profiles");
//            verify(profileRepository).save(any(Profile.class));
//        }
//
//        @Test
//        @DisplayName("성공 - 이미지 없이 프로필 생성")
//        void create_withoutImage_success() {
//            // given
//            UUID memberId = UUID.randomUUID();
//            ProfileCreateRequest request = new ProfileCreateRequest(
//                "새유저",
//                25,
//                "MALE",
//                null,
//                "소개",
//                "장소"
//            );
//
//            given(profileRepository.existsByMemberId(memberId)).willReturn(false);
//            given(profileRepository.existsByNicknameIgnoreCase("새유저")).willReturn(false);
//            given(profileRepository.save(any(Profile.class))).will(returnsFirstArg());
//
//            // when
//            ProfileResponse resp = profileService.createMyProfile(memberId, request);
//
//            // then
//            assertThat(resp.nickname()).isEqualTo("새유저");
//            assertThat(resp.imageUrl()).isNull();
//
//            verify(s3StorageService, never()).uploadImage(any(), any()); // S3 업로드 호출 안됨
//            verify(profileRepository).save(any(Profile.class));
//        }
//
//        @Test
//        @DisplayName("내 프로필 생성 실패 - 이미 프로필이 존재하면 IllegalStateException 발생")
//        void createMyProfile_fail_profileAlreadyExists() {
//            UUID memberId = UUID.randomUUID();
//            ProfileCreateRequest request = new ProfileCreateRequest(
//                "닉네임",
//                20,
//                "MALE",
//                null,
//                "소개",
//                "장소"
//            );
//
//            given(profileRepository.existsByMemberId(memberId)).willReturn(true);
//
//            assertThrows(IllegalStateException.class,
//                () -> profileService.createMyProfile(memberId, request));
//
//            verify(profileRepository).existsByMemberId(memberId);
//            verify(profileRepository, never()).existsByNicknameIgnoreCase(anyString());
//            verify(profileRepository, never()).save(any(Profile.class));
//        }
//
//        @Test
//        @DisplayName("내 프로필 생성 실패 - 이미 닉네임이 존재하면 IllegalArgumentException 발생")
//        void createMyProfile_fail_nicknameAlreadyExists() {
//            UUID memberId = UUID.randomUUID();
//            ProfileCreateRequest request = new ProfileCreateRequest(
//                "중복된닉네임",
//                20,
//                "MALE",
//                null,
//                "소개",
//                "장소"
//            );
//
//            given(profileRepository.existsByMemberId(memberId)).willReturn(false);
//            given(
//                profileRepository.existsByNicknameIgnoreCase(request.nickname().trim())).willReturn(
//                true);
//
//            assertThrows(IllegalArgumentException.class,
//                () -> profileService.createMyProfile(memberId, request));
//
//            verify(profileRepository).existsByMemberId(memberId);
//            verify(profileRepository).existsByNicknameIgnoreCase(request.nickname().trim());
//            verify(profileRepository, never()).save(any(Profile.class));
//        }
//    }
//
//    @Nested
//    @DisplayName("프로필 수정 (updateMyProfile)")
//    class UpdateProfile {
//
//        @Test
//        @DisplayName("성공 - 새 이미지로 교체")
//        void update_withNewImage_success() {
//            // given
//            UUID memberId = UUID.randomUUID();
//            Profile existingProfile = Profile.create(
//                memberId,
//                "기존유저",
//                25,
//                Gender.MALE,
//                "http://old.png",
//                "소개",
//                "장소"
//            );
//            MockMultipartFile newImageFile = new MockMultipartFile(
//                "image",
//                "new.png",
//                "image/png",
//                new byte[]{2}
//            );
//            ProfileUpdateRequest request = new ProfileUpdateRequest(
//                "수정유저",
//                30,
//                "FEMALE",
//                newImageFile,
//                "수정소개",
//                "수정장소"
//            );
//
//            String newImageUrl = "http://new.png";
//            given(profileRepository.findByMemberId(memberId)).willReturn(
//                Optional.of(existingProfile));
//            given(profileRepository.existsByNicknameIgnoreCase("수정유저")).willReturn(false);
//            given(s3StorageService.uploadImage(newImageFile, "/profiles")).willReturn(newImageUrl);
//
//            // when
//            ProfileResponse resp = profileService.updateMyProfile(memberId, request);
//
//            // then
//            assertThat(resp.nickname()).isEqualTo("수정유저");
//            assertThat(resp.imageUrl()).isEqualTo(newImageUrl);
//
//            verify(s3StorageService, times(1))
//                .deleteImage("http://old.png"); // 기존 이미지 삭제
//            verify(s3StorageService, times(1))
//                .uploadImage(newImageFile, "/profiles"); // 새 이미지 업로드
//        }
//
//        @Test
//        @DisplayName("성공 - 이미지 변경 없이 텍스트 정보만 수정")
//        void update_withoutImage_success() {
//            // given
//            UUID memberId = UUID.randomUUID();
//            Profile existingProfile = Profile.create(
//                memberId,
//                "기존유저",
//                25,
//                Gender.MALE,
//                "http://keep.png",
//                "소개",
//                "장소"
//            );
//            ProfileUpdateRequest request = new ProfileUpdateRequest(
//                "수정유저",
//                30,
//                "FEMALE",
//                null,
//                "수정소개",
//                "수정장소"
//            ); // 이미지는 null
//
//            given(profileRepository.findByMemberId(memberId)).willReturn(
//                Optional.of(existingProfile));
//            given(profileRepository.existsByNicknameIgnoreCase("수정유저")).willReturn(false);
//
//            // when
//            ProfileResponse resp = profileService.updateMyProfile(memberId, request);
//
//            // then
//            assertThat(resp.nickname()).isEqualTo("수정유저");
//            assertThat(resp.imageUrl()).isEqualTo("http://keep.png"); // 기존 이미지 URL 유지
//
//            // S3 관련 메서드가 전혀 호출되지 않았는지 검증
//            verify(s3StorageService, never()).deleteImage(any());
//            verify(s3StorageService, never()).uploadImage(any(), any());
//        }
//
//        @Test
//        @DisplayName("내 프로필 수정 실패 - 프로필이 존재하지 않으면 NoSuchElementException 발생")
//        void updateMyProfile_fail_profileNotFound() {
//            UUID memberId = UUID.randomUUID();
//            ProfileUpdateRequest request = new ProfileUpdateRequest(
//                "닉네임",
//                20,
//                "MALE",
//                null,
//                "소개",
//                "장소"
//            );
//
//            given(profileRepository.findByMemberId(memberId)).willReturn(Optional.empty());
//
//            assertThrows(NoSuchElementException.class,
//                () -> profileService.updateMyProfile(memberId, request));
//
//            verify(profileRepository).findByMemberId(memberId);
//            verify(profileRepository, never()).existsByNicknameIgnoreCase(anyString());
//        }
//
//        @Test
//        @DisplayName("내 프로필 수정 실패 - 닉네임이 중복되면 IllegalArgumentException 발생")
//        void updateMyProfile_fail_duplicateNickname() {
//            UUID memberId = UUID.randomUUID();
//            Profile existingProfile = Profile.create(
//                memberId,
//                "기존유저",
//                25,
//                Gender.MALE,
//                null,
//                "소개",
//                "장소"
//            );
//            ProfileUpdateRequest request = new ProfileUpdateRequest(
//                "중복된닉네임",
//                30,
//                "FEMALE",
//                null,
//                "소개",
//                "장소"
//            );
//
//            given(profileRepository.findByMemberId(memberId)).willReturn(
//                Optional.of(existingProfile));
//            given(
//                profileRepository.existsByNicknameIgnoreCase(request.nickname().trim())).willReturn(
//                true);
//
//            assertThrows(IllegalArgumentException.class,
//                () -> profileService.updateMyProfile(memberId, request));
//
//            verify(profileRepository).findByMemberId(memberId);
//            verify(profileRepository).existsByNicknameIgnoreCase(request.nickname().trim());
//        }
//    }
//}