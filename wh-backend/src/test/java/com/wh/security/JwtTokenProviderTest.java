package com.wh.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"sqlite", "dev"})
@DisplayName("JwtTokenProvider 单元测试")
class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static final String TEST_USER_ID = "user_001";
    private static final Map<String, Object> TEST_CLAIMS = Map.of(
            "username", "test_user",
            "roles", new String[]{"ROLE_USER"}
    );

    private String validToken;

    @BeforeEach
    void setUp() {
        validToken = jwtTokenProvider.generateToken(TEST_USER_ID, TEST_CLAIMS);
    }

    @Nested
    @DisplayName("generateToken 方法")
    class GenerateToken {

        @Test
        @DisplayName("生成令牌不应为 null 或空")
        void generateToken_shouldReturnNonNullString() {
            assertThat(validToken).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("生成的令牌应包含三个由点分隔的部分")
        void generateToken_shouldHaveThreeParts() {
            String[] parts = validToken.split("\\.");
            assertThat(parts).hasSize(3);
        }

        @Test
        @DisplayName("不同的 subject 生成不同令牌")
        void generateToken_withDifferentSubjects_shouldProduceDifferentTokens() {
            String token1 = jwtTokenProvider.generateToken("user_a", TEST_CLAIMS);
            String token2 = jwtTokenProvider.generateToken("user_b", TEST_CLAIMS);
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("getSubjectFromToken 方法")
    class GetSubjectFromToken {

        @Test
        @DisplayName("从令牌中提取的 subject 应与生成时一致")
        void getSubject_fromValidToken_shouldMatch() {
            String subject = jwtTokenProvider.getSubjectFromToken(validToken);
            assertThat(subject).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("从令牌中提取自定义 claims")
        void getClaims_fromValidToken_shouldContainCustomClaims() {
            Claims claims = jwtTokenProvider.getClaims(validToken);
            assertThat(claims.get("username")).isEqualTo("test_user");
        }
    }

    @Nested
    @DisplayName("validateToken 方法")
    class ValidateToken {

        @Test
        @DisplayName("有效令牌应返回 true")
        void validToken_shouldReturnTrue() {
            assertTrue(jwtTokenProvider.validateToken(validToken));
        }

        @Test
        @DisplayName("被篡改的令牌应返回 false")
        void tamperedToken_shouldReturnFalse() {
            String tampered = validToken.substring(0, validToken.length() - 5) + "XXXXX";
            assertFalse(jwtTokenProvider.validateToken(tampered));
        }

        @Test
        @DisplayName("空字符串令牌应返回 false")
        void emptyToken_shouldReturnFalse() {
            assertFalse(jwtTokenProvider.validateToken(""));
        }

        @Test
        @DisplayName("null 令牌应返回 false")
        void nullToken_shouldReturnFalse() {
            assertFalse(jwtTokenProvider.validateToken(null));
        }

        @Test
        @DisplayName("乱字符串应返回 false")
        void garbageString_shouldReturnFalse() {
            assertFalse(jwtTokenProvider.validateToken("this.is.a.garbage.token.string"));
        }
    }

    @Nested
    @DisplayName("getClaims 方法")
    class GetClaims {

        @Test
        @DisplayName("解析出的 claims 应包含标准字段")
        void getClaims_shouldContainStandardFields() {
            Claims claims = jwtTokenProvider.getClaims(validToken);

            assertThat(claims.getSubject()).isEqualTo(TEST_USER_ID);
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiration()).isNotNull();
        }

        @Test
        @DisplayName("过期时间应晚于签发时间")
        void expiration_shouldBeAfterIssuedAt() {
            Claims claims = jwtTokenProvider.getClaims(validToken);
            assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        }

        @Test
        @DisplayName("自定义 claims 应正确解析")
        void getClaims_shouldContainCustomClaims() {
            Claims claims = jwtTokenProvider.getClaims(validToken);
            assertThat(claims.get("username")).isEqualTo("test_user");
            assertThat(claims.get("roles")).isNotNull();
        }

        @Test
        @DisplayName("无效令牌解析 claims 应抛出异常")
        void invalidToken_shouldThrowException() {
            assertThrows(Exception.class, () ->
                    jwtTokenProvider.getClaims("invalid.token.here"));
        }
    }
}
