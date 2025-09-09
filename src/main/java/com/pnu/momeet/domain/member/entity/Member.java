package com.pnu.momeet.domain.member.entity;

import com.pnu.momeet.domain.common.entity.BaseEntity;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.member.enums.Provider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@Table(
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "provider_id"})
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Column(unique = true)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    private String providerId;

    @OneToMany(mappedBy = "member",fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberRole> roles = new ArrayList<>();


    private boolean enabled = true;

    private boolean isAccountNonLocked = true;

    private LocalDateTime tokenIssuedAt;

    public Member(String email, String password, Provider provider, String providerId, Collection<Role> roles) {
        this.email = email;
        this.password = password;
        this.provider = provider;
        this.providerId = providerId;
        setRoles(roles);
    }

    public Member(String email, String password, Collection<Role> roles) {
        this(email, password, Provider.EMAIL, null, roles);
    }

    public Member(String email, Provider provider, String providerId, Collection<Role> roles) {
        this(email, null, provider, providerId, roles);
    }

    public void setRoles(Collection<Role> roles) {
        this.roles.clear();
        this.roles.addAll(roles.stream()
                .map(role -> new MemberRole(role, this))
                .toList()
        );
    }
}
