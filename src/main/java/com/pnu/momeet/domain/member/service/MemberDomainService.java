package com.pnu.momeet.domain.member.service;

import com.pnu.momeet.domain.member.dto.request.ChangePasswordRequest;
import com.pnu.momeet.domain.member.dto.request.MemberCreateRequest;
import com.pnu.momeet.domain.member.dto.request.MemberEditRequest;
import com.pnu.momeet.domain.member.dto.response.MemberInfo;
import com.pnu.momeet.domain.member.dto.response.MemberResponse;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.service.mapper.MemberDtoMapper;
import com.pnu.momeet.domain.member.service.mapper.MemberEntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class MemberDomainService {

    private final MemberEntityService entityService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberResponse saveMember(MemberCreateRequest request) {
        var savedMember = entityService.createMember(MemberDtoMapper.toEntity(request));
        log.info("새로운 사용자 생성 완료: id={}, email={}", savedMember.getId(), savedMember.getEmail());
        return MemberEntityMapper.toDto(savedMember);
    }

    @Transactional(readOnly = true)
    public Page<MemberResponse> getAllMembersByPagination(Pageable pageable) {
        return entityService.getAllByPagination(pageable)
            .map(MemberEntityMapper::toDto);
    }

    @Transactional(readOnly = true)
    public MemberResponse getMemberById(UUID id) {
        return MemberEntityMapper.toDto(entityService.getById(id));
    }

    @Transactional(readOnly = true)
    public MemberResponse getMemberByEmail(String email) {
        var member = entityService.getByEmail(email);
        return MemberEntityMapper.toDto(member);
    }

    @Transactional(readOnly = true)
    public MemberInfo getMemberInfoById(UUID id) {
        var member = entityService.getById(id);
        return MemberEntityMapper.toMemberInfo(member);
    }

    @Transactional
    public MemberResponse updateMemberById(UUID id, MemberEditRequest request) {
        Member member = entityService.getById(id);
        member = entityService.updateMember(member, MemberDtoMapper.toConsumer(request));
        log.info("사용자 정보 수정 완료: id={}, email={}", member.getId(), member.getEmail());
        return MemberEntityMapper.toDto(member);
    }

    @Transactional
    public MemberResponse validateAndUpdatePasswordById(UUID id, ChangePasswordRequest request) {
        Member member = entityService.getById(id);
        if (!passwordEncoder.matches(request.oldPassword(), member.getPassword())) {
            log.warn("기존 비밀번호 불일치로 인한 비밀번호 변경 실패. id={}", id);
            throw new IllegalArgumentException("기존 비밀번호가 일치하지 않습니다.");
        }
        member = entityService.updateMember(member, m -> {
            m.setEnabled(false); // 비밀번호 변경 시 계정 비활성화
            m.setPassword(request.newPassword()); // 비밀번호 변경(암호화는 entityService 에서 처리)
        });
        log.info("사용자 비밀번호 변경 완료: id={}, email={}", member.getId(), member.getEmail());
        return MemberEntityMapper.toDto(member);
    }

    @Transactional
    public MemberResponse updatePasswordById(UUID id, String newPassword) {
        Member member = entityService.getById(id);
        member = entityService.updateMember(member, m -> {
            m.setEnabled(false); // 비밀번호 변경 시 계정 비활성화
            m.setPassword(newPassword); // 비밀번호 변경(암호화는 entityService 에서 처리)
        });
        log.info("관리자에 의해 사용자 비밀번호 변경 완료: id={}, email={}", member.getId(), member.getEmail());
        return MemberEntityMapper.toDto(member);
    }

    @Transactional
    public void deleteMemberById(UUID id) {
        entityService.deleteById(id);
        log.info("사용자 삭제 완료: id={}", id);
    }
}
