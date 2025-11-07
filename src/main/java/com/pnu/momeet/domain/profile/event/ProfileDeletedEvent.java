package com.pnu.momeet.domain.profile.event;

import com.pnu.momeet.common.event.DomainEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;

@Getter
public final class ProfileDeletedEvent extends DomainEvent {

    private final UUID profileId;
    private final String imageUrl;

    public ProfileDeletedEvent(UUID profileId, String imageUrl) {
        this.profileId = profileId;
        this.imageUrl = imageUrl;
    }

    @Override
    public Map<String, Object> logInfo() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>(super.logInfo());
        m.put("profileId", profileId);
        m.put("imageUrl", imageUrl);
        return m;
    }
}
