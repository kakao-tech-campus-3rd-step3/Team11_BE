package com.pnu.momeet.common.service;

import com.pnu.momeet.common.exception.StorageException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    // CloudFront(or 도메인) 베이스 URL
    @Value("${frontend.url}")
    private String cdnBaseUrl;

    public String uploadImage(MultipartFile multipartFile, String prefix) {
        return uploadImageToS3(multipartFile, prefix);
    }

    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return; // 멱등
        String key = getKeyFromImageUrl(imageUrl);
        if (key == null || key.isBlank()) return; // 파싱 실패 시 안전 탈출
        if (!StringUtils.hasText(key)) return;
        DeleteObjectRequest req = DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();
        s3Client.deleteObject(req);
    }

    private String uploadImageToS3(MultipartFile multipartFile, String prefix) {
        String originalFilename = multipartFile.getOriginalFilename();
        String extension = safeExt(originalFilename);
        String key = buildKey(prefix, extension);

        try (InputStream inputStream = multipartFile.getInputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(toMime(extension))
                .contentLength(multipartFile.getSize())
                .cacheControl("public, max-age=31536000, immutable")
                .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, multipartFile.getSize()));
        } catch (software.amazon.awssdk.services.s3.model.S3Exception s3e) {
            log.error("S3Exception code={} requestId={} msg={}",
                s3e.awsErrorDetails() != null ? s3e.awsErrorDetails().errorCode() : "N/A",
                s3e.requestId(), s3e.getMessage());
            throw new StorageException("파일 저장 중 오류가 발생했습니다.");
        } catch (IOException ioe) {
            log.error("I/O error while uploading to S3", ioe);
            throw new StorageException("파일 저장 중 오류가 발생했습니다.");
        }

        // CloudFront URL 반환
        return cdnBaseUrl.endsWith("/")
            ? cdnBaseUrl + key
            : cdnBaseUrl + "/" + key;
    }

    private String getKeyFromImageUrl(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (!StringUtils.hasText(path)) return null;
            String key = path.startsWith("/") ? path.substring(1) : path;
            return StringUtils.hasText(key) ? key : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String toMime(String ext) {
        return switch (ext.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> throw new IllegalArgumentException("지원하지 않는 확장자입니다.");
        };
    }

    private String safeExt(String name) {
        int dot = name.lastIndexOf('.');
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String buildKey(String prefix, String extension) {
        if (prefix == null) prefix = "";
        prefix = prefix.replace('\\','/');
        prefix = prefix.replaceAll("/+", "/");
        if (prefix.startsWith("/")) {
            prefix = prefix.substring(1); // 맨 앞 "/" 제거
        }
        if (!prefix.isEmpty() && !prefix.endsWith("/")) prefix += "/";
        return prefix + UUID.randomUUID() + "." + extension;
    }
}
