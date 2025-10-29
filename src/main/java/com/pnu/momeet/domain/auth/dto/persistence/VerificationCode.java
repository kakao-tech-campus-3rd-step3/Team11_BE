package com.pnu.momeet.domain.auth.dto.persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VerificationCode(
        String body,
        String memberId
) {
    public static VerificationCode generate(UUID memberId) {
        String body = UUID.randomUUID().toString();
        return new VerificationCode(body, memberId.toString());
    }

    @JsonIgnore
    public UUID getMemberUUID() {
        return UUID.fromString(memberId);
    }
}
