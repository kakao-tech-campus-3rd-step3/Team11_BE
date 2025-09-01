package com.pnu.momeet.domain.member.entity;

import com.pnu.momeet.domain.member.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
public class MemberRole {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private Role name;

    @CreatedDate
    LocalDateTime grantedAt;

    protected MemberRole() {

    }


    public MemberRole(Role name) {
        this.name = name;
    }
}
