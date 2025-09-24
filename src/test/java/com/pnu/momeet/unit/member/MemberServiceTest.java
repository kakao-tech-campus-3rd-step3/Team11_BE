//package com.pnu.momeet.unit.member;
//
//import com.pnu.momeet.domain.member.dto.response.MemberInfo;
//import com.pnu.momeet.domain.member.dto.response.MemberResponse;
//import com.pnu.momeet.domain.member.entity.Member;
//import com.pnu.momeet.domain.member.enums.Role;
//import com.pnu.momeet.domain.member.repository.MemberRepository;
//import com.pnu.momeet.domain.member.service.MemberDomainService;
//import com.pnu.momeet.unit.BaseUnitTest;
//import org.junit.jupiter.api.*;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.util.List;
//import java.util.UUID;
//
//@Tag("member")
//@Tag("service")
//public class MemberServiceTest extends BaseUnitTest {
//
//    @Mock
//    private PasswordEncoder passwordEncoder;
//
//    @Mock
//    private MemberRepository memberRepository;
//
//    @InjectMocks
//    private MemberDomainService memberService;
//
//    @Test
//    @DisplayName("멤버 저장 테스트")
//    public void testSaveMember() {
//        // given
//        Member newMember = new Member("test@test.com", "password123", List.of(Role.ROLE_USER));
//
//        // when
//        Mockito.when(passwordEncoder.encode(Mockito.anyString()))
//                .thenAnswer(invocation -> "encoded_" + invocation.getArgument(0));
//        Mockito.when(memberRepository.existsByEmail(newMember.getEmail()))
//            .thenReturn(false);
//        Mockito.when(memberRepository.save(Mockito.any(Member.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        MemberResponse savedMember = memberService.saveMember(newMember);
//
//        // then
//        Assertions.assertNotNull(savedMember);
//        Mockito.verify(memberRepository).save(newMember);
//    }
//
//    @Test
//    @DisplayName("멤버 정보 수정 테스트")
//    public void testUpdateMemberById() {
//        // given
//        Member originMember = new Member("test@test.com", "password123", List.of(Role.ROLE_USER));
//        originMember.setId(UUID.randomUUID());
//        UUID memberId = originMember.getId();
//        originMember.setEnabled(true);
//
//
//        Mockito.when(memberRepository.findById(memberId))
//                .thenReturn(java.util.Optional.of(originMember));
//        Mockito.when(memberRepository.save(Mockito.any(Member.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        // when: 닉네임 변경
//        MemberResponse updated = memberService.updateMemberById(memberId, m -> m.setAccountNonLocked(false));
//
//        // then
//        Assertions.assertFalse(updated.isAccountNonLocked()); // 수정된 값
//        Mockito.verify(memberRepository).save(originMember);
//    }
//
//    @Test
//    @DisplayName("일반 사용자 비밀번호 변경 테스트 (기존 비밀번호 검증)")
//    public void testValidateAndUpdatePasswordById() {
//        // given
//        UUID memberId = UUID.randomUUID();
//        Member member = new Member("user@test.com", "encoded_oldpass", List.of(Role.ROLE_USER));
//        member.setId(memberId);
//        member.setEnabled(true);
//
//        Mockito.when(memberRepository.findById(memberId))
//                .thenReturn(java.util.Optional.of(member));
//        Mockito.when(memberRepository.save(Mockito.any(Member.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//        Mockito.when(passwordEncoder.matches("oldpass", "encoded_oldpass")).thenReturn(true);
//        Mockito.when(passwordEncoder.encode("newpass")).thenReturn("encoded_newpass");
//
//        // when
//        memberService.validateAndUpdatePasswordById(memberId, "oldpass", "newpass");
//
//        // then
//        Mockito.verify(memberRepository).save(member);
//    }
//
//    @Test
//    @DisplayName("관리자 비밀번호 변경 테스트 (기존 비밀번호 검증 없이)")
//    public void testUpdatePasswordById() {
//        // given
//        UUID memberId = UUID.randomUUID();
//        Member member = new Member("admin@test.com", "encoded_adminpass", List.of(Role.ROLE_ADMIN));
//        member.setId(memberId);
//        member.setEnabled(true);
//
//        Mockito.when(memberRepository.findById(memberId))
//                .thenReturn(java.util.Optional.of(member));
//        Mockito.when(memberRepository.save(Mockito.any(Member.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//        Mockito.when(passwordEncoder.encode("newadminpass")).thenReturn("encoded_newadminpass");
//
//        // when
//        memberService.updatePasswordById(memberId, "newadminpass");
//        MemberInfo info = memberService.getMemberInfoById(memberId);
//
//        // then
//        Assertions.assertFalse(info.enabled()); // 변경 전 비활성화
//        Assertions.assertEquals("encoded_newadminpass", info.password()); // 변경된 비밀번호
//        Mockito.verify(memberRepository).save(member);
//    }
//}
