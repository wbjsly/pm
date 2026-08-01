package com.wh.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.FilterChain;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 测试")
class JwtAuthenticationFilterTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenProvider);
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWithAuth(String authHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authHeader != null) {
            request.addHeader("Authorization", authHeader);
        }
        return request;
    }

    @Test
    @DisplayName("有效 token - 设置认证信息并放行")
    void validToken_setsAuthentication() throws Exception {
        MockHttpServletRequest request = requestWithAuth("Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getSubjectFromToken("valid-token")).thenReturn("user-1");
        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.get("roles")).thenReturn(List.of("ROLE_PM", "ROLE_ADMIN"));
        when(jwtTokenProvider.getClaims("valid-token")).thenReturn(claims);

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("user-1", auth.getPrincipal());
        assertEquals(2, auth.getAuthorities().size());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("无 token - 不设置认证并放行")
    void noToken_skipsAuth() throws Exception {
        MockHttpServletRequest request = requestWithAuth(null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("无效 token - 不设置认证")
    void invalidToken_skipsAuth() throws Exception {
        MockHttpServletRequest request = requestWithAuth("Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtTokenProvider.validateToken("bad-token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("roles 非 List 时生成空权限")
    void nonListRoles_emptyAuthorities() throws Exception {
        MockHttpServletRequest request = requestWithAuth("Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getSubjectFromToken("valid-token")).thenReturn("user-1");
        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.get("roles")).thenReturn("not-a-list");
        when(jwtTokenProvider.getClaims("valid-token")).thenReturn(claims);

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(0, auth.getAuthorities().size());
    }

    @Test
    @DisplayName("非 Bearer 前缀 - 不设置认证")
    void nonBearerHeader_skipsAuth() throws Exception {
        MockHttpServletRequest request = requestWithAuth("Basic abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
