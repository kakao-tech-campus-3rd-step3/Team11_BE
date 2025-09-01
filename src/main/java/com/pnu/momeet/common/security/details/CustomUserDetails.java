package com.pnu.momeet.common.security.details;

import com.pnu.momeet.domain.member.dto.MemberInfo;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;

public class CustomUserDetails implements UserDetails {

    private final MemberInfo memberInfo;

    public CustomUserDetails(MemberInfo memberInfo) {
        this.memberInfo = memberInfo;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return memberInfo.roles()
                .stream()
                .map(role -> (GrantedAuthority) () -> role)
                .toList();
    }

    @Override
    public String getPassword() {
        return switch (memberInfo.provider()) {
            case EMAIL -> memberInfo.password();
            case GOOGLE, KAKAO -> memberInfo.providerId();
        };
    }

    @Override
    public String getUsername() {
        return memberInfo.id().toString();
    }

    @Override
    public boolean isEnabled() {
        return memberInfo.enabled();
    }

    @Override
    public boolean isAccountNonLocked() {
        return memberInfo.isAccountNonLocked();
    }

    public LocalDateTime getTokenIssuedAt() {
        return memberInfo.tokenIssuedAt();
    }
}
