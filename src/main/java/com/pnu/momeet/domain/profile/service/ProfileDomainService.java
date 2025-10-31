package com.pnu.momeet.domain.profile.service;

import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.common.tx.AfterCommitExecutor;
import com.pnu.momeet.common.util.ImageHashUtil;
import com.pnu.momeet.domain.profile.command.ProfileChanges;
import com.pnu.momeet.domain.profile.dto.request.LocationInput;
import com.pnu.momeet.domain.profile.dto.request.ProfileCreateRequest;
import com.pnu.momeet.domain.profile.dto.request.ProfileUpdateRequest;
import com.pnu.momeet.domain.profile.dto.response.ProfileResponse;
import com.pnu.momeet.domain.profile.entity.Profile;
import com.pnu.momeet.domain.profile.enums.Gender;
import com.pnu.momeet.domain.profile.service.mapper.ProfileDtoMapper;
import com.pnu.momeet.domain.profile.service.mapper.ProfileEntityMapper;
import com.pnu.momeet.domain.sigungu.entity.Sigungu;
import com.pnu.momeet.domain.sigungu.service.SigunguEntityService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileDomainService {
    
    private static final String PROFILE_IMAGE_PREFIX = "/profiles";

    private final ProfileEntityService entityService;
    private final S3StorageService s3StorageService;
    private final SigunguEntityService sigunguService;
    private final GeometryFactory geometryFactory;
    private final ImageHashUtil imageHashUtil;
    private final AfterCommitExecutor afterCommitExecutor;

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(UUID memberId) {
        return ProfileEntityMapper.toResponseDto(entityService.getByMemberId(memberId));
    }

    @Deprecated
    @Transactional(readOnly = true)
    public Profile getProfileEntityByMemberId(UUID memberId) {
        return entityService.getByMemberId(memberId);
    }

    @Deprecated
    @Transactional(readOnly = true)
    public Profile getProfileEntityByProfileId(UUID profileId) {
        return entityService.getById(profileId);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfileByMemberId(UUID memberId) {
        return ProfileEntityMapper.toResponseDto(entityService.getByMemberId(memberId));
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfileById(UUID profileId) {
        return ProfileEntityMapper.toResponseDto(entityService.getById(profileId));
    }

    private boolean has(String s){
        return s != null && !s.isBlank();
    }

    private Sigungu resolveSigungu(LocationInput in) {
        // 1) PK
        if (in.baseLocationId() != null) {
            return sigunguService.getById(in.baseLocationId());
        }
        // 2) 이름
        if (has(in.sidoName()) && has(in.sigunguName())) {
            return sigunguService.getBySidoNameAndSigunguName(in.sidoName(), in.sigunguName());
        }
        // 좌표 매칭
        if (in.latitude() != null && in.longitude() != null) {
            Point point = geometryFactory.createPoint(new Coordinate(in.longitude(), in.latitude()));
            return sigunguService.getByPointIn(point);
        }
        throw new IllegalArgumentException("지역 입력이 필요합니다.");
    }

    @Transactional
    public ProfileResponse createMyProfile(UUID memberId, ProfileCreateRequest request) {
        if (entityService.existsByMemberId(memberId)) {
            log.warn("이미 존재하는 프로필로 프로필 생성 시도. memberId={}", memberId);
            throw new IllegalStateException("프로필이 이미 존재합니다.");
        }
        if (entityService.existsByNicknameIgnoreCase(request.nickname().trim())) {
            log.info("이미 존재하는 닉네임으로 프로필 생성 시도. memberId={}, nickname={}", memberId, request.nickname());
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
        }
        String profileImageUrl = null;
        if (request.image() != null) {
            profileImageUrl = s3StorageService.uploadImage(request.image(), PROFILE_IMAGE_PREFIX);
        }
        Sigungu sigungu = resolveSigungu(request.baseLocation());
        Profile newProfile = ProfileDtoMapper.toEntity(request, sigungu, profileImageUrl, memberId);
        newProfile = entityService.createProfile(newProfile);
        log.info("새로운 프로필 생성 성공. id={}, memberId={}", newProfile.getId(), memberId);
        return ProfileEntityMapper.toResponseDto(newProfile);
    }

    @Transactional
    public ProfileResponse updateMyProfile(UUID memberId, ProfileUpdateRequest request) {
        log.debug("프로필 수정 시도. memberId={}", memberId);
        Profile profile = entityService.getByMemberId(memberId);

        ProfileChanges changes = change(request, profile);

        if (changes.nothingToDo()) {
            log.info("프로필 수정 건너뜀(변경 없음). id={}, memberId={}", profile.getId(), memberId);
            return ProfileEntityMapper.toResponseDto(profile);
        }

        // 닉네임은 '실제 변경 시'에만 + 자기자신 제외 중복 검사
        if (changes.nickChanged() &&
            entityService.existsByNicknameIgnoreCaseAndIdNot(changes.nickname(), profile.getId())) {
            log.info("이미 존재하는 닉네임으로 프로필 수정 시도. id={}, nickname={}", profile.getId(), changes.nickname());
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
        }

        // 텍스트 필드 반영 (null은 무시)
        if (changes.textChanged()) {
            profile = entityService.updateProfile(profile, p -> p.updateProfile(
                changes.nickChanged()   ? changes.nickname()     : null,
                changes.ageChanged()    ? changes.age()          : null,
                changes.genderChanged() ? changes.gender()       : null,
                changes.descChanged()   ? changes.description()  : null,
                changes.baseChanged()   ? changes.baseLocation() : null
            ));
        }

        // 이미지 교체: 동일 파일은 스킵, 다르면 업로드 + 커밋 후 기존 삭제
        if (changes.hasImagePart() && changes.imageChanged()) {
            String oldUrl = profile.getImageUrl();
            String newUrl = s3StorageService.uploadImage(request.image(), PROFILE_IMAGE_PREFIX);
            profile.updateImage(newUrl, changes.imageHash());

            // 트랜잭션 있으면 커밋 후, 없으면 즉시 실행
            afterCommitExecutor.run(() -> {
                if (oldUrl != null) s3StorageService.deleteImage(oldUrl);
            });
            log.info("프로필 이미지 변경 성공. id={}, newUrl={}", profile.getId(), newUrl);
        }

        log.info("프로필 수정 성공. id={}, memberId={}", profile.getId(), memberId);
        return ProfileEntityMapper.toResponseDto(profile);
    }

    private ProfileChanges change(ProfileUpdateRequest request, Profile profile) {
        // 정규화
        String inNickname = request.nickname() == null ? null : request.nickname().trim();
        Integer inAge = request.age();
        Gender inGender = request.gender() != null ? Gender.valueOf(request.gender().toUpperCase()) : null;
        String inDesc = request.description();
        Sigungu inSigungu = (request.baseLocation() == null) ? null : resolveSigungu(request.baseLocation());

        // 텍스트 변경 감지
        boolean nickChanged = inNickname != null && !inNickname.equalsIgnoreCase(profile.getNickname());
        boolean ageChanged = inAge != null && !inAge.equals(profile.getAge());
        boolean genderChanged = inGender != null && inGender != profile.getGender();
        boolean descChanged = inDesc != null && !inDesc.equals(profile.getDescription());
        boolean baseChanged = inSigungu != null && !inSigungu.equals(profile.getBaseLocation());

        // 이미지 변경 감지
        boolean hasImagePart = request.image() != null && !request.image().isEmpty();
        String incomingHash = null;
        boolean imageChanged = false;
        if (hasImagePart) {
            incomingHash = imageHashUtil.sha256Hex(request.image());
            imageChanged = incomingHash != null && !incomingHash.equals(profile.getImageHash());
        }

        return new ProfileChanges(
            inNickname, nickChanged,
            inAge, ageChanged,
            inGender, genderChanged,
            inDesc, descChanged,
            inSigungu, baseChanged,
            hasImagePart, incomingHash, imageChanged
        );
    }
}
