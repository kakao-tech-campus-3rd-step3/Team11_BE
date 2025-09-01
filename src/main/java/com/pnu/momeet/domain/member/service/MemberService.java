package com.pnu.momeet.domain.member.service;

import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Member saveMember(Member member) {
        String encodedPassword = passwordEncoder.encode(member.getPassword());
        member.setPassword(encodedPassword);
        return memberRepository.save(member);
    }

    public Page<Member> findAllMembers(Pageable pageable) {
        return memberRepository.findAllBy(pageable);
    }

    public Member findMemberById(UUID id) {
        return memberRepository.findById(id).orElseThrow(
            () -> new NoSuchElementException("해당 Id의 사용자가 존재하지 않습니다. id=" + id)
        );
    }

    public Member findMemberByEmail(String email) {
        return memberRepository.findMemberByEmail(email).orElseThrow(
            () -> new NoSuchElementException("해당 이메일의 사용자가 존재하지 않습니다. email=" + email)
        );
    }

    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    public Member disableMemberById(UUID id) {
        Member member = findMemberById(id);
        member.setEnabled(false);
        return memberRepository.save(member);
    }

    @Transactional
    public Member updateMemberById(UUID id, Consumer<Member> updater) {
        Member member = findMemberById(id);
        member.setEnabled(false); // 사용자 정보 변경 전 비활성화
        updater.accept(member);
        return memberRepository.save(member);
    }

    @Transactional
    public Member updatePasswordById(UUID id, String oldPassword, String newPassword) {
        Member member = disableMemberById(id); // 사용자 정보 변경 전 비활성화

        if (!passwordEncoder.matches(oldPassword, member.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호가 일치하지 않습니다.");
        }
        member.setPassword(passwordEncoder.encode(newPassword));
        return memberRepository.save(member);
    }

    @Transactional
    public void deleteMemberById(UUID id) {
        findMemberById(id); // 존재하지 않는 회원 삭제 방지
        memberRepository.deleteById(id);
    }
}
