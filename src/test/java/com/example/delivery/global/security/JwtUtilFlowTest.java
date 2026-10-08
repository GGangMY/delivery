package com.example.delivery.global.security;

import com.example.delivery.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtUtil 전체 흐름 테스트.
 *
 * <p>로그인 → 토큰 발급 → (다음 요청) 토큰 검증 → 사용자 정보 복원
 * <pre>
 *   createToken(userId, role)  →  "Bearer xxx.yyy.zzz"
 *   "Bearer " 제거             →  "xxx.yyy.zzz"
 *   validateToken(token)       →  true
 *   getUserId(token)           →  userId
 *   getRole(token)             →  role
 * </pre>
 *
 * <p>validateToken, getUserId, getRole이 구현되기 전에는 실패한다. 구현 목표(스펙)로 쓴다.
 */
@SpringJUnitConfig(JwtUtil.class)
@TestPropertySource(properties = {
        "jwt.secret=" + JwtUtilFlowTest.SECRET,
        "jwt.expiration-ms=3600000"
})
@DisplayName("JwtUtil 흐름: 여러 사용자의 토큰 발급 → 검증 → 정보 복원")
class JwtUtilFlowTest {

    /** 원문 "delivery-test-secret-key-for-jwt-util-test!!" (44바이트). 테스트 전용. */
    static final String SECRET = "ZGVsaXZlcnktdGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdXRpbC10ZXN0ISE=";

    @Autowired
    JwtUtil jwtUtil;

    /** 테스트에 쓸 사용자들. (userId, role, 설명) */
    static Stream<Arguments> users() {
        return Stream.of(
                Arguments.of(1L, UserRole.OWNER, "사장님 1"),
                Arguments.of(2L, UserRole.CUSTOMER, "손님 1"),
                Arguments.of(3L, UserRole.CUSTOMER, "손님 2"),
                Arguments.of(100L, UserRole.OWNER, "사장님 2 (id 세 자리)"),
                Arguments.of(Long.MAX_VALUE, UserRole.CUSTOMER, "id 최댓값")
        );
    }

    @ParameterizedTest(name = "[{index}] {2}: userId={0}, role={1}")
    @MethodSource("users")
    @DisplayName("사용자별: 발급한 토큰을 검증하면 같은 userId와 role이 나온다")
    void eachUserRoundTrip(Long userId, UserRole role, String description) {
        // 1) 로그인 성공 → 토큰 발급
        String bearerToken = jwtUtil.createToken(userId, role);

        // 2) 다음 요청의 Authorization 헤더에서 "Bearer " 제거
        String token = stripBearer(bearerToken);

        // 3) 토큰 검증
        boolean valid = jwtUtil.validateToken(token);

        // 4) 토큰에서 사용자 정보 복원
        Long restoredUserId = jwtUtil.getUserId(token);
        UserRole restoredRole = jwtUtil.getRole(token);

        assertThat(bearerToken).startsWith("Bearer ");
        assertThat(valid).isTrue();
        assertThat(restoredUserId).isEqualTo(userId);
        assertThat(restoredRole).isEqualTo(role);

        System.out.printf("✅ %-20s | 발급 (%d, %s) → 복원 (%d, %s)%n",
                description, userId, role, restoredUserId, restoredRole);
    }

    @Test
    @DisplayName("전체 시나리오: 모든 사용자에게 먼저 발급하고, 나중에 한꺼번에 검증해도 서로 섞이지 않는다")
    void issueAllThenVerifyAll() {
        // given: 사용자 목록
        List<Arguments> users = users().toList();

        // when 1: 모든 사용자에게 토큰 발급 (토큰 → 원래 사용자)
        Map<String, Arguments> issued = new LinkedHashMap<>();
        for (Arguments user : users) {
            Long userId = (Long) user.get()[0];
            UserRole role = (UserRole) user.get()[1];
            issued.put(stripBearer(jwtUtil.createToken(userId, role)), user);
        }

        // then 1: 사용자마다 서로 다른 토큰이 나왔다
        assertThat(issued).hasSameSizeAs(users);

        // when 2 / then 2: 각 토큰을 검증하면 발급받은 그 사용자로 복원된다
        issued.forEach((token, user) -> {
            Long expectedUserId = (Long) user.get()[0];
            UserRole expectedRole = (UserRole) user.get()[1];

            assertThat(jwtUtil.validateToken(token)).isTrue();
            assertThat(jwtUtil.getUserId(token)).isEqualTo(expectedUserId);
            assertThat(jwtUtil.getRole(token)).isEqualTo(expectedRole);

            System.out.printf("✅ %-20s | %s...%n", user.get()[2], token.substring(0, 40));
        });
    }

    @Test
    @DisplayName("다른 사용자의 토큰으로는 내 정보가 나오지 않는다")
    void tokenBelongsOnlyToItsOwner() {
        String ownerToken = stripBearer(jwtUtil.createToken(1L, UserRole.OWNER));
        String customerToken = stripBearer(jwtUtil.createToken(2L, UserRole.CUSTOMER));

        assertThat(ownerToken).isNotEqualTo(customerToken);
        assertThat(jwtUtil.getUserId(customerToken)).isNotEqualTo(1L);
        assertThat(jwtUtil.getRole(customerToken)).isNotEqualTo(UserRole.OWNER);
    }

    @Test
    @DisplayName("검증 실패 케이스는 예외가 아니라 false로 돌아온다")
    void invalidTokensReturnFalse() {
        String validToken = stripBearer(jwtUtil.createToken(1L, UserRole.OWNER));
        String expiredToken = stripBearer(new JwtUtil(SECRET, -1000L).createToken(1L, UserRole.OWNER));

        // "Bearer "를 떼지 않은 토큰
        assertThat(jwtUtil.validateToken("Bearer " + validToken)).isFalse();
        // 서명 한 글자 변조
        assertThat(jwtUtil.validateToken(tamperSignature(validToken))).isFalse();
        // 만료된 토큰
        assertThat(jwtUtil.validateToken(expiredToken)).isFalse();
        // 형식이 아예 아닌 문자열
        assertThat(jwtUtil.validateToken("not-a-jwt")).isFalse();
        // 빈 문자열
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    // ===================== 헬퍼 =====================

    private static String stripBearer(String bearerToken) {
        return bearerToken.substring("Bearer ".length());
    }

    /**
     * 서명의 첫 글자를 다른 글자로 바꿔 서명을 깨뜨린다.
     * (마지막 글자는 Base64 남는 비트라 바꿔도 디코딩 결과가 같을 수 있어서 첫 글자를 바꾼다)
     */
    private static String tamperSignature(String token) {
        int signatureStart = token.lastIndexOf('.') + 1;
        char first = token.charAt(signatureStart);
        return token.substring(0, signatureStart) + (first == 'A' ? 'B' : 'A') + token.substring(signatureStart + 1);
    }
}
