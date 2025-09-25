//package com.pnu.momeet.unit.auth;
//
//
//import com.pnu.momeet.common.security.util.JwtTokenProvider;
//import com.pnu.momeet.domain.auth.dto.response.TokenResponse;
//import com.pnu.momeet.domain.auth.entity.RefreshToken;
//import com.pnu.momeet.domain.auth.repository.RefreshTokenRepository;
//import com.pnu.momeet.domain.auth.service.EmailAuthService;
//import com.pnu.momeet.domain.member.dto.response.MemberResponse;
//import com.pnu.momeet.domain.member.entity.Member;
//import com.pnu.momeet.domain.member.enums.Provider;
//import com.pnu.momeet.domain.member.enums.Role;
//import com.pnu.momeet.domain.member.service.MemberDomainService;
//import com.pnu.momeet.domain.member.service.MemberEntityService;
//import com.pnu.momeet.domain.member.service.mapper.MemberEntityMapper;
//import com.pnu.momeet.unit.BaseUnitTest;
//import io.jsonwebtoken.Claims;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Tag;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//@Tag("auth")
//@Tag("service")
//public class AuthServiceTest extends BaseUnitTest {
//
//    @Mock
//    private RefreshTokenRepository refreshTokenRepository;
//
//    @Mock
//    private JwtTokenProvider tokenProvider;
//
//    @Mock
//    private MemberEntityService memberService;
//
//    @Mock
//    private PasswordEncoder passwordEncoder;
//
//    @InjectMocks
//    private EmailAuthService authService;
//
//    @Test
//    @DisplayName("회원가입(signUp) 테스트")
//    public void testSignUp() {
//        // given
//        String email = "test@email.com";
//        String password = "pw123";
//        Member member = new Member(email, password, List.of(Role.ROLE_USER));
//        member.setId(UUID.randomUUID());
//        Member updatedMember = new Member(email, password, List.of(Role.ROLE_USER));
//
//        // when
//        Mockito.when(memberService.saveMember(Mockito.any(Member.class))).thenReturn(member);
//        Mockito.when(tokenProvider.generateAccessToken(member.getId())).thenReturn("access");
//        Mockito.when(tokenProvider.generateRefreshToken(member.getId())).thenReturn("refresh");
//        Mockito.when(refreshTokenRepository.save(Mockito.any())).thenReturn(null);
//        Mockito.when(memberService.updateMember(Mockito.any(), Mockito.any())).thenReturn(response);
//
//        // then
//        TokenResponse result = authService.signUp(email, password);
//        Assertions.assertNotNull(result);
//        Assertions.assertEquals("refresh", result.refreshToken());
//        Assertions.assertEquals("access", result.accessToken());
//    }
//
//    @Test
//    @DisplayName("로그인(login) 테스트")
//    public void testLogin() {
//        // given
//        String email = "test@email.com";
//        String password = "pw123";
//        Member member = new Member(email, password, List.of(Role.ROLE_USER));
//        member.setId(UUID.randomUUID());
//        member.setProvider(Provider.EMAIL);
//        member.setAccountNonLocked(true);
//
//        var memberResponse = MemberEntityMapper.toMemberInfo(member);
//
//        // when
//        Mockito.when(memberService.getMemberInfoByEmail(email))
//                .thenReturn(memberResponse);
//
//        Mockito.when(passwordEncoder.matches(password, member.getPassword()))
//                .thenReturn(true);
//        Mockito.when(tokenProvider.generateAccessToken(member.getId())).thenReturn("access");
//        Mockito.when(tokenProvider.generateRefreshToken(member.getId())).thenReturn("refresh");
//
//        // then
//        TokenResponse result = authService.login(email, password);
//        Assertions.assertNotNull(result);
//        Assertions.assertEquals("refresh", result.refreshToken());
//        Assertions.assertEquals("access", result.accessToken());
//    }
//
//    @Test
//    @DisplayName("로그아웃(logout) 테스트")
//    public void testLogout() {
//        // given
//        UUID memberId = UUID.randomUUID();
//        // when
//        Mockito.when(refreshTokenRepository.existsById(memberId)).thenReturn(true);
//
//        authService.logout(memberId);
//
//        // then
//        Mockito.verify(memberService).disableMemberById(memberId);
//        Mockito.verify(refreshTokenRepository).deleteById(memberId);
//    }
//
//    @Test
//    @DisplayName("토큰 리프레시(refreshTokens) 테스트")
//    public void testRefreshTokens() {
//        // given
//        String refreshToken = "refreshTokenValue";
//        UUID memberId = UUID.randomUUID();
//        RefreshToken savedToken = new RefreshToken(memberId, refreshToken);
//        Claims claims = Mockito.mock(Claims.class);
//
//        // when
//        Mockito.when(tokenProvider.getPayload(refreshToken)).thenReturn(claims);
//        Mockito.when(claims.getSubject()).thenReturn(memberId.toString());
//        Mockito.when(refreshTokenRepository.findById(memberId)).thenReturn(Optional.of(savedToken));
//        Mockito.when(tokenProvider.generateAccessToken(memberId)).thenReturn("access");
//        Mockito.when(tokenProvider.generateRefreshToken(memberId)).thenReturn("refresh");
//        Mockito.when(refreshTokenRepository.save(Mockito.any())).thenReturn(null);
//        // Member 생성자 접근 오류 해결: 테스트용 임의 Member 객체 생성
//        Member dummyMember = new Member("dummy@email.com", "dummyPw", List.of(Role.ROLE_USER));
//        dummyMember.setId(memberId);
//        MemberResponse dummyMemberResponse = MemberEntityMapper.toDto(dummyMember);
//        Mockito.when(memberService.updateMemberById(Mockito.any(), Mockito.any())).thenReturn(dummyMemberResponse);
//
//        // then
//        TokenResponse result = authService.refreshTokens(refreshToken);
//        Assertions.assertNotNull(result);
//        Assertions.assertEquals("refresh", result.refreshToken());
//        Assertions.assertEquals("access", result.accessToken());
//    }
//}
