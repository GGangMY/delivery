package com.example.delivery;

import com.example.delivery.global.security.JwtUtil;
import com.example.delivery.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    // 테스트 전용 키 (원문 44바이트). 실제 JWT_SECRET과 다른 값
    static final String SECRET = "ZGVsaXZlcnktdGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdXRpbC10ZXN0ISE=";
    static final String OTHER_SECRET = "YW5vdGhlci1zZWNyZXQta2V5LWF0dGFja2VyLW1hZGUtaXQtMzJieXRlcys=";
    static final long EXPIRATION_MS = 3_600_000L;

    final JwtUtil jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);

    @ParameterizedTest(name = "{0} ({1})")
    @CsvSource({"owner1, OWNER", "cust1, CUSTOMER"})
    @DisplayName("발급한 토큰을 검증하면 같은 아이디와 역할이 나온다")
    void createAndRead(String username, UserRole role) {
        String token = jwtUtil.createToken(username, role);

        assertThat(token).doesNotStartWith("Bearer ");
        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.getUsername(token)).isEqualTo(username);
        assertThat(jwtUtil.getRole(token)).isEqualTo(role);
    }

    @Test
    @DisplayName("토큰에는 아이디, 역할, 발급·만료 시각만 담긴다")
    void payloadContents() {
        String payload = decodePayload(jwtUtil.createToken("owner1", UserRole.OWNER));

        assertThat(payload).contains("\"sub\":\"owner1\"", "\"role\":\"OWNER\"", "\"iat\":", "\"exp\":");
        assertThat(payload).doesNotContain("password");
    }

    @ParameterizedTest(name = "[{0}] → {1}")
    @CsvSource(value = {
            "Bearer abc.def.ghi, abc.def.ghi",
            "abc.def.ghi, NULL",
            "Basic abc, NULL",
            "NULL, NULL"
    }, nullValues = "NULL")
    @DisplayName("Authorization 헤더에서 Bearer 접두사를 뗀 토큰만 꺼낸다")
    void resolveToken(String header, String expected) {
        assertThat(jwtUtil.resolveToken(header)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTokens")
    @DisplayName("잘못된 토큰은 예외 없이 false")
    void invalidTokenReturnsFalse(String description, String token) {
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    static Stream<Arguments> invalidTokens() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);
        String[] parts = jwtUtil.createToken("cust1", UserRole.CUSTOMER).split("\\.");

        // 손님 토큰의 payload만 사장님으로 바꾸고 서명은 그대로
        String forgedPayload = encode(decode(parts[1])
                .replace("\"sub\":\"cust1\"", "\"sub\":\"owner1\"")
                .replace("\"role\":\"CUSTOMER\"", "\"role\":\"OWNER\""));

        return Stream.of(
                Arguments.of("만료된 토큰", new JwtUtil(SECRET, -1000L).createToken("cust1", UserRole.CUSTOMER)),
                Arguments.of("다른 키로 서명", new JwtUtil(OTHER_SECRET, EXPIRATION_MS).createToken("cust1", UserRole.CUSTOMER)),
                Arguments.of("payload 위조 (손님 → 사장님)", parts[0] + "." + forgedPayload + "." + parts[2]),
                Arguments.of("서명 없는 토큰 (alg:none)", encode("{\"alg\":\"none\"}") + "." + parts[1] + "."),
                Arguments.of("Bearer 접두사 포함", "Bearer " + String.join(".", parts)),
                Arguments.of("JWT 형식 아님", "not-a-jwt"),
                Arguments.of("빈 문자열", ""),
                Arguments.of("null", null)
        );
    }

    // ===================== 헬퍼 =====================

    private static String decodePayload(String token) {
        return decode(token.split("\\.")[1]);
    }

    private static String decode(String part) {
        return new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8);
    }

    private static String encode(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
