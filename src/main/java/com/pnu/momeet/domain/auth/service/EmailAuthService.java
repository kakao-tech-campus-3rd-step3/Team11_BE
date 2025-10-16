package com.pnu.momeet.domain.auth.service;

import com.pnu.momeet.common.security.config.SecurityProperties;
import com.pnu.momeet.common.service.MailSenderService;
import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
import com.pnu.momeet.domain.auth.entity.VerificationCode;
import com.pnu.momeet.domain.auth.repository.VerificationCodeRepository;
import com.pnu.momeet.domain.member.enums.Provider;
import com.pnu.momeet.domain.member.entity.Member;
import com.pnu.momeet.domain.member.enums.Role;
import com.pnu.momeet.domain.member.service.MemberEntityService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class EmailAuthService {
    private final BaseAuthService authService;
    private final MemberEntityService memberService;
    private final PasswordEncoder passwordEncoder;
    private final MailSenderService mailSenderService;
    private final TemplateEngine templateEngine;
    private final VerificationCodeRepository verificationCodeRepository;
    private String baseUrl;


    public EmailAuthService(
            BaseAuthService authService,
            MemberEntityService memberService,
            PasswordEncoder passwordEncoder,
            MailSenderService mailSenderService,
            TemplateEngine templateEngine,
            VerificationCodeRepository verificationCodeRepository,
            SecurityProperties securityProperties
    ) {
        this.authService = authService;
        this.memberService = memberService;
        this.passwordEncoder = passwordEncoder;
        this.mailSenderService = mailSenderService;
        this.templateEngine = templateEngine;
        this.verificationCodeRepository = verificationCodeRepository;
        this.baseUrl =  (securityProperties.getHttps().isEnabled()) ? "https://" : "http://";
        this.baseUrl += securityProperties.getHttps().getDomain();
    }

    @Transactional
    public void signUp(String email, String password) {
        if (memberService.existsByEmail(email)) {
            log.info("회원가입 실패: 이미 존재하는 이메일 - {}", email);
            throw new DuplicateKeyException("이미 존재하는 이메일입니다.");
        }
        Member savedMember = memberService.createMember(
                new Member(email, password, List.of(Role.ROLE_USER), false)
        );
        Optional<VerificationCode> existingCode = verificationCodeRepository.findByMemberId(savedMember.getId());
        if (existingCode.isPresent()) {
            verificationCodeRepository.delete(existingCode.get());
            log.debug("기존 인증 코드 삭제: {}", email);
        }
        log.info("새 인증 코드 발급: {}", email);
        VerificationCode verificationCode = new VerificationCode(savedMember.getId());
        
        verificationCodeRepository.save(verificationCode);
        sendVerificationEmail(email, verificationCode.getCode().toString());
        log.info("인증 메일 발송 완료: {}", email);
    }

    private void sendVerificationEmail(String email, String code) {
        log.debug("인증 메일 발송 시도: {} - {}", email, code);
        Context context = new Context();
        context.setVariable("title", "MoMeet 회원가입 인증");
        String verificationUrl = baseUrl + "/auth/verify?code=" + code;
        context.setVariable("verificationUrl", verificationUrl);
        String htmlContent = templateEngine.process("mail/email-verification.html", context);
        mailSenderService.sendHtmlMail(
                email,
                "MoMeet 회원가입 인증 메일입니다.",
                htmlContent
        );
    }

    @Transactional
    public TokenResponse login(String email, String password) {
        Member member;
        try {
            member = memberService.getByEmail(email);
        } catch (NoSuchElementException e) {
            log.info("로그인 실패: 존재하지 않는 이메일 - {}", email);
            throw new AuthenticationException("존재하지 않는 이메일이거나, 잘못된 비밀번호입니다.") {};
        }
        if (member.getProvider() != Provider.EMAIL) {

            // provider는 service 별로 고정되어 있으므로, 이메일 로그인 시도 시 EMAIL이 아닌 경우는 지원하지 않는 경로로 간주
            log.warn("로그인 실패: 지원하지 않는 경로 - {} - {}", member.getProvider(), email);
            throw new AuthenticationException("지원하지 않은 경로로 로그인을 시도하였습니다.") {};
        }
        if (!member.isVerified()) {
            log.info("로그인 실패: 이메일 인증되지 않음 - {}", email);
            throw new AuthenticationException("이메일 인증이 완료되지 않았습니다. 이메일 인증 후 다시 시도해주세요.") {};
        }
        if (!passwordEncoder.matches(password, member.getPassword())) {
            log.info("로그인 실패: 비밀번호 불일치 - {}", email);
            throw new AuthenticationException("존재하지 않는 이메일이거나, 잘못된 비밀번호입니다.") {};
        }
        var response =  authService.generateTokenPair(member.getId());
        log.info("로그인 성공: {}", email);
        return response;
    }

    @Transactional
    public void verifyEmail(UUID code) {
        log.debug("이메일 인증 시도: 코드 - {}", code);
        VerificationCode verificationCode = verificationCodeRepository.findById(code)
                .orElseThrow(() -> {
                    log.debug("이메일 인증 실패: 존재하지 않는 코드 - {}", code);
                    return new IllegalArgumentException("유효하지 않은 인증 코드입니다.");
                });
        Member member = memberService.getById(verificationCode.getMemberId());
        if (member.isVerified()) {
            log.debug("이메일 인증 실패: 이미 인증된 이메일 - {}", member.getEmail());
            throw new IllegalArgumentException("이미 인증된 이메일입니다.");
        }
        if (verificationCode.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            log.debug("이메일 인증 실패: 만료된 코드 - {}", code);
            throw new IllegalArgumentException("만료된 인증 코드입니다.");
        }
        memberService.updateMember(member, m -> m.setVerified(true));
        log.info("이메일 인증 성공: {}", member.getEmail());
    }
}
