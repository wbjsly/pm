package com.wh.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SecurityUtils 测试")
class SecurityUtilsTest {

    @BeforeEach
    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("无认证信息时返回 null")
    void noAuth_returnsNull() {
        assertNull(SecurityUtils.getCurrentUserId());
    }

    @Test
    @DisplayName("已认证时返回 principal 名称")
    void authenticated_returnsPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-42", "pw"));
        assertEquals("user-42", SecurityUtils.getCurrentUserId());
    }

    @Test
    @DisplayName("匿名认证时返回名称")
    void anonymous_returnsName() {
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.AnonymousAuthenticationToken(
                        "key", "anonymous",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertEquals("anonymous", SecurityUtils.getCurrentUserId());
    }

    @Test
    @DisplayName("principal 为 null 时返回 null")
    void nullPrincipal_returnsNull() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(null, "pw"));
        assertNull(SecurityUtils.getCurrentUserId());
    }
}
