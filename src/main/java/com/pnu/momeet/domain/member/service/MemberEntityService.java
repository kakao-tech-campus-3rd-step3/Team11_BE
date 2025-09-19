package com.pnu.momeet.domain.member.service;

import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.repository.MemberRepository;
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
public class MemberEntityService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<Member> getAllByPagination(Pageable pageable) {
        return memberRepository.findAll(pageable);
    }

    public Member getById(UUID id) {
        log.debug("특정 id의 사용자 조회 시도. id={}", id);
        var member = memberRepository.findById(id);

        if (member.isEmpty()) {
            log.warn("존재하지 않는 id의 사용자 조회 시도. id={}", id);
            throw new NoSuchElementException("해당 Id의 사용자가 존재하지 않습니다. id=" + id);
        }
        log.debug("특정 id의 사용자 조회 성공. id={}", id);
        return member.get();
    }

    @Transactional(readOnly = true)
    public Member getByEmail(String email) {
        log.debug("특정 이메일의 사용자 조회 시도. email={}", email);
        var member = memberRepository.findMemberByEmail(email);
        if (member.isEmpty()) {
            log.warn("존재하지 않는 이메일의 사용자 조회 시도. email={}", email);
            throw new NoSuchElementException("해당 이메일의 사용자가 존재하지 않습니다. email=" + email);
        }
        log.debug("특정 이메일의 사용자 조회 성공. email={}", email);
        return member.get();
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    @Transactional
    public Member createMember(Member member) {
        log.debug("사용자 생성 시도. email={}", member.getEmail());
        if (existsByEmail(member.getEmail())) {
            log.warn("이미 존재하는 이메일로 사용자 생성 시도. email={}", member.getEmail());
            throw new DuplicateKeyException("이미 존재하는 이메일입니다. email=" + member.getEmail());
        }
        member.setPassword(passwordEncoder.encode(member.getPassword()));
        var savedMember = memberRepository.save(member);

        log.debug("사용자 생성 성공. email={}", member.getEmail());
        return savedMember;
    }

    @Transactional
    public Member saveMember(Member member) {
        log.debug("사용자 정보 저장. id={}", member.getId());
        return memberRepository.save(member);
    }

    @Transactional
    public Member updateMember(Member member, Consumer<Member> updater) {
        log.debug("사용자 정보 수정 시도. id={}", member.getId());
        String oldHashedPassword = member.getPassword();

        updater.accept(member);

        if (!oldHashedPassword.equals(member.getPassword())) {
            log.debug("비밀번호 변경 감지, 비밀번호 암호화 수행. id={}", member.getId());
            member.setPassword(passwordEncoder.encode(member.getPassword()));
        }
        log.debug("사용자 정보 수정 성공. id={}", member.getId());
        return member;
    }

    @Transactional
    public void deleteById(UUID id) {
        log.debug("사용자 삭제 시도. id={}", id);
        if (!memberRepository.existsById(id)) { // 존재하지 않는 회원 삭제 방지
            log.warn("존재하지 않는 id의 사용자 삭제 시도. id={}", id);
            throw new NoSuchElementException("해당 Id의 사용자가 존재하지 않습니다. id=" + id);
        }
        memberRepository.deleteById(id);
        log.debug("사용자 삭제 성공. id={}", id);
    }
}
