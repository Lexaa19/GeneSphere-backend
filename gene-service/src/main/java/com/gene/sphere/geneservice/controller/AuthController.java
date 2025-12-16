package com.gene.sphere.geneservice.controller;

import com.gene.sphere.geneservice.model.LogInRequest;
import com.gene.sphere.geneservice.model.LogInResponse;
import com.gene.sphere.geneservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<LogInResponse> login(@RequestBody LogInRequest request) {
        System.out.println("Login attempt for user: " + request.getUsername());
        System.out.println("Password received: " + request.getPassword());
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            String token = jwtTokenProvider.generateToken(authentication);
            return ResponseEntity.ok(new LogInResponse(token, "Bearer", 3600L));
        } catch (Exception e) {
            System.out.println("Authentication failed: " + e.getMessage());
            throw e;
        }
    }

}