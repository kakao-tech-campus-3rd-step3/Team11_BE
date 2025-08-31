package com.pnu.momeet.domain.member.service;

import com.pnu.momeet.domain.common.enums.Provider;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.entity.Role;
import com.pnu.momeet.domain.member.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Member saveMember(String email, String password, List<String> roles) {
        List<Role> roleEntities = roleService.findRolesByNames(roles);
        String encodedPassword = passwordEncoder.encode(password);
        Member member = new Member(email, encodedPassword, roleEntities);
        return memberRepository.save(member);
    }

    @Transactional
    public Member saveMember(String email, Provider provider, String providerId, List<String> roles) {
        List<Role> roleEntities = roleService.findRolesByNames(roles);
        String encodedProviderId = passwordEncoder.encode(providerId);
        Member member = new Member(email, provider, encodedProviderId, roleEntities);
        return memberRepository.save(member);
    }

    public Member findMemberById(UUID id) {
        return memberRepository.findById(id).orElseThrow(
            () -> new IllegalArgumentException("해당 Id의 사용자가 존재하지 않습니다. id=" + id)
        );
    }

    public Member findMemberByEmail(String email) {
        return memberRepository.findMemberByEmail(email).orElseThrow(
            () -> new IllegalArgumentException("해당 이메일의 사용자가 존재하지 않습니다. email=" + email)
        );
    }
}
