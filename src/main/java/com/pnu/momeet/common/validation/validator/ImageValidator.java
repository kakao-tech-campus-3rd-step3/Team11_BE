package com.pnu.momeet.common.validation.validator;

import com.pnu.momeet.common.validation.annotation.ValidImage;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

public class ImageValidator implements ConstraintValidator<ValidImage, MultipartFile> {

    @Value("${spring.servlet.multipart.max-file-size}")
    private static long MAX_FILE_SIZE;
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return true;
        }

        // 1. 파일 크기 검증
        if (file.getSize() > MAX_FILE_SIZE) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("파일 크기는 5MB를 초과할 수 없습니다.")
                .addConstraintViolation();
            return false;
        }

        // 2. 파일 이름 및 확장자 검증
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank() || fileName.lastIndexOf(".") < 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("파일 이름이 유효하지 않습니다.")
                .addConstraintViolation();
            return false;
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXT.contains(extension)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("허용되지 않은 파일 확장자입니다. (허용: " + ALLOWED_EXT + ")")
                .addConstraintViolation();
            return false;
        }

        // 3. Content-Type 검증
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("파일의 Content-Type이 이미지가 아닙니다.")
                .addConstraintViolation();
            return false;
        }

        // 4. 실제 이미지 데이터인지 검증
        try (InputStream in = file.getInputStream()) {
            if (ImageIO.read(in) == null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("실제 이미지 파일이 아닙니다.")
                    .addConstraintViolation();
                return false;
            }
        } catch (IOException e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("파일을 읽는 중 오류가 발생했습니다.")
                .addConstraintViolation();
            return false;
        }

        return true;
    }
}
