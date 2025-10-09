package com.pnu.momeet.domain.auth.dto.request;

public record KakaoTokenRequest(
        String grant_type,
        String client_id,
        String client_secret,
        String redirect_uri,
        String code
) {
}
