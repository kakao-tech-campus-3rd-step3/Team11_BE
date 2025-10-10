package com.pnu.momeet.domain.auth.service;

import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.member.enums.Provider;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.member.service.MemberEntityService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAuthService {
    private final MemberEntityService memberService;
    private final PasswordEncoder passwordEncoder;
    private final DefaultAuthService authService;

    @Transactional
    public TokenResponse signUp(String email, String password) {
        Member savedMember = memberService.createMember(
                new Member(email, password, List.of(Role.ROLE_USER))
        );
        var response =  authService.generateTokenPair(savedMember.getId());
        log.info("회원가입 성공: {}", email);
        return response;
    }

    @Transactional
    public TokenResponse login(String email, String password) {
        Member member;
        try {
            member = memberService.getByEmail(email);
        } catch (NoSuchElementException e) {
            log.info("로그인 실패: 존재하지 않는 이메일 - {}", email);
            throw new AuthenticationException("존재하지 않는 이메일이거나, 잘못된 비밀번호입니다.") {};
        }
        if (member.getProvider() != Provider.EMAIL) {
            // provider는 service 별로 고정되어 있으므로, 이메일 로그인 시도 시 EMAIL이 아닌 경우는 지원하지 않는 경로로 간주
            log.warn("로그인 실패: 지원하지 않는 경로 - {} - {}", member.getProvider(), email);
            throw new AuthenticationException("지원하지 않은 경로로 로그인을 시도하였습니다.") {};
        }
        if (!passwordEncoder.matches(password, member.getPassword())) {
            log.info("로그인 실패: 비밀번호 불일치 - {}", email);
            throw new AuthenticationException("존재하지 않는 이메일이거나, 잘못된 비밀번호입니다.") {};
        }
        var response =  authService.generateTokenPair(member.getId());
        log.info("로그인 성공: {}", email);
        return response;
    }

}
