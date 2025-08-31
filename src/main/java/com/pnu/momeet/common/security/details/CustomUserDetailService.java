package com.pnu.momeet.common.security.details;

import com.pnu.momeet.domain.member.mapper.EntityMapper;
import com.pnu.momeet.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final MemberService memberService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var memberInfo = EntityMapper.toMemberInfo(memberService.findMemberById(UUID.fromString(username)));
        return new CustomUserDetails(memberInfo);
    }
}
