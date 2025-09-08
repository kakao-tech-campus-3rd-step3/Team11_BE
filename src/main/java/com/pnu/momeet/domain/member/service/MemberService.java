package com.pnu.momeet.domain.member.service;

import com.pnu.momeet.domain.member.dto.response.MemberInfo;
import com.pnu.momeet.domain.member.dto.response.MemberResponse;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.repository.MemberRepository;
import com.pnu.momeet.domain.member.service.mapper.MemberEntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Consumer;


@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    private Member findMemberByIdInternal(UUID id) {
        return memberRepository.findById(id).orElseThrow(
            () -> new NoSuchElementException("해당 Id의 사용자가 존재하지 않습니다. id=" + id)
        );
    }

    @Transactional
    public MemberResponse saveMember(Member member) {
        if (memberRepository.existsByEmail(member.getEmail())) {
            throw new DuplicateKeyException("이미 가입한 이메일입니다.");
        }
        String encodedPassword = passwordEncoder.encode(member.getPassword());
        member.setPassword(encodedPassword);
        return MemberEntityMapper.toDto(memberRepository.save(member));
    }

    @Transactional(readOnly = true)
    public Page<MemberResponse> findAllMembers(Pageable pageable) {
        return memberRepository.findAll(pageable)
            .map(MemberEntityMapper::toDto);
    }

    @Transactional(readOnly = true)
    public MemberResponse findMemberById(UUID id) {
        return MemberEntityMapper.toDto(findMemberByIdInternal(id));
    }

    @Transactional(readOnly = true)
    public MemberResponse findMemberByEmail(String email) {
        var member =  memberRepository.findMemberByEmail(email).orElseThrow(
            () -> new NoSuchElementException("해당 이메일의 사용자가 존재하지 않습니다. email=" + email)
        );
        return MemberEntityMapper.toDto(member);
    }

    @Transactional(readOnly = true)
    public MemberInfo findMemberInfoById(UUID id) {
        var member = findMemberByIdInternal(id);
        return MemberEntityMapper.toMemberInfo(member);
    }

    @Transactional(readOnly = true)
    public MemberInfo findMemberInfoByEmail(String email) {
        var member = memberRepository.findMemberByEmail(email).orElseThrow(
            () -> new NoSuchElementException("해당 이메일의 사용자가 존재하지 않습니다. email=" + email)
        );
        return MemberEntityMapper.toMemberInfo(member);
    }

    @Transactional
    public void disableMemberById(UUID id) {
        Member member = findMemberByIdInternal(id);
        member.setEnabled(false);
        memberRepository.save(member);
    }

    @Transactional
    public MemberResponse updateMemberById(UUID id, Consumer<Member> updater) {
        Member member = findMemberByIdInternal(id);
        member.setEnabled(false); // 사용자 정보 변경 전 비활성화
        updater.accept(member);
        return MemberEntityMapper.toDto(memberRepository.save(member));
    }

    @Transactional
    public MemberResponse validateAndUpdatePasswordById(UUID id, String oldPassword, String newPassword) {
        Member member = findMemberByIdInternal(id);
        member.setEnabled(false); // 비밀번호 변경 전 비활성화

        if (!passwordEncoder.matches(oldPassword, member.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호가 일치하지 않습니다.");
        }
        member.setPassword(passwordEncoder.encode(newPassword));
        return MemberEntityMapper.toDto(memberRepository.save(member));
    }

    @Transactional
    public MemberResponse updatePasswordById(UUID id, String newPassword) {
        Member member = findMemberByIdInternal(id);
        member.setEnabled(false); // 비밀번호 변경 전 비활성화
        member.setPassword(passwordEncoder.encode(newPassword));
        return MemberEntityMapper.toDto(memberRepository.save(member));
    }

    @Transactional
    public void deleteMemberById(UUID id) {
        findMemberByIdInternal(id); // 존재하지 않는 회원 삭제 방지
        memberRepository.deleteById(id);
    }
}
