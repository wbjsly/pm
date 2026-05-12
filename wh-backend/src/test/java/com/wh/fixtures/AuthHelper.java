package com.wh.fixtures;

import com.wh.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AuthHelper {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public String generateToken(String userId, List<String> roles) {
        return jwtTokenProvider.generateToken(userId,
                Map.of("username", "test_user", "roles", (Object) roles));
    }

    public String adminToken() {
        return generateToken("admin_001", List.of("ROLE_ADMIN"));
    }

    public String pmToken() {
        return generateToken("pm_001", List.of("ROLE_PM"));
    }

    public String userToken() {
        return generateToken("user_001", List.of("ROLE_USER"));
    }
}
