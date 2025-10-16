package com.pnu.momeet.domain.auth.event;

import com.pnu.momeet.common.event.DomainEvent;
import lombok.Getter;

import java.util.UUID;

@Getter
public class SendVerificationEmailEvent extends DomainEvent {
    private final String email;
    private final String code;

    public SendVerificationEmailEvent(String email, UUID code) {
        super();
        this.email = email;
        this.code = code.toString();
    }
}
