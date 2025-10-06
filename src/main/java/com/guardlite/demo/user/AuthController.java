package com.guardlite.demo.user;

import com.guardlite.demo.entities.User;
import com.guardlite.demo.exceptions.EmailAlreadyExistsException;
import com.guardlite.demo.exceptions.InvalidCredentialsException;
import com.guardlite.demo.security.jwt.JwtService;
import com.guardlite.demo.user.requests.LoginReq;
import com.guardlite.demo.user.requests.RegisterReq;
import com.guardlite.demo.user.requests.TokenRes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;

    @PostMapping("/register")
    public TokenRes register(@RequestBody @Valid RegisterReq r) {
        if (users.existsByEmail(r.email())) {
            throw new EmailAlreadyExistsException(r.email());
        }
        User u = new User();
        u.setEmail(r.email());
        u.setPasswordHash(encoder.encode(r.password()));
        if (r.role() != null) {
            u.setRole(Role.valueOf(r.role()));
        }
        users.save(u);
        return new TokenRes(jwt.generateToken(u));
    }

    @PostMapping("/login")
    public TokenRes login(@RequestBody @Valid LoginReq r) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(r.email(), r.password()));
            return new TokenRes(jwt.generateToken(r.email()));
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException();
        }
    }
}