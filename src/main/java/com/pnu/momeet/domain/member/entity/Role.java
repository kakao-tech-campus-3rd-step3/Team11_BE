package com.pnu.momeet.domain.member.entity;

import com.pnu.momeet.domain.member.enums.MemberRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    @Enumerated(EnumType.STRING)
    private MemberRole name;

    protected Role()  {

    }
}
