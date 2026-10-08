package com.example.delivery.global.security;

import com.example.delivery.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtUtil 학습용 테스트.
 *
 * <p>읽는 순서 (JwtUtil의 흐름과 같다)
 * <ol>
 *   <li>{@link Constructor}  : @Value 주입 → Base64 디코딩 → SecretKey</li>
 *   <li>{@link CreateToken}  : userId, role → header.payload.signature</li>
 *   <li>{@link ParseClaims}  : 토큰 문자열 → 서명 검증 → Claims</li>
 *   <li>{@link ParseFailure} : 잘못된 토큰이 어떤 예외로 거절되는지</li>
 * </ol>
 *
 * <p>DB, Security 없이 JwtUtil 빈 하나만 띄운다.
 * IntelliJ에서 실행하면 테스트 트리가 위 순서대로 보이고, Run 창에 중간 값이 출력된다.
 */
@SpringJUnitConfig(JwtUtil.class)
@TestPropertySource(properties = {
        "jwt.secret=" + JwtUtilTest.SECRET,
        "jwt.expiration-ms=" + JwtUtilTest.EXPIRATION_MS
})
@DisplayName("JwtUtil")
class JwtUtilTest {

    /** 원문 "delivery-test-secret-key-for-jwt-util-test!!" (44바이트)를 Base64 인코딩한 값. 테스트 전용. */
    static final String SECRET = "ZGVsaXZlcnktdGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdXRpbC10ZXN0ISE=";
    static final long EXPIRATION_MS = 3_600_000L; // 1시간

    /** 다른 서버(또는 공격자)가 가진 키라고 가정. 원문 "another-secret-key-attacker-made-it-32bytes+" */
    static final String OTHER_SECRET = "YW5vdGhlci1zZWNyZXQta2V5LWF0dGFja2VyLW1hZGUtaXQtMzJieXRlcys=";

    static final Long USER_ID = 1L;
    static final UserRole ROLE = UserRole.OWNER;

    /** 스프링이 @TestPropertySource 값을 @Value에 넣어 생성자를 호출한 결과 */
    @Autowired
    JwtUtil jwtUtil;

    // =====================================================================
    @Nested
    @DisplayName("1. 생성자: @Value로 받은 값을 SecretKey로 바꿔 필드에 저장한다")
    class Constructor {

        @Test
        @DisplayName("jwt.secret(Base64 문자열) → byte[] → HMAC-SHA256 키")
        void secretIsConvertedToKey() {
            // given: 스프링이 ${jwt.secret}에서 읽어 생성자에 넘긴 값
            String secretParam = SECRET;

            // when: 생성자 안에서 일어나는 변환을 그대로 따라간다
            byte[] keyBytes = Decoders.BASE64.decode(secretParam);     // ① Base64 → byte[]
            SecretKey expectedKey = Keys.hmacShaKeyFor(keyBytes);      // ② byte[] → SecretKey

            // then: 실제로 필드에 저장된 key와 같다
            SecretKey actualKey = privateField("key");
            assertThat(actualKey).isEqualTo(expectedKey);
            assertThat(actualKey.getAlgorithm()).isEqualTo("HmacSHA256");
            assertThat(keyBytes.length).isGreaterThanOrEqualTo(32);

            print("① 주입된 secret", secretParam);
            print("② 디코딩한 byte[]", keyBytes.length + "바이트 \"" + new String(keyBytes, StandardCharsets.UTF_8) + "\"");
            print("③ 필드 key", actualKey.getAlgorithm());
        }

        @Test
        @DisplayName("jwt.expiration-ms → long 필드")
        void expirationIsInjected() {
            long expirationMs = privateField("expirationMs");

            assertThat(expirationMs).isEqualTo(EXPIRATION_MS);
            print("필드 expirationMs", expirationMs + "ms (" + expirationMs / 60_000 + "분)");
        }

        @Test
        @DisplayName("스프링 없이 new로 호출해도 같은 키가 만들어진다 (스프링은 생성자를 대신 불러줄 뿐)")
        void newInstanceCreatesSameKey() {
            JwtUtil manual = new JwtUtil(SECRET, EXPIRATION_MS);

            SecretKey manualKey = (SecretKey) ReflectionTestUtils.getField(manual, "key");
            assertThat(manualKey).isEqualTo(privateField("key"));
        }

        @Test
        @DisplayName("[함정] ${}에서 $를 빠뜨리면 \"{jwt.secret}\" 글자 자체가 디코딩되다 실패한다")
        void missingDollarSignFailsToDecode() {
            assertThatThrownBy(() -> new JwtUtil("{jwt.secret}", EXPIRATION_MS))
                    .isInstanceOf(DecodingException.class)
                    .satisfies(e -> printError("\"{jwt.secret}\"", e));
        }

        @Test
        @DisplayName("[함정] 디코딩 결과가 32바이트 미만이면 WeakKeyException")
        void shortKeyIsRejected() {
            String shortSecret = Base64.getEncoder().encodeToString("short-key".getBytes()); // 9바이트

            assertThatThrownBy(() -> new JwtUtil(shortSecret, EXPIRATION_MS))
                    .isInstanceOf(WeakKeyException.class)
                    .satisfies(e -> printError("9바이트 키", e));
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("2. createToken(userId, role): 값을 넣고 서명해서 문자열 하나로 만든다")
    class CreateToken {

        // when: 모든 테스트가 같은 호출 결과를 본다
        final String bearerToken = jwtUtil.createToken(USER_ID, ROLE);
        final TokenParts parts = TokenParts.from(stripBearer(bearerToken));

        @Test
        @DisplayName("반환값 = \"Bearer \" + header.payload.signature")
        void returnsBearerPrefixedToken() {
            assertThat(bearerToken).startsWith("Bearer ");
            assertThat(stripBearer(bearerToken).split("\\.")).hasSize(3);

            print("반환값", bearerToken);
        }

        @Test
        @DisplayName("header ← .signWith(key): 키 길이(44바이트)를 보고 HS256이 정해진다")
        void headerHasAlgorithm() {
            assertThat(parts.headerJson()).isEqualTo("{\"alg\":\"HS256\"}");

            print("header 원문", parts.header());
            print("header 디코딩", parts.headerJson());
        }

        @Test
        @DisplayName("payload ← .subject(String.valueOf(userId)): Long 1 → 문자열 \"1\"")
        void payloadSubjectIsUserIdString() {
            assertThat(parts.payloadJson()).contains("\"sub\":\"1\"");
        }

        @Test
        @DisplayName("payload ← .claim(\"role\", role.name()): enum OWNER → 문자열 \"OWNER\"")
        void payloadRoleIsEnumName() {
            assertThat(parts.payloadJson()).contains("\"role\":\"OWNER\"");
        }

        @Test
        @DisplayName("payload ← .issuedAt(now), .expiration(now + expirationMs): 초 단위로 저장, 차이는 3600초")
        void payloadExpIsIatPlusExpiration() {
            Claims claims = parse(stripBearer(bearerToken));
            long iatSec = claims.getIssuedAt().getTime() / 1000;
            long expSec = claims.getExpiration().getTime() / 1000;

            assertThat(expSec - iatSec).isEqualTo(EXPIRATION_MS / 1000);
            assertThat(parts.payloadJson()).contains("\"iat\":" + iatSec, "\"exp\":" + expSec);
        }

        @Test
        @DisplayName("payload는 암호화가 아니라 인코딩이다: 키 없이 누구나 읽을 수 있다")
        void payloadIsReadableWithoutKey() {
            // 비밀키를 전혀 쓰지 않고 Base64URL 디코딩만 했다
            String decoded = new String(Base64.getUrlDecoder().decode(parts.payload()), StandardCharsets.UTF_8);

            assertThat(decoded).isEqualTo(parts.payloadJson());
            print("payload 원문", parts.payload());
            print("payload 디코딩", decoded);
        }

        @Test
        @DisplayName("signature ← 비밀키로 header.payload를 서명한 값")
        void signatureIsPresent() {
            assertThat(parts.signature()).isNotBlank();
            print("signature", parts.signature());
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("3. parseClaims(token): 서명을 검증하고 payload(Claims)를 꺼낸다")
    class ParseClaims {

        final String token = stripBearer(jwtUtil.createToken(USER_ID, ROLE));

        @Test
        @DisplayName("체인을 한 단계씩 끊어 보면 타입이 이렇게 바뀐다")
        void chainStepTypes() {
            SecretKey key = privateField("key");

            JwtParserBuilder builder = Jwts.parser();                 // ① 설정 시작
            builder = builder.verifyWith(key);                        // ② 검증 키 등록
            JwtParser parser = builder.build();                       // ③ 파서 완성
            Jws<Claims> jws = parser.parseSignedClaims(token);        // ④ 실제 검증 (실패하면 여기서 예외)
            Claims claims = jws.getPayload();                         // ⑤ payload만 꺼냄

            // JwtUtil.parseClaims의 결과와 같다
            assertThat(claims).isEqualTo(parse(token));

            print("④ jws.getHeader()", jws.getHeader());
            print("⑤ getPayload()", claims);
        }

        @Test
        @DisplayName("createToken에서 넣은 값을 꺼낼 때는 반대로 변환한다")
        void readsBackCreatedValues() {
            Claims claims = parse(token);

            // sub: "1" → Long.valueOf → 1L        (getUserId에서 할 일)
            String sub = claims.getSubject();
            assertThat(sub).isEqualTo("1");
            assertThat(Long.valueOf(sub)).isEqualTo(USER_ID);

            // role: "OWNER" → UserRole.valueOf → OWNER   (getRole에서 할 일)
            String role = claims.get("role", String.class);
            assertThat(role).isEqualTo("OWNER");
            assertThat(UserRole.valueOf(role)).isEqualTo(ROLE);

            print("getSubject()", sub + " → Long " + Long.valueOf(sub));
            print("get(\"role\")", role + " → UserRole." + UserRole.valueOf(role));
            print("getIssuedAt()", claims.getIssuedAt());
            print("getExpiration()", claims.getExpiration());
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("4. parseClaims 실패: 원인마다 다른 예외 (validateToken이 잡을 것들)")
    class ParseFailure {

        final String token = stripBearer(jwtUtil.createToken(USER_ID, ROLE));
        final TokenParts parts = TokenParts.from(token);

        @Test
        @DisplayName("\"Bearer \"를 떼지 않으면 → MalformedJwtException")
        void withBearerPrefix() {
            String withBearer = "Bearer " + token;

            assertThatThrownBy(() -> parse(withBearer))
                    .isInstanceOf(MalformedJwtException.class)
                    .satisfies(e -> printError("Bearer 포함", e));
        }

        @Test
        @DisplayName("payload를 고치면(sub 1 → 999) 서명이 안 맞아서 → SignatureException")
        void tamperedPayload() {
            String forgedPayload = TokenParts.encode(parts.payloadJson().replace("\"sub\":\"1\"", "\"sub\":\"999\""));
            String forged = parts.header() + "." + forgedPayload + "." + parts.signature();

            assertThatThrownBy(() -> parse(forged))
                    .isInstanceOf(SignatureException.class)
                    .satisfies(e -> printError("payload 위조", e));
        }

        @Test
        @DisplayName("다른 키로 서명한 토큰 → SignatureException")
        void signedWithOtherKey() {
            String otherToken = stripBearer(new JwtUtil(OTHER_SECRET, EXPIRATION_MS).createToken(USER_ID, ROLE));

            assertThatThrownBy(() -> parse(otherToken))
                    .isInstanceOf(SignatureException.class)
                    .satisfies(e -> printError("다른 키", e));
        }

        @Test
        @DisplayName("만료 시각이 지난 토큰 → ExpiredJwtException")
        void expiredToken() {
            // 만료 시간을 음수로 줘서 발급 즉시 만료된 토큰을 만든다
            String expired = stripBearer(new JwtUtil(SECRET, -1000L).createToken(USER_ID, ROLE));

            assertThatThrownBy(() -> parse(expired))
                    .isInstanceOf(ExpiredJwtException.class)
                    .satisfies(e -> printError("만료", e));
        }

        @Test
        @DisplayName("alg:none 무서명 토큰 → UnsupportedJwtException (parseSignedClaims라서 거부)")
        void unsignedToken() {
            String unsigned = TokenParts.encode("{\"alg\":\"none\"}") + "." + parts.payload() + ".";

            assertThatThrownBy(() -> parse(unsigned))
                    .isInstanceOf(UnsupportedJwtException.class)
                    .satisfies(e -> printError("alg:none", e));
        }
    }

    // ===================== 헬퍼 =====================

    /** 토큰을 점(.)으로 나누고 header, payload를 디코딩해 둔 것 */
    record TokenParts(String header, String payload, String signature) {

        static TokenParts from(String token) {
            String[] split = token.split("\\.");
            return new TokenParts(split[0], split[1], split[2]);
        }

        String headerJson() {
            return decode(header);
        }

        String payloadJson() {
            return decode(payload);
        }

        static String decode(String part) {
            return new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8);
        }

        static String encode(String json) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** private parseClaims를 리플렉션으로 호출한다 (학습용) */
    private Claims parse(String token) {
        return ReflectionTestUtils.invokeMethod(jwtUtil, "parseClaims", token);
    }

    @SuppressWarnings("unchecked")
    private <T> T privateField(String name) {
        return (T) ReflectionTestUtils.getField(jwtUtil, name);
    }

    private static String stripBearer(String bearerToken) {
        return bearerToken.substring("Bearer ".length());
    }

    private static void print(String label, Object value) {
        System.out.printf("✅ %-20s : %s%n", label, value);
    }

    private static void printError(String label, Throwable e) {
        System.out.printf("❌ %-20s : %s - %s%n", label, e.getClass().getSimpleName(), e.getMessage());
    }
}
