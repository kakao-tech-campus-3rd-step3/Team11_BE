package com.pnu.momeet.common.service;

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

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of("jpg","jpeg","png","gif","webp");
    private final S3Client s3Client;
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String uploadImage(MultipartFile multipartFile, String prefix) {
        validate(multipartFile, multipartFile.getOriginalFilename());
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
                // TODO: 운영 전환 시 PUBLIC_READ 제거하고 버킷 정책/CloudFront/프리사인드 URL 검토
                .acl(ObjectCannedACL.PUBLIC_READ)
                .contentType(toMime(extension))
                .contentLength(multipartFile.getSize())
                .cacheControl("public, max-age=31536000, immutable")
                .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, multipartFile.getSize()));
        } catch (software.amazon.awssdk.services.s3.model.S3Exception s3e) {
            log.error("S3Exception code={} requestId={} msg={}",
                s3e.awsErrorDetails() != null ? s3e.awsErrorDetails().errorCode() : "N/A",
                s3e.requestId(), s3e.getMessage());
            throw new IllegalStateException("파일 저장 중 오류가 발생했습니다.");
        } catch (IOException ioe) {
            log.error("I/O error while uploading to S3", ioe);
            throw new IllegalStateException("파일 저장 중 오류가 발생했습니다.");
        }

        return s3Client.utilities().getUrl(url -> url.bucket(bucket).key(key)).toString();
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
            default -> "application/octet-stream";
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

    private void validate(MultipartFile multipartFile, String fileName) {
        validateFileSize(multipartFile);
        validateFile(fileName);
        validateContentType(multipartFile);
        validateImageContent(multipartFile);
    }

    private void validateFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("파일 이름은 비어 있을 수 없습니다");
        }

        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf < 0) {
            throw new IllegalArgumentException("확장자가 존재해야 합니다.");
        }

        String extension = safeExt(fileName);
        if (!ALLOWED_EXT.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않은 확장자입니다.");
        }
    }

    private void validateContentType(MultipartFile file) {
        String ct = file.getContentType();
        if (ct == null || !ct.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("이미지 Content-Type이 아닙니다.");
        }
    }

    private void validateFileSize(MultipartFile multipartFile) {
        long size = multipartFile.getSize();
        if (size <= 0) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }
        if (size > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기가 5MB를 초과했습니다.");
        }
    }

    private void validateImageContent(MultipartFile multipartFile) {
        try (InputStream in = multipartFile.getInputStream()) {
            if (ImageIO.read(in) == null) {
                throw new IllegalArgumentException("이미지 파일이 아닙니다.");
            }
        } catch (IOException e) {
            throw new IllegalStateException("이미지 판별 중 오류가 발생했습니다.");
        }
    }
}
