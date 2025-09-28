package com.pnu.momeet.common.security.details;

import com.pnu.momeet.domain.member.service.MemberDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final MemberDomainService memberService;

    @Override
    public CustomUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var memberInfo = memberService.getMemberInfoById(UUID.fromString(username));
        return new CustomUserDetails(memberInfo);
    }
}
