package com.wh.controller;

import com.wh.bo.system.AuthBo;
import com.wh.common.R;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthBo authBo;

    public AuthController(AuthBo authBo) {
        this.authBo = authBo;
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> loginReq) {
        return R.ok(authBo.login(loginReq.get("username"), loginReq.get("password")));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok();
    }

    @GetMapping("/info")
    public R<Map<String, Object>> getUserInfo(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return R.ok(authBo.getUserInfo(authHeader));
    }
}
