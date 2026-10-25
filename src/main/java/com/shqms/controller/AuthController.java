package com.shqms.controller;

import com.shqms.dto.request.LoginRequest;
import com.shqms.dto.request.RegisterRequest;
import com.shqms.dto.response.LoginResponse;
import com.shqms.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final SecurityContextRepository contextRepository;

    public AuthController(AuthService authService, SecurityContextRepository contextRepository) {
        this.authService = authService;
        this.contextRepository = contextRepository;
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) { return token; }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest credentials,
            HttpServletRequest request, HttpServletResponse response) {
        LoginResponse identity = authService.login(credentials);
        // Discard the anonymous/previous session, including its CSRF token, on login.
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        request.getSession(true);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
            identity.getUserId().toString(), null,
            List.of(new SimpleGrantedAuthority("ROLE_" + identity.getRole())));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
        return ResponseEntity.ok(identity);
    }
}
