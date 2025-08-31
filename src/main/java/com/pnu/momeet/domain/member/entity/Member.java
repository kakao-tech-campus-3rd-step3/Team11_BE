package com.pnu.momeet.domain.member.entity;

import com.pnu.momeet.domain.common.entity.BaseEntity;
import com.pnu.momeet.domain.common.enums.Provider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "provider_id"})
    }
)
public class Member extends BaseEntity {

    @Column(unique = true)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    private String providerId;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "member_role",
        joinColumns = @JoinColumn(name = "member_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<Role> roles = new ArrayList<>();

    private boolean enabled = true;

    private boolean accountNonExpired = true;

    private boolean credentialsNonExpired = true;

    private boolean accountNonLocked = true;

    protected Member() {

    }

    public Member(String email, String password, Provider provider, String providerId, List<Role> roles) {
        this.email = email;
        this.password = password;
        this.provider = provider;
        this.providerId = providerId;
        setRoles(roles);
    }

    public Member(String email, String password, List<Role> roles) {
        this(email, password, Provider.EMAIL, null, roles);
    }

    public Member(String email, Provider provider, String providerId, List<Role> roles) {
        this(email, null, provider, providerId, roles);
    }

    public void setRoles(List<Role> roles) {
        this.roles.clear();
        this.roles.addAll(roles);
    }
}
