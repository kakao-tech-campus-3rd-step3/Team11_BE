package com.pnu.momeet.domain.profile.service.listener;

import com.pnu.momeet.common.logging.LogTags;
import com.pnu.momeet.common.service.S3StorageService;
import com.pnu.momeet.domain.profile.event.ProfileDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileImageCleanupListener {

    private final S3StorageService s3StorageService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ProfileDeletedEvent e) {
        final String imageUrl = e.getImageUrl();
        final long started = System.currentTimeMillis();

        log.info("{}, eventId={}, type={}, profileId={}, imageUrl={}",
            LogTags.HANDLE_START, e.getEventId(), e.type(), e.getProfileId(), imageUrl
        );

        if (imageUrl == null || imageUrl.isBlank()) {
            log.info("[ProfileImageCleanup] 삭제할 s3 이미지가 없습니다. (profileId={})", e.getProfileId());
            return; // 멱등
        }

        try {
            s3StorageService.deleteImage(imageUrl);
            long elapsed = System.currentTimeMillis() - started;
            log.info("{}, eventId={}, type={}, profileId={}, imageUrl={}, elapsedMs={}",
                LogTags.HANDLE_END, e.getEventId(), e.type(), e.getProfileId(), imageUrl, elapsed
            );
        } catch (Exception ex) {
            // 멱등/운영안정: 실패해도 서비스 흐름은 방해하지 않음
            log.info("[ProfileImageCleanup] s3 이미지 삭제에 실패했습니다. (profileId={}, imageUrl={}), cause={}",
                e.getProfileId(), imageUrl, ex.toString());
        }
    }
}
